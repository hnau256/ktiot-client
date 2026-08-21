package org.hnau.ktiot.client.model.property

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.fold
import org.hnau.ktiot.client.model.property.value.EditableModel
import org.hnau.ktiot.client.model.property.value.InlineModel
import org.hnau.ktiot.client.model.property.value.InlineModel.Flag
import org.hnau.ktiot.client.model.property.value.InlineModel.Fraction
import org.hnau.ktiot.client.model.property.value.ValueModel
import org.hnau.ktiot.client.model.property.value.createEditableModel
import org.hnau.ktiot.client.model.property.value.createValueModel
import org.hnau.ktiot.client.model.property.value.editable.EditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberEditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberViewModel
import org.hnau.ktiot.client.model.property.value.editable.TextEditModel
import org.hnau.ktiot.client.model.property.value.editable.TextViewModel
import org.hnau.ktiot.client.model.property.value.editable.ViewModel
import org.hnau.ktiot.client.model.utils.ChildTopic
import org.hnau.ktiot.mqtt.types.MqttSession
import org.hnau.ktiot.scheme.Element
import org.hnau.ktiot.scheme.PropertyMode
import org.hnau.ktiot.scheme.PropertyType


class PropertyModel(
    private val scope: CoroutineScope,
    private val dependencies: Dependencies,
    private val skeleton: Skeleton,
    val title: String,
    val topic: ChildTopic,
    private val property: Element.Type.Property<*>,
) {

    @Pipe
    interface Dependencies {

        val mqttClient: MqttSession

        fun inline(): InlineModel.Dependencies

        fun editable(): EditableModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var value: ValueModel.Skeleton? = null,
    )

    val mode: PropertyMode
        get() = property.mode

    val value: StateFlow<Loadable<Result<ValueModel>>> = when (val type = property.type) {
        is PropertyType.Events -> TODO()
        is PropertyType.State -> when (type) {
            is PropertyType.State.Fraction -> createInlineModel<Float, PropertyType.State.Fraction, InputType.Fraction<Float>, Fraction> (
                propertyType = type,
                inlineType = InputType.Fraction(type.range),
                createInlineModel = ::Fraction,
            )

            is PropertyType.State.Enum -> TODO()

            is PropertyType.State.Flag -> createInlineModel<Boolean, PropertyType.State.Flag, InputType.Flag, Flag> (
                propertyType = type,
                inlineType = InputType.Flag,
                createInlineModel = ::Flag,
            )

            is PropertyType.State.Number -> createEditableModel(
                createViewModelSkeleton = { NumberViewModel.Skeleton() },
                extractViewDependencies = { numberView() },
                viewFactory = NumberViewModel.factory,
                createEditModelSkeleton = { initial -> NumberEditModel.Skeleton(initial) },
                extractEditDependencies = { numberEdit() },
                editFactory = NumberEditModel.factory,
                type = type,
            )

            is PropertyType.State.Text -> createEditableModel(
                createViewModelSkeleton = { TextViewModel.Skeleton() },
                extractViewDependencies = { textView() },
                viewFactory = TextViewModel.factory,
                createEditModelSkeleton = { initial -> TextEditModel.Skeleton(initial) },
                extractEditDependencies = { textEdit() },
                editFactory = TextEditModel.factory,
                type = type,
            )
        }
    }

    private inline fun <reified T, P : PropertyType.State<T>, reified I : InputType<T>, reified M: InlineModel<T, I>> createInlineModel(
        propertyType: P,
        inlineType: I,
        crossinline createInlineModel: (
            scope: CoroutineScope,
            value: StateFlow<T>,
            publish: StateFlow<ActionOrElse<T, CancelOrInProgress.InProgress>>,
            type: I,
            mutable: Boolean,
        ) -> M,
    ): StateFlow<Loadable<Result<M>>> = createValueModel(
        createInitialSkeleton = { InlineModel.Skeleton() },
        extractDependencies = { inline() },
        valueModelFactory = { scope, _, _, flow, publish, bool ->
            createInlineModel(
                scope,
                flow,
                publish,
                inlineType,
                bool,
            )
        },
        type = propertyType,
    )

    private inline fun <reified T, P : PropertyType.State<T>, D, reified S : ValueModel.Skeleton, M : ValueModel> createValueModel(
        crossinline valueModelFactory: (
            scope: CoroutineScope,
            dependencies: D,
            skeleton: S,
            value: StateFlow<T>,
            publish: StateFlow<ActionOrElse<T, CancelOrInProgress.InProgress>>,
            mutable: Boolean
        ) -> M,
        crossinline createInitialSkeleton: () -> S,
        crossinline extractDependencies: Dependencies.() -> D,
        type: P,
    ): StateFlow<Loadable<Result<M>>> = createValueModel(
        scope = scope,
        dependencies = dependencies,
        skeleton = skeleton,
        topic = topic,
        createInitialSkeleton = createInitialSkeleton,
        extractDependencies = extractDependencies,
        valueModelFactory = valueModelFactory,
        type = type,
        mode = property.mode,
    )

    private inline fun <
            reified T, P : PropertyType.State<T>,
            V : ViewModel, VS : ViewModel.Skeleton, VD,
            E : EditModel<T>, ES : EditModel.Skeleton, ED,
            > createEditableModel(
        noinline createViewModelSkeleton: () -> VS,
        noinline extractViewDependencies: EditableModel.Dependencies.() -> VD,
        viewFactory: ViewModel.Factory<T, P, VD, VS, V>,
        noinline createEditModelSkeleton: (initialValue: T) -> ES,
        noinline extractEditDependencies: EditableModel.Dependencies.() -> ED,
        editFactory: EditModel.Factory<T, P, ED, ES, E>,
        type: P,
    ): StateFlow<Loadable<Result<EditableModel<T, P, V, VS, VD, E, ES, ED>>>> = createEditableModel(
        scope = scope,
        dependencies = dependencies,
        skeleton = skeleton,
        topic = topic,
        type = type,
        mode = property.mode,
        createViewModelSkeleton = createViewModelSkeleton,
        extractViewDependencies = extractViewDependencies,
        viewFactory = viewFactory,
        createEditModelSkeleton = createEditModelSkeleton,
        extractEditDependencies = extractEditDependencies,
        editFactory = editFactory,
    )

    val goBackHandler: GoBackHandler = value.flatMapState(scope) { valueOrErrorLoading ->
        valueOrErrorLoading.fold(
            ifLoading = { NeverGoBackHandler },
            ifReady = { valueOrError ->
                valueOrError.fold(
                    onFailure = { NeverGoBackHandler },
                    onSuccess = ValueModel::goBackHandler,
                )
            }
        )
    }
}