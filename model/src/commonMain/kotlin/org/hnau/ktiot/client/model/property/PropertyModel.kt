package org.hnau.ktiot.client.model.property

import org.hnau.ktiot.mqtt.types.MqttSession
import org.hnau.ktiot.client.model.property.value.EditableModel
import org.hnau.ktiot.client.model.property.value.FlagModel
import org.hnau.ktiot.client.model.property.value.FractionModel
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
import org.hnau.ktiot.scheme.Element
import org.hnau.ktiot.scheme.PropertyMode
import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.fold


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

        fun flag(): FlagModel.Dependencies

        fun fraction(): FractionModel.Dependencies

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
            is PropertyType.State.Fraction -> createValueModel(
                valueModelFactory = FractionModel.factory,
                createInitialSkeleton = { FractionModel.Skeleton() },
                extractDependencies = Dependencies::fraction,
                type = type,
            )

            is PropertyType.State.Enum -> TODO()

            is PropertyType.State.Flag -> createValueModel(
                valueModelFactory = FlagModel.factory,
                createInitialSkeleton = { FlagModel.Skeleton() },
                extractDependencies = Dependencies::flag,
                type = type,
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

    private inline fun <reified T, P : PropertyType.State<T>, D, reified S : ValueModel.Skeleton, M : ValueModel> createValueModel(
        valueModelFactory: ValueModel.Factory<T, P, D, S, M>,
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