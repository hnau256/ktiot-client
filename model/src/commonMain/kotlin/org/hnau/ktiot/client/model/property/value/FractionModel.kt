package org.hnau.ktiot.client.model.property.value

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.coroutines.fold
import org.hnau.commons.kotlin.foldNullable
import org.hnau.ktiot.scheme.PropertyType
import kotlin.time.Duration.Companion.milliseconds

class FractionModel(
    private val scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    value: StateFlow<Float>,
    private val publish: StateFlow<ActionOrElse<Float, CancelOrInProgress.InProgress>>,
    val type: PropertyType.State.Fraction,
    val mutable: Boolean,
) : ValueModel {

    companion object {

        val factory: ValueModel.Factory<Float, PropertyType.State.Fraction, Dependencies, Skeleton, FractionModel> =
            ValueModel.Factory(::FractionModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("fraction")
    /*data*/ class Skeleton : ValueModel.Skeleton

    private val overwriteValue: MutableStateFlow<Float?> = MutableStateFlow(null)

    val value: StateFlow<Float> = overwriteValue.flatMapState(scope) { overwriteOrNull ->
        overwriteOrNull.foldNullable(
            ifNull = { value },
            ifNotNull = { it.toMutableStateFlowAsInitial() },
        )
    }

    fun update(
        newValue: Float,
    ) {
        overwriteValue.value = newValue
    }

    val isPublishing: StateFlow<Boolean> = publish.mapState(scope) { publishOrInProgress ->
        publishOrInProgress.fold(
            ifAction = { false },
            ifElse = { true },
        )
    }

    fun publish() {
        val valueToPublish = overwriteValue.value ?: return
        publish.value.fold(
            ifElse = {},
            ifAction = { publish ->
                publish(valueToPublish)
                scope.launch {//TODO
                    delay(10.milliseconds)
                    overwriteValue.value = null
                }
            }
        )
    }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}