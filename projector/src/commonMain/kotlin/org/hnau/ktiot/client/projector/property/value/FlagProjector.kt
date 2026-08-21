package org.hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.fractal.input.createInputProjector
import org.hnau.commons.app.projector.fractal.input.type.toInputProjectorPrototype
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.property.value.FlagModel
import org.hnau.ktiot.client.projector.utils.Localization

@Immutable
class FlagProjector(
    scope: CoroutineScope,
    private val model: FlagModel,
    private val dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    private val inputProjector = model
        .stateHolder
        .toInputProjectorPrototype()
        .createInputProjector(
            scope = scope,
            title = "Flag",
        )

    @Composable
    override fun STableScope.Top() {
        SCell { inputProjector.Content() }
    }
}