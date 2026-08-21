package org.hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import arrow.optics.Iso
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.app.projector.fractal.input.createInputProjector
import org.hnau.commons.app.projector.fractal.input.type.toInputProjectorPrototype
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.property.value.InlineModel
import org.hnau.ktiot.client.projector.utils.Localization

@Immutable
class InlineProjector<T, I : InputType<T>>(
    scope: CoroutineScope,
    private val model: InlineModel<T, I>,
    private val dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    private val inputProjector = when (model) {
        is InlineModel.Flag -> model.stateHolder.toInputProjectorPrototype()
        is InlineModel.Fraction -> model.stateHolder.toInputProjectorPrototype(
            floatIso = Iso.id(),
        )
    }.createInputProjector(
        scope = scope,
        title = "Flag",
    )

    @Composable
    override fun STableScope.Top() {
        SCell { inputProjector.Content() }
    }
}