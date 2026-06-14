package org.hnau.ktiot.client.projector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.hnau.ktiot.client.model.LoginModel
import org.hnau.ktiot.client.projector.utils.Button
import org.hnau.ktiot.client.projector.utils.Localization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.fractal.SButton
import org.hnau.commons.app.projector.fractal.SPanel
import org.hnau.commons.app.projector.fractal.table.STable
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.app.projector.fractal.table.Subtable
import org.hnau.commons.app.projector.uikit.TextInput
import org.hnau.commons.app.projector.uikit.actionOrCancel
import org.hnau.commons.app.projector.uikit.onClick
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.app.projector.utils.Orientation
import org.hnau.commons.app.projector.utils.TitleOrIcon
import org.hnau.commons.app.projector.utils.horizontalDisplayPadding
import org.hnau.commons.app.projector.utils.verticalDisplayPadding
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.foldBoolean
import org.hnau.commons.kotlin.ifTrue

@Immutable
class LoginProjector(
    scope: CoroutineScope,
    private val model: LoginModel,
    private val dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    private val visibleItems: StateFlow<List<Item>> = model.useCredentials.mapState(
        scope = scope,
    ) { useCredentials ->
        buildList {
            add(Item.AddressWithPort)
            add(
                Item.ClientId(
                    isLast = !useCredentials,
                )
            )
            add(Item.AuthSwitcher)
            useCredentials.ifTrue {
                add(Item.User)
                add(Item.Password)
            }
        }
    }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalDisplayPadding()
                .verticalDisplayPadding()
                .imePadding()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimens.separation,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val useCredentials by model.useCredentials.collectAsState()
            STable(
                orientation = Orientation.Vertical,
                modifier = Modifier.requiredWidthIn(
                    max = 320.dp,
                ),
            ) {
                AddressWithPort()
                val useCredentials by model.useCredentials.collectAsState()
                ClientId(
                    isLast = !useCredentials,
                )
                AuthSwitcher()
                if (useCredentials) {
                    User(model.user)
                    Password(model.password)
                }
            }
            SButton(
                actionOrElseOrDisabled = model
                    .loginOrLogginingOrDisabled
                    .collectAsState()
                    .value
                    ?.collectAsState()
                    ?.value,
                titleOrIcon = TitleOrIcon.Title(
                    dependencies.localization.login,
                )
            )
        }
    }

    @Composable
    private fun STableScope.AddressWithPort() {

        SCell {
            SPanel {
                val focusRequester = remember { FocusRequester() }
                Input(
                    label = dependencies.localization.address,
                    input = model.address,
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier
                        .focusRequester(focusRequester),
                )
                LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
            }
        }

        SCell {
            SPanel {
                Input(
                    label = dependencies.localization.port,
                    input = model.port,
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }
    }

    @Composable
    private fun STableScope.ClientId(
        isLast: Boolean,
    ) {
        SCell {
            SPanel {
                Input(
                    label = dependencies.localization.client_id,
                    input = model.clientId,
                    keyboardType = KeyboardType.Ascii,
                    isLast = isLast,
                )
            }
        }
    }

    @Composable
    private fun STableScope.AuthSwitcher() {
        SCell {
            SPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = dependencies.localization.credentials,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = model.useCredentials.collectAsState().value,
                        onCheckedChange = { model.useCredentials.value = it },
                    )
                }
            }
        }
    }

    @Composable
    private fun STableScope.User(
        input: LoginModel.Input,
    ) {
        SCell {
            SPanel {
                Input(
                    label = dependencies.localization.user,
                    input = input,
                    keyboardType = KeyboardType.Email,
                )
            }
        }
    }

    @Composable
    private fun STableScope.Password(
        input: LoginModel.Input,
    ) {
        SCell {
            SPanel {
                Input(
                    label = dependencies.localization.password,
                    input = input,
                    keyboardType = KeyboardType.Password,
                    isLast = true,
                )
            }
        }
    }

    @Composable
    private fun Input(
        label: String,
        input: LoginModel.Input,
        keyboardType: KeyboardType,
        modifier: Modifier = Modifier,
        isLast: Boolean = false,
    ) {
        TextInput(
            label = { Text(label) },
            modifier = modifier,
            value = input.editingString,
            isError = input.correct.collectAsState().value.not(),
            keyboardActions = KeyboardActions(
                onDone = isLast.ifTrue {
                    {
                        model
                            .loginOrLogginingOrDisabled
                            .value
                            ?.value
                            ?.onClick
                            ?.invoke()
                    }
                }
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = isLast.foldBoolean(
                    ifTrue = { ImeAction.Done },
                    ifFalse = { ImeAction.Next },
                ),
                keyboardType = keyboardType,
            )
        )
    }

    @Immutable
    private sealed interface Item {

        @Immutable
        data object AddressWithPort : Item

        @Immutable
        data class ClientId(
            val isLast: Boolean,
        ) : Item

        @Immutable
        data object AuthSwitcher : Item

        @Immutable
        data object User : Item

        @Immutable
        data object Password : Item
    }
}