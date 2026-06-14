package org.hnau.ktiot.client.projector

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import org.hnau.ktiot.client.model.ConnectedModel
import org.hnau.ktiot.client.projector.screen.ScreenProjector
import org.hnau.ktiot.client.projector.utils.BackButtonWidth
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.uikit.FullScreen
import org.hnau.commons.app.projector.uikit.TopBar
import org.hnau.commons.gen.pipe.annotations.Pipe

@Immutable
class ConnectedProjector(
    scope: CoroutineScope,
    model: ConnectedModel,
    private val dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        val backButtonWidth: BackButtonWidth

        fun screen(): ScreenProjector.Dependencies
    }

    private val rootScreen = ScreenProjector(
        scope = scope,
        model = model.rootScreen,
        dependencies = dependencies.screen(),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        FullScreen(
            backButtonWidth = dependencies.backButtonWidth.width,
            top = { contentPadding ->
                TopBar(
                    modifier = Modifier.padding(contentPadding),
                ) {}
            },
        ) { contentPadding ->
            rootScreen.Content(
                contentPadding = contentPadding,
            )
        }
    }
}