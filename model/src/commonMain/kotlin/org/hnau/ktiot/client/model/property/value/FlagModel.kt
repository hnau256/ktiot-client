package org.hnau.ktiot.client.model.property.value

import arrow.core.None
import arrow.core.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.app.model.input.InputStateHolder
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.KeyValue
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.coroutines.fold
import org.hnau.commons.kotlin.foldBoolean
import org.hnau.ktiot.scheme.PropertyType

class FlagModel(
    private val scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    value: StateFlow<Boolean>,
    publish: StateFlow<ActionOrElse<Boolean, CancelOrInProgress.InProgress>>,
    type: PropertyType.State.Flag,
    mutable: Boolean,
) : ValueModel {

    companion object {

        val factory: ValueModel.Factory<Boolean, PropertyType.State.Flag, Dependencies, Skeleton, FlagModel> =
            ValueModel.Factory(::FlagModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("flag")
    /*data*/ class Skeleton : ValueModel.Skeleton

    val stateHolder: InputStateHolder<Boolean, Nothing, InputType.Flag> =
        object : InputStateHolder<Boolean, Nothing, InputType.Flag> {
            override val type: InputType.Flag
                get() = InputType.Flag

            override val stateWithErrorOrNone: StateFlow<KeyValue<Boolean, Option<Nothing>>> =
                value.mapState(scope) { value ->
                    KeyValue(value, None)
                }

            override val updateState: StateFlow<((Boolean) -> Unit)?> = mutable.foldBoolean(
                ifFalse = { null.toMutableStateFlowAsInitial() },
                ifTrue = {
                    publish.mapState(scope) { publishOrElse ->
                        publishOrElse.fold(
                            ifAction = { publish ->
                                { value ->
                                    publish(value)
                                }
                            },
                            ifElse = { null }
                        )
                    }
                }
            )

            override val decoration: StateFlow<InputStateHolder.Decoration?> =
                publish.mapState(scope) { publishOrElse ->
                    publishOrElse.fold(
                        ifAction = { null },
                        ifElse = { InputStateHolder.Decoration.InProgress },
                    )
                }
        }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}