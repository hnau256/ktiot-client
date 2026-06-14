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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.uikit.TextInput
import org.hnau.commons.app.projector.uikit.table.Subtable
import org.hnau.commons.app.projector.uikit.table.Table
import org.hnau.commons.app.projector.uikit.table.TableOrientation
import org.hnau.commons.app.projector.uikit.table.TableScope
import org.hnau.commons.app.projector.uikit.utils.Dimens
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

    private val visibleItems: StateFlow<ImmutableList<Item>> = model.useCredentials.mapState(
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
        }.toImmutableList()
    }

    @Composable
    fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalDisplayPadding()
                .verticalDisplayPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimens.separation,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Table(
                modifier = Modifier.requiredWidthIn(
                    max = 320.dp,
                ),
                orientation = TableOrientation.Vertical,
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
            model
                .loginOrLogginingOrDisabled
                .collectAsState()
                .value
                .Button { Text(dependencies.localization.login) }
        }
    }

    @Composable
    private fun TableScope.AddressWithPort() {
        Subtable {

            Cell { modifier ->
                val focusRequester = remember { FocusRequester() }
                Input(
                    label = dependencies.localization.address,
                    input = model.address,
                    shape = shape,
                    keyboardType = KeyboardType.Uri,
                    modifier = modifier
                        .weight(3f)
                        .focusRequester(focusRequester),
                )
                LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
            }

            Cell { modifier ->
                Input(
                    label = dependencies.localization.port,
                    input = model.port,
                    shape = shape,
                    keyboardType = KeyboardType.Decimal,
                    modifier = modifier.weight(1f),
                )
            }
        }
    }

    @Composable
    private fun TableScope.ClientId(
        isLast: Boolean,
    ) {
        Cell { modifier ->
            Input(
                modifier = modifier,
                label = dependencies.localization.client_id,
                input = model.clientId,
                shape = shape,
                keyboardType = KeyboardType.Ascii,
                isLast = isLast,
            )
        }
    }

    @Composable
    private fun TableScope.AuthSwitcher() {
        Cell { modifier ->
            val shape = shape
            Row(
                modifier = modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = shape,
                    )
                    .padding(
                        horizontal = Dimens.separation,
                        vertical = Dimens.smallSeparation,
                    ),
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

    @Composable
    private fun TableScope.User(
        input: LoginModel.Input,
    ) {
        Cell { modifier ->
            Input(
                modifier = modifier,
                label = dependencies.localization.user,
                input = input,
                shape = shape,
                keyboardType = KeyboardType.Email,
            )
        }
    }

    @Composable
    private fun TableScope.Password(
        input: LoginModel.Input,
    ) {
        Cell { modifier ->
            Input(
                modifier = modifier,
                label = dependencies.localization.password,
                input = input,
                shape = shape,
                keyboardType = KeyboardType.Password,
                isLast = true,
            )
        }
    }

    @Composable
    private fun Input(
        label: String,
        input: LoginModel.Input,
        shape: Shape,
        keyboardType: KeyboardType,
        modifier: Modifier = Modifier,
        isLast: Boolean = false,
    ) {
        TextInput(
            label = { Text(label) },
            modifier = modifier,
            shape = shape,
            value = input.editingString,
            isError = input.correct.collectAsState().value.not(),
            keyboardActions = KeyboardActions(
                onDone = isLast.ifTrue {
                    {
                        model
                            .loginOrLogginingOrDisabled
                            .value
                            ?.value
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