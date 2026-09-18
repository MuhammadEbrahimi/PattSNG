package com.v2ray.ang.core

import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.Session
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A minimal SOCKS5 front end for an established JSch [Session].
 *
 * JSch only implements local (-L) and remote (-R) forwarding; it has no
 * equivalent of OpenSSH's dynamic (-D) forwarding, so the SOCKS side is
 * handled here and every accepted connection is carried over its own
 * "direct-tcpip" channel on the existing SSH session.
 *
 * Only the CONNECT command is supported. UDP ASSOCIATE and BIND are refused,
 * which matches what the SSH transport itself can carry.
 */
class SshSocksServer(
    private val session: Session,
    private val port: Int,
    private val bindAddress: String = "127.0.0.1",
    private val connectTimeoutMs: Int = 20_000,
) {
    private val running = AtomicBoolean(false)
    private val workers = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "ssh-socks-worker").apply { isDaemon = true }
    }

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var acceptThread: Thread? = null

    val isRunning: Boolean
        get() = running.get()

    /** Binds the listener synchronously so a bind failure surfaces to the caller. */
    @Throws(IOException::class)
    fun start() {
        if (running.getAndSet(true)) return
        val socket = try {
            ServerSocket(port, BACKLOG, InetAddress.getByName(bindAddress))
        } catch (e: IOException) {
            running.set(false)
            throw e
        }
        serverSocket = socket
        acceptThread = Thread({ acceptLoop(socket) }, "ssh-socks-accept").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptThread = null
        workers.shutdownNow()
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running.get() && !socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                if (running.get()) continue else break
            }
            try {
                workers.execute { serve(client) }
            } catch (e: Exception) {
                closeQuietly(client)
            }
        }
    }

    private fun serve(client: Socket) {
        var channel: ChannelDirectTCPIP? = null
        try {
            client.tcpNoDelay = true
            val input = DataInputStream(client.getInputStream())
            val output = client.getOutputStream()

            if (!negotiateMethod(input, output)) return
            val target = readRequest(input, output) ?: return

            channel = session.openChannel("direct-tcpip") as ChannelDirectTCPIP
            channel.setHost(target.host)
            channel.setPort(target.port)
            // JSch requires the streams to be taken before connect().
            val channelIn = channel.inputStream
            val channelOut = channel.outputStream
            try {
                channel.connect(connectTimeoutMs)
            } catch (e: Exception) {
                reply(output, REP_HOST_UNREACHABLE)
                return
            }

            reply(output, REP_SUCCEEDED)

            // One direction here, the other on a worker thread; both stop when
            // either side closes.
            val upstream = Thread({
                runCatching { pump(input, channelOut) }
                runCatching { channel.disconnect() }
                closeQuietly(client)
            }, "ssh-socks-up").apply { isDaemon = true }
            upstream.start()

            runCatching { pump(channelIn, output) }
            upstream.interrupt()
        } catch (e: Exception) {
            // A dropped client is routine; nothing here is recoverable.
        } finally {
            runCatching { channel?.disconnect() }
            closeQuietly(client)
        }
    }

    /** Greeting: version, method count, methods. Only "no authentication" is offered. */
    private fun negotiateMethod(input: DataInputStream, output: OutputStream): Boolean {
        val version = input.read()
        if (version != SOCKS_VERSION) return false
        val methodCount = input.read()
        if (methodCount <= 0) return false
        val methods = ByteArray(methodCount)
        input.readFully(methods)
        if (methods.none { it.toInt() and 0xff == METHOD_NO_AUTH }) {
            output.write(byteArrayOf(SOCKS_VERSION.toByte(), METHOD_NONE_ACCEPTABLE.toByte()))
            output.flush()
            return false
        }
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), METHOD_NO_AUTH.toByte()))
        output.flush()
        return true
    }

    private fun readRequest(input: DataInputStream, output: OutputStream): Target? {
        val version = input.read()
        val command = input.read()
        input.read() // reserved
        val addressType = input.read()
        if (version != SOCKS_VERSION) return null
        if (command != CMD_CONNECT) {
            reply(output, REP_COMMAND_NOT_SUPPORTED)
            return null
        }

        val host = when (addressType) {
            ATYP_IPV4 -> {
                val raw = ByteArray(4)
                input.readFully(raw)
                InetAddress.getByAddress(raw).hostAddress
            }

            ATYP_DOMAIN -> {
                val length = input.read()
                if (length <= 0) return null
                val raw = ByteArray(length)
                input.readFully(raw)
                String(raw, Charsets.US_ASCII)
            }

            ATYP_IPV6 -> {
                val raw = ByteArray(16)
                input.readFully(raw)
                InetAddress.getByAddress(raw).hostAddress
            }

            else -> {
                reply(output, REP_ADDRESS_TYPE_NOT_SUPPORTED)
                return null
            }
        } ?: return null

        val high = input.read()
        val low = input.read()
        if (high < 0 || low < 0) return null
        val targetPort = ((high and 0xff) shl 8) or (low and 0xff)
        return Target(host, targetPort)
    }

    /** The bound address is reported as 0.0.0.0:0, which every SOCKS5 client accepts. */
    private fun reply(output: OutputStream, code: Int) {
        output.write(
            byteArrayOf(
                SOCKS_VERSION.toByte(), code.toByte(), 0x00, ATYP_IPV4.toByte(),
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
            )
        )
        output.flush()
    }

    private fun pump(source: InputStream, sink: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = source.read(buffer)
            if (read < 0) break
            if (read > 0) {
                sink.write(buffer, 0, read)
                sink.flush()
            }
        }
    }

    private fun closeQuietly(socket: Socket) {
        runCatching { socket.close() }
    }

    private data class Target(val host: String, val port: Int)

    private companion object {
        const val SOCKS_VERSION = 0x05
        const val METHOD_NO_AUTH = 0x00
        const val METHOD_NONE_ACCEPTABLE = 0xFF
        const val CMD_CONNECT = 0x01
        const val ATYP_IPV4 = 0x01
        const val ATYP_DOMAIN = 0x03
        const val ATYP_IPV6 = 0x04
        const val REP_SUCCEEDED = 0x00
        const val REP_COMMAND_NOT_SUPPORTED = 0x07
        const val REP_ADDRESS_TYPE_NOT_SUPPORTED = 0x08
        const val REP_HOST_UNREACHABLE = 0x04
        const val BACKLOG = 64
        const val BUFFER_SIZE = 32 * 1024
    }
}
