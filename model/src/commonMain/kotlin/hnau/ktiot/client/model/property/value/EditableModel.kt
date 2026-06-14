@file:UseSerializers(
    MutableStateFlowSerializer::class,
    EitherSerializer::class,
)

package org.hnau.ktiot.client.model.property.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.serialization.EitherSerializer
import org.hnau.ktiot.client.model.property.value.editable.EditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberEditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberViewModel
import org.hnau.ktiot.client.model.property.value.editable.TextEditModel
import org.hnau.ktiot.client.model.property.value.editable.TextViewModel
import org.hnau.ktiot.client.model.property.value.editable.ViewModel
import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.ifNull
import org.hnau.commons.kotlin.ifTrue
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer

class EditableModel<
        T, P : PropertyType.State<T>,
        V : ViewModel, VS : ViewModel.Skeleton, VD,
        E : EditModel<T>, ES : EditModel.Skeleton, ED,
        >(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton<VS, ES>,
    value: StateFlow<T>,
    publish: StateFlow<((T) -> Unit)?>,
    type: P,
    mutable: Boolean,
    private val createViewModelSkeleton: () -> VS,
    extractViewDependencies: Dependencies.() -> VD,
    viewFactory: ViewModel.Factory<T, P, VD, VS, V>,
    createEditModelSkeleton: (initialValue: T) -> ES,
    extractEditDependencies: Dependencies.() -> ED,
    editFactory: EditModel.Factory<T, P, ED, ES, E>,
) : ValueModel {

    class Factory<
            T, P : PropertyType.State<T>,
            V : ViewModel, VS : ViewModel.Skeleton, VD,
            E : EditModel<T>, ES : EditModel.Skeleton, ED,
            >(
        private val createViewModelSkeleton: () -> VS,
        private val extractViewDependencies: Dependencies.() -> VD,
        private val viewFactory: ViewModel.Factory<T, P, VD, VS, V>,
        private val createEditModelSkeleton: (initialValue: T) -> ES,
        private val extractEditDependencies: Dependencies.() -> ED,
        private val editFactory: EditModel.Factory<T, P, ED, ES, E>,
    ): ValueModel.Factory<
            T, P, Dependencies, Skeleton<VS, ES>,
            EditableModel<T, P, V, VS, VD, E, ES, ED>
            > {

        override fun createValueModel(
            scope: CoroutineScope,
            dependencies: Dependencies,
            skeleton: Skeleton<VS, ES>,
            value: StateFlow<T>,
            publish: StateFlow<((T) -> Unit)?>,
            type: P,
            mutable: Boolean,
        ): EditableModel<T, P, V, VS, VD, E, ES, ED> = EditableModel(
            scope = scope,
            dependencies = dependencies,
            skeleton = skeleton,
            value = value,
            publish = publish,
            type = type,
            mutable = mutable,
            createViewModelSkeleton = createViewModelSkeleton,
            extractViewDependencies = extractViewDependencies,
            viewFactory = viewFactory,
            createEditModelSkeleton = createEditModelSkeleton,
            extractEditDependencies = extractEditDependencies,
            editFactory = editFactory,
        )
    }

    @Pipe
    interface Dependencies {

        fun textView(): TextViewModel.Dependencies

        fun textEdit(): TextEditModel.Dependencies

        fun numberView(): NumberViewModel.Dependencies

        fun numberEdit(): NumberEditModel.Dependencies
    }

    @Serializable
    @SerialName("editable")
    data class Skeleton<VS : ViewModel.Skeleton, ES : EditModel.Skeleton>(
        val state: MutableStateFlow<Either<ES, VS>>,
    ) : ValueModel.Skeleton

    sealed interface State<out T, out V : ViewModel, out E : EditModel<T>> {

        data class View<out V : ViewModel>(
            val model: V,
            val edit: (() -> Unit)?,
        ) : State<Nothing, V, Nothing>

        data class Edit<out T, out E : EditModel<T>>(
            val model: E,
            val save: StateFlow<StateFlow<(() -> Unit)?>?>,
            val cancel: () -> Unit,
        ) : State<T, Nothing, E>

    }

    private fun switchToView() {
        skeleton.state.value = createViewModelSkeleton().right()
    }

    val state: StateFlow<State<T, V, E>> = skeleton
        .state
        .mapState(scope) { stateSkeletonOrNull ->
            stateSkeletonOrNull.ifNull {
                createViewModelSkeleton()
                    .right()
                    .also { viewModelInitialSkeleton ->
                        skeleton.state.value = viewModelInitialSkeleton
                    }
            }
        }
        .mapWithScope(scope) { stateScope, stateSkeleton ->
            stateSkeleton.fold(
                ifLeft = { editSkeleton ->
                    val model = editFactory.createEditModel(
                        scope = stateScope,
                        dependencies = dependencies.extractEditDependencies(),
                        skeleton = editSkeleton,
                        enabled = publish.mapState(scope) { it != null },
                        type = type,
                    )
                    State.Edit(
                        model = model,
                        cancel = ::switchToView,
                        save = model
                            .value
                            .mapWithScope(stateScope) { saveScope, valueOrNone ->
                                valueOrNone
                                    .map { value ->
                                        publish.mapState(saveScope) { publishOrNull ->
                                            publishOrNull?.let { publish ->
                                                {
                                                    publish(value)
                                                    switchToView()
                                                }
                                            }
                                        }
                                    }
                                    .getOrNull()
                            }
                    )
                },
                ifRight = { viewSkeleton ->
                    State.View(
                        model = viewFactory.createViewModel(
                            scope = stateScope,
                            dependencies = dependencies.extractViewDependencies(),
                            skeleton = viewSkeleton,
                            type = type,
                            value = value,
                        ),
                        edit = mutable.ifTrue {
                            { skeleton.state.value = createEditModelSkeleton(value.value).left() }
                        },
                    )

                }
            )
        }

    override val goBackHandler: GoBackHandler = state
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, state) ->
            when (state) {
                is State.View -> state.model.goBackHandler
                is State.Edit -> state.model.goBackHandler.mapState(stateScope) { backFromEdit ->
                    backFromEdit ?: ::switchToView
                }
            }
        }
}