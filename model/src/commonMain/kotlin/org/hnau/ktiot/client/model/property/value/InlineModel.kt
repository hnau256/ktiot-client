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

class InlineModel<T, P: PropertyType.State<T>, I: InputType<T>, V: InlineType<T, P, I>>(
    private val scope: CoroutineScope,
    value: StateFlow<T>,
    publish: StateFlow<ActionOrElse<T, CancelOrInProgress.InProgress>>,
    type: V,
    mutable: Boolean,
) : ValueModel {

    companion object {

        fun <T, P: PropertyType.State<T>, I: InputType<T>, V: InlineType<T, P, I>> createFactory(
            type: V,
        ): ValueModel.Factory<T, Dependencies, Skeleton, InlineModel<T, P, I, V>> = ValueModel.Factory<T, Dependencies, Skeleton, InlineModel<T, P, I, V>> {
                scope: CoroutineScope,
                _: Dependencies,
                _: Skeleton,
                value: StateFlow<T>,
                publish: StateFlow<ActionOrElse<T, CancelOrInProgress.InProgress>>,
                mutable: Boolean ->
            InlineModel(
                scope = scope,
                value = value,
                publish = publish,
                type = type,
                mutable = mutable,
            )
        }
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("inline")
    /*data*/ class Skeleton : ValueModel.Skeleton

    val stateHolder: InputStateHolder<T, Nothing, I> =
        object : InputStateHolder<T, Nothing, I> {
            override val type: I
                get() = type.input

            override val stateWithErrorOrNone: StateFlow<KeyValue<T, Option<Nothing>>> =
                value.mapState(scope) { value ->
                    KeyValue(value, None)
                }

            override val updateState: StateFlow<((T) -> Unit)?> = mutable.foldBoolean(
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