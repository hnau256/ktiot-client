package org.hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.app.projector.fractal.input.createInputProjector
import org.hnau.commons.app.projector.fractal.input.type.toInputProjectorPrototype
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.property.value.FlagModel
import org.hnau.ktiot.client.model.property.value.InlineModel
import org.hnau.ktiot.client.model.property.value.InlineType
import org.hnau.ktiot.client.projector.utils.Localization
import org.hnau.ktiot.scheme.PropertyType

@Immutable
class InlineProjector<T, P: PropertyType.State<T>, I: InputType<T>, V: InlineType<T, P, I>>(
    scope: CoroutineScope,
    private val model: InlineModel<T, P, I, V>,
    private val dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    private val inputProjector = model.stateHolder.type
        model
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