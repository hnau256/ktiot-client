@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package org.hnau.ktiot.client.model.property.value.editable

import arrow.core.Option
import arrow.core.some
import org.hnau.ktiot.scheme.PropertyType
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
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer

class TextViewModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    type: PropertyType.State.Text,
    val value: StateFlow<String>,
) : ViewModel {

    companion object {

        val factory: ViewModel.Factory<String, PropertyType.State.Text, Dependencies, Skeleton, TextViewModel> =
            ViewModel.Factory(::TextViewModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("text")
    /*data*/ class Skeleton : ViewModel.Skeleton

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}

class TextEditModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton,
    type: PropertyType.State.Text,
    val enabled: StateFlow<Boolean>,
) : EditModel<String> {

    companion object {

        val factory: EditModel.Factory<String, PropertyType.State.Text, Dependencies, Skeleton, TextEditModel> =
            EditModel.Factory(::TextEditModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("text")
    data class Skeleton(
        val input: MutableStateFlow<EditingString>,
    ) : EditModel.Skeleton {

        constructor(
            initial: String?,
        ) : this(
            input = initial
                .orEmpty()
                .toEditingString()
                .toMutableStateFlowAsInitial(),
        )
    }

    val input: MutableStateFlow<EditingString>
        get() = skeleton.input

    override val value: StateFlow<Option<String>> = skeleton
        .input
        .mapState(scope) { input -> input.text.some() }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler

}