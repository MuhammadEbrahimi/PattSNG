package com.v2ray.ang.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.SshAuthMode
import com.v2ray.ang.extension.toast
import com.v2ray.ang.fmt.SshFmt
import com.v2ray.ang.ui.compose.FormDropdownField
import com.v2ray.ang.ui.compose.FormTextField
import com.v2ray.ang.ui.compose.SettingsSwitchItem

/**
 * Editor of an SSH tunnel profile. It reuses the shared server-editor scaffold and the shared form
 * fields, so the screen keeps the look, the insets, the theming and the save flow of every other
 * protocol editor; only the block between the basic fields and the dial mode is SSH-specific.
 */
class ServerSshActivity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.SSH

    @Composable
    override fun ScreenContent() {
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(initialConfig = initialConfig)
        }.apply {
            configType = serverConfigType
        }

        val authLabels = stringArrayResource(R.array.ssh_auth_mode_entries)
        val authModes = listOf(SshAuthMode.PASSWORD, SshAuthMode.PRIVATE_KEY)
        val selectedMode = SshAuthMode.fromString(uiState.sshAuthMode)

        ServerEditorScaffold(
            title = serverConfigType.toString(),
            onSaveClick = { saveServer(uiState) }
        ) {
            // Remarks, address and port; a new SSH profile opens with the port already on 22.
            CommonBasicFields(uiState)

            FormTextField(
                stringResource(R.string.server_lab_ssh_username),
                uiState.sshUsername,
                { uiState.sshUsername = it },
                maxLines = 1
            )
            FormDropdownField(
                label = stringResource(R.string.server_lab_ssh_auth_mode),
                value = authLabels.getOrElse(authModes.indexOf(selectedMode)) { authLabels.first() },
                options = authLabels.toList(),
                onValueChange = { label ->
                    val index = authLabels.indexOf(label).takeIf { it >= 0 } ?: 0
                    uiState.sshAuthMode = authModes[index].type
                }
            )

            when (selectedMode) {
                SshAuthMode.PASSWORD -> FormTextField(
                    stringResource(R.string.server_lab_ssh_password),
                    uiState.password,
                    { uiState.password = it },
                    maxLines = 1
                )

                SshAuthMode.PRIVATE_KEY -> {
                    FormTextField(
                        stringResource(R.string.server_lab_ssh_private_key),
                        uiState.sshPrivateKey,
                        { uiState.sshPrivateKey = it },
                        placeholder = stringResource(R.string.server_lab_ssh_private_key_hint)
                    )
                    FormTextField(
                        stringResource(R.string.server_lab_ssh_passphrase),
                        uiState.sshPassphrase,
                        { uiState.sshPassphrase = it },
                        maxLines = 1
                    )
                }
            }

            FormTextField(
                stringResource(R.string.server_lab_ssh_host_key),
                uiState.sshHostKey,
                { uiState.sshHostKey = it },
                maxLines = 1,
                placeholder = stringResource(R.string.server_lab_ssh_host_key_hint)
            )
            FormTextField(
                stringResource(R.string.server_lab_ssh_keep_alive),
                uiState.sshKeepAlive,
                { uiState.sshKeepAlive = it },
                keyboardType = KeyboardType.Number,
                maxLines = 1
            )
            SettingsSwitchItem(
                title = stringResource(R.string.server_lab_ssh_compression),
                summary = stringResource(R.string.server_lab_ssh_compression_summary),
                checked = uiState.sshCompression,
                onCheckedChange = { uiState.sshCompression = it }
            )

            CommonDialModeField(uiState)
        }
    }

    /**
     * The tunnel cannot open without a user and a credential, so the normalization the core relies
     * on runs at save time and names the first missing field instead of failing at connect time.
     */
    override fun validateProtocolConfig(config: ProfileItem): Boolean {
        val problem = SshFmt.normalize(config) ?: return true
        toast(
            when (problem) {
                SshFmt.Problem.MISSING_HOST -> R.string.server_lab_address
                SshFmt.Problem.INVALID_PORT -> R.string.server_lab_port
                SshFmt.Problem.MISSING_USERNAME -> R.string.server_lab_ssh_username
                SshFmt.Problem.MISSING_PASSWORD -> R.string.server_lab_ssh_password
                SshFmt.Problem.MISSING_KEY -> R.string.server_lab_ssh_private_key
            }
        )
        return false
    }
}
