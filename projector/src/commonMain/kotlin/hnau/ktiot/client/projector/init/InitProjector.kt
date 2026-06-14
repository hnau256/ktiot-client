package org.hnau.ktiot.client.projector.init

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import org.hnau.ktiot.client.model.init.InitModel
import org.hnau.ktiot.client.model.init.InitStateModel
import org.hnau.ktiot.client.projector.LoggedProjector
import org.hnau.ktiot.client.projector.LoginProjector
import org.hnau.ktiot.client.projector.utils.BackButtonWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.uikit.backbutton.BackButtonProjector
import org.hnau.commons.app.projector.uikit.state.LoadableContent
import org.hnau.commons.app.projector.uikit.state.StateContent
import org.hnau.commons.app.projector.uikit.state.TransitionSpec
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.map


@Immutable
class InitProjector(
    scope: CoroutineScope,
    model: InitModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        @Pipe
        interface WithInitDependencies {

            fun login(): LoginProjector.Dependencies

            fun logged(): LoggedProjector.Dependencies
        }

        fun withInitDependencies(
            backButtonWidth: BackButtonWidth,
        ): WithInitDependencies

        companion object
    }

    private val backButton = BackButtonProjector(
        scope = scope,
        goBackHandler = model.goBackHandler,
    )

    private val dependenciesWithInitDependencies: Dependencies.WithInitDependencies = dependencies.withInitDependencies(
        backButtonWidth = BackButtonWidth.create(backButton),
    )

    private val state: StateFlow<Loadable<InitStateProjector>> = model
        .state
        .mapWithScope(scope) { stateScope, stateOrLoading ->
            stateOrLoading.map { state ->
                when (state) {
                    is InitStateModel.Login -> InitStateProjector.Login(
                        projector = LoginProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependenciesWithInitDependencies.login(),
                        )
                    )

                    is InitStateModel.Logged -> InitStateProjector.Logged(
                        projector = LoggedProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependenciesWithInitDependencies.logged(),
                        )
                    )
                }
            }
        }

    @Composable
    fun Content() {
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
                        stateProjectorLocal.Content()
                    }
            }
        backButton.Content()
    }
}