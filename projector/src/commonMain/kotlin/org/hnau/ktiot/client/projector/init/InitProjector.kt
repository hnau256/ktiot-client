package org.hnau.ktiot.client.projector.init

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.uikit.backbutton.BackButtonHost
import org.hnau.commons.app.projector.uikit.state.LoadableContent
import org.hnau.commons.app.projector.uikit.state.StateContent
import org.hnau.commons.app.projector.uikit.transition.TransitionSpec
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.map
import org.hnau.ktiot.client.model.init.InitModel
import org.hnau.ktiot.client.model.init.InitStateModel
import org.hnau.ktiot.client.projector.LoggedProjector
import org.hnau.ktiot.client.projector.LoginProjector


@Immutable
class InitProjector(
    scope: CoroutineScope,
    private val model: InitModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun login(): LoginProjector.Dependencies

        fun logged(): LoggedProjector.Dependencies

        companion object
    }

    private val state: StateFlow<Loadable<InitStateProjector>> = model
        .state
        .mapWithScope(scope) { stateScope, stateOrLoading ->
            stateOrLoading.map { state ->
                when (state) {
                    is InitStateModel.Login -> InitStateProjector.Login(
                        projector = LoginProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependencies.login(),
                        )
                    )

                    is InitStateModel.Logged -> InitStateProjector.Logged(
                        projector = LoggedProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependencies.logged(),
                        )
                    )
                }
            }
        }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        BackButtonHost(
            contentPadding = contentPadding,
            goBackHandler = model.goBackHandler,
        ) {contentPadding ->
            state
                .collectAsState()
                .value
                .LoadableContent(
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = TransitionSpec.crossfade(),
                ) { stateProjector ->
                    stateProjector
                        .StateContent(
                            modifier = Modifier.fillMaxSize(),
                            label = "LoginOrLogged",
                            contentKey = InitStateProjector::key,
                            transitionSpec = TransitionSpec.crossfade(),
                        ) { stateProjectorLocal ->
                            stateProjectorLocal.Content(
                                contentPadding = contentPadding,
                            )
                        }
                }
        }
    }
}