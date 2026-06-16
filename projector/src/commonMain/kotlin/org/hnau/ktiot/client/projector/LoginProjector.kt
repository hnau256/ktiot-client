package org.hnau.ktiot.client.projector

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.fractal.SButton
import org.hnau.commons.app.projector.fractal.SContentWithActions
import org.hnau.commons.app.projector.fractal.SScreen
import org.hnau.commons.app.projector.fractal.SText
import org.hnau.commons.app.projector.fractal.input.InputProjector
import org.hnau.commons.app.projector.fractal.input.createInputProjector
import org.hnau.commons.app.projector.fractal.input.type.toInputProjectorPrototype
import org.hnau.commons.app.projector.fractal.table.STable
import org.hnau.commons.app.projector.fractal.table.Subtable
import org.hnau.commons.app.projector.fractal.table.lazy.SLazyTable
import org.hnau.commons.app.projector.fractal.table.lazy.Subtable
import org.hnau.commons.app.projector.fractal.table.lazy.cell
import org.hnau.commons.app.projector.uikit.line.weight
import org.hnau.commons.app.projector.utils.Drawable
import org.hnau.commons.app.projector.utils.Orientation
import org.hnau.commons.app.projector.utils.TitleOrIcon
import org.hnau.commons.app.projector.utils.horizontalDisplayPadding
import org.hnau.commons.app.projector.utils.verticalDisplayPadding
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.ktiot.client.model.LoginModel
import org.hnau.ktiot.client.projector.utils.Localization

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

        fun auth(): LoginAuthProjector.Dependencies
    }

    private val host: InputProjector = model
        .host
        .toInputProjectorPrototype(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Uri,
            requestFocusOnStart = true,
            showClearButton = false,
        )
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.address,
            icon = Drawable.Vector(Icons.Default.Public),
        ) { _, _ -> dependencies.localization.addressIsIncorrectError }

    private val port: InputProjector = model
        .port
        .toInputProjectorPrototype(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number,
            showClearButton = false,
        )
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.port,
            icon = null,
        ) { _, _ -> dependencies.localization.portIsIncorrectError }

    private val protocol: InputProjector = model
        .protocol
        .toInputProjectorPrototype { protocol -> SText(protocol.name) }
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.protocol,
            icon = Drawable.Vector(Icons.Default.Translate),
        )

    private val clientId: InputProjector = model
        .clientId
        .toInputProjectorPrototype(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Ascii,
        )
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.client_id,
            icon = Drawable.Vector(Icons.Default.Badge),
        ) { _, _ -> dependencies.localization.clientIdIsEmptyError }

    private val useCredentials: InputProjector = model
        .useCredentials
        .toInputProjectorPrototype()
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.credentials,
            icon = Drawable.Vector(Icons.Default.Shield),
        )

    private val auth: StateFlow<LoginAuthProjector?> = model
        .auth
        .mapWithScope(scope) { scope, authOrNull ->
            authOrNull?.let { auth ->
                LoginAuthProjector(
                    scope = scope,
                    model = auth,
                    dependencies = dependencies.auth(),
                )
            }
        }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        SScreen(
            contentPadding = contentPadding,
            modifier = Modifier.imePadding(),
            title = {},
        ) {

            val authOrNull = auth
                .collectAsState()
                .value

            SContentWithActions(
                content = {
                    SLazyTable(
                        orientation = Orientation.Vertical,
                    ) {
                        cell(
                            key = "host_with_port"
                        ) {
                            Subtable {
                                SCell(
                                    modifier = Modifier.weight(3f),
                                ) {
                                    host.Content()
                                }
                                SCell(
                                    modifier = Modifier.weight(2f),
                                ) {
                                    port.Content()
                                }
                            }
                        }
                        cell(key = "protocol") { protocol.Content() }
                        cell(key = "client_id") { clientId.Content() }
                        cell(key = "use_credentials") { useCredentials.Content() }
                        authOrNull?.let { auth ->
                            with(auth) { Content() }
                        }
                    }
                },
                actions = {
                    SButton(
                        actionOrElseOrDisabled = model.login.collectAsState().value,
                        titleOrIcon = TitleOrIcon.Both(
                            title = dependencies.localization.login,
                            icon = Drawable.Vector(Icons.Default.RocketLaunch),
                        )
                    )
                }
            )
        }
    }
}