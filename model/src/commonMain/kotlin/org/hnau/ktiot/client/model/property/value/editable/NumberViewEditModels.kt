@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package org.hnau.ktiot.client.model.property.value.editable

import arrow.core.Option
import arrow.core.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.app.model.EditingString
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.app.model.toEditingString
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.ifNull
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.ktiot.scheme.PropertyType

class NumberViewModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    type: PropertyType.State.Number,
    value: StateFlow<Float>,
) : ViewModel {

    companion object {

        val factory: ViewModel.Factory<Float, PropertyType.State.Number, Dependencies, Skeleton, NumberViewModel> =
            ViewModel.Factory(::NumberViewModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("number")
    /*data*/ class Skeleton : ViewModel.Skeleton

    val value: StateFlow<String> = value.mapState(
        scope = scope,
        transform = Float::toString,
    )

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}

class NumberEditModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton,
    type: PropertyType.State.Number,
    val enabled: StateFlow<Boolean>,
) : EditModel<Float> {

    companion object {

        val factory: EditModel.Factory<Float, PropertyType.State.Number, Dependencies, Skeleton, NumberEditModel> =
            EditModel.Factory(::NumberEditModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("number")
    data class Skeleton(
        val input: MutableStateFlow<EditingString>,
    ) : EditModel.Skeleton {

        constructor(
            initial: Float?,
        ) : this(
            input = initial
                .ifNull { 0f }
                .toString()
                .toEditingString()
                .toMutableStateFlowAsInitial(),
        )
    }

    val input: MutableStateFlow<EditingString>
        get() = skeleton.input

    override val value: StateFlow<Option<Float>> = skeleton
        .input
        .mapState(scope) { input -> input.text.toFloatOrNull().toOption() }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler

}
