package org.hnau.ktiot.client.projector

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.fractal.SScreen
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.ConnectedModel
import org.hnau.ktiot.client.projector.screen.ScreenProjector

@Immutable
class ConnectedProjector(
    scope: CoroutineScope,
    model: ConnectedModel,
    private val dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun screen(): ScreenProjector.Dependencies
    }

    private val rootScreen = ScreenProjector(
        scope = scope,
        model = model.rootScreen,
        dependencies = dependencies.screen(),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        SScreen(
            contentPadding = contentPadding,
            title = {}
        ) {
            rootScreen.Content()
        }
    }
}