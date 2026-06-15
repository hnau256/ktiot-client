package org.hnau.ktiot.client.projector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastJoinToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.hnau.commons.app.projector.fractal.SButton
import org.hnau.commons.app.projector.uikit.ErrorPanel
import org.hnau.commons.app.projector.uikit.state.StateContent
import org.hnau.commons.app.projector.uikit.transition.TransitionSpec
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.app.projector.utils.TitleOrIcon
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.ktiot.client.model.LoggedModel
import org.hnau.ktiot.client.projector.utils.Localization
import org.hnau.ktiot.client.projector.utils.format
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import androidx.compose.material3.Button as MaterialButton

@Immutable
class LoggedProjector(
    scope: CoroutineScope,
    model: LoggedModel,
    private val dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization

        fun connected(): ConnectedProjector.Dependencies
    }

    @Immutable
    sealed interface State {

        @Immutable
        data class Connecting(
            val logout: StateFlow<ActionOrElse<Unit, CancelOrInProgress.InProgress>>,
        ) : State

        @Immutable
        data class WaitingForReconnection(
            val errorMessage: String?,
            val beforeReconnection: StateFlow<Duration>,
            val logout: StateFlow<ActionOrElse<Unit, CancelOrInProgress.InProgress>>,
            val reconnectNow: () -> Unit,
        ) : State

        @Immutable
        data class Connected(
            val projector: ConnectedProjector,
        ) : State
    }

    private val state: StateFlow<State> = model
        .state
        .mapWithScope(scope) { sateScope, state ->
            when (state) {
                is LoggedModel.State.Connected -> State.Connected(
                    projector = ConnectedProjector(
                        scope = sateScope,
                        model = state.model,
                        dependencies = dependencies.connected(),
                    )
                )

                is LoggedModel.State.Connecting -> State.Connecting(
                    logout = state.logout,
                )

                is LoggedModel.State.WaitingForReconnection -> State.WaitingForReconnection(
                    errorMessage = state.cause.message,
                    logout = state.logout,
                    reconnectNow = state.reconnectNow,
                    beforeReconnection = run {
                        val calc = { state.reconnectionAt - Clock.System.now() }
                        ticker(
                            delayMillis = 1.seconds.inWholeMilliseconds,
                        )
                            .consumeAsFlow()
                            .map { calc() }
                            .stateIn(
                                scope = scope,
                                started = SharingStarted.Eagerly,
                                initialValue = calc()
                            )
                    }
                )
            }
        }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        state
            .collectAsState()
            .value
            .StateContent(
                modifier = Modifier.fillMaxSize(),
                label = "MqttState",
                transitionSpec = TransitionSpec.crossfade(),
                contentKey = { state ->
                    when (state) {
                        is State.Connecting -> 0
                        is State.WaitingForReconnection -> 1
                        is State.Connected -> 2
                    }
                },
            ) { state ->
                when (state) {
                    is State.Connected -> state.projector.Content(
                        contentPadding = contentPadding,
                    )

                    is State.Connecting -> Connecting(
                        state = state,
                        contentPadding = contentPadding,
                    )

                    is State.WaitingForReconnection -> WaitingForReconnection(
                        state = state,
                        contentPadding = contentPadding,
                    )
                }
            }
    }

    @Composable
    private fun LogoutButton(
        logout: StateFlow<ActionOrElse<Unit, CancelOrInProgress.InProgress>>,
    ) {
        SButton(
            actionOrElseOrDisabled = logout.collectAsState().value,
            titleOrIcon = TitleOrIcon.Title(dependencies.localization.logout)
        )
    }

    @Composable
    private fun Connecting(
        state: State.Connecting,
        contentPadding: PaddingValues,
    ) {
        //TODO handle contentPadding
        ErrorPanel(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    CircularProgressIndicator()
                    Text(dependencies.localization.connecting)
                }
            },
            button = {
                LogoutButton(
                    logout = state.logout,
                )
            }
        )
    }

    @Composable
    private fun WaitingForReconnection(
        state: State.WaitingForReconnection,
        contentPadding: PaddingValues,
    ) {
        //TODO handle contentPadding
        ErrorPanel(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    Text(
                        listOfNotNull(
                            dependencies.localization.connection_error,
                            state.errorMessage
                        ).fastJoinToString(
                            separator = ": ",
                        )
                    )
                    Text(
                        dependencies.localization.before_reconnection +
                                ": " +
                                state.beforeReconnection.collectAsState().value.format(
                                    localization = dependencies.localization,
                                )
                    )
                }
            },
            button = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    LogoutButton(
                        logout = state.logout,
                    )
                    MaterialButton(
                        onClick = state.reconnectNow,
                    ) {
                        Text(dependencies.localization.reconnect_now)
                    }
                }
            }
        )
    }
}