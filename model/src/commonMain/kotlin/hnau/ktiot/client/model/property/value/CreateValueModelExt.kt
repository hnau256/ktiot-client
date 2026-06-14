package org.hnau.ktiot.client.model.property.value

import arrow.core.None
import arrow.core.right
import arrow.core.toOption
import co.touchlab.kermit.Logger
import org.hnau.ktiot.client.model.property.PropertyModel
import org.hnau.ktiot.client.model.property.value.editable.EditModel
import org.hnau.ktiot.client.model.property.value.editable.ViewModel
import org.hnau.ktiot.client.model.utils.ChildTopic
import org.hnau.ktiot.client.model.utils.Timestamped
import org.hnau.ktiot.scheme.PropertyMode
import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.Loading
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.Stickable
import org.hnau.commons.kotlin.coroutines.flow.state.combineState
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.coroutines.flow.state.predetermined
import org.hnau.commons.kotlin.coroutines.flow.state.stateFlow
import org.hnau.commons.kotlin.coroutines.flow.state.stick
import org.hnau.commons.kotlin.coroutines.operationOrNullIfExecuting
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.getOrInit
import org.hnau.commons.kotlin.logging.tryOrLog
import org.hnau.commons.kotlin.shrinkType
import org.hnau.commons.kotlin.toAccessor
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

val logger = Logger.withTag("CreateValueModelExt")

inline fun <
        reified T, P : PropertyType.State<T>,
        V : ViewModel, VS : ViewModel.Skeleton, VD,
        E : EditModel<T>, ES : EditModel.Skeleton, ED,
        > createEditableModel(
    scope: CoroutineScope,
    dependencies: PropertyModel.Dependencies,
    skeleton: PropertyModel.Skeleton,
    topic: ChildTopic,
    noinline createViewModelSkeleton: () -> VS,
    noinline extractViewDependencies: EditableModel.Dependencies.() -> VD,
    viewFactory: ViewModel.Factory<T, P, VD, VS, V>,
    noinline createEditModelSkeleton: (initialValue: T) -> ES,
    noinline extractEditDependencies: EditableModel.Dependencies.() -> ED,
    editFactory: EditModel.Factory<T, P, ED, ES, E>,
    type: P,
    mode: PropertyMode,
): StateFlow<Loadable<Result<EditableModel<T, P, V, VS, VD, E, ES, ED>>>> = createValueModel(
    scope = scope,
    dependencies = dependencies,
    skeleton = skeleton,
    topic = topic,
    createInitialSkeleton = {
        EditableModel.Skeleton<VS, ES>(
            state = createViewModelSkeleton().right().toMutableStateFlowAsInitial(),
        )
    },
    extractDependencies = { editable() },
    valueModelFactory = EditableModel.Factory(
        createViewModelSkeleton = createViewModelSkeleton,
        extractViewDependencies = extractViewDependencies,
        viewFactory = viewFactory,
        createEditModelSkeleton = createEditModelSkeleton,
        extractEditDependencies = extractEditDependencies,
        editFactory = editFactory,
    ),
    type = type,
    mode = mode,
)

inline fun <reified T, P : PropertyType.State<T>, D, reified S : ValueModel.Skeleton, M : ValueModel> createValueModel(
    scope: CoroutineScope,
    dependencies: PropertyModel.Dependencies,
    skeleton: PropertyModel.Skeleton,
    topic: ChildTopic,
    crossinline createInitialSkeleton: () -> S,
    crossinline extractDependencies: PropertyModel.Dependencies.() -> D,
    valueModelFactory: ValueModel.Factory<T, P, D, S, M>,
    type: P,
    mode: PropertyMode,
): StateFlow<Loadable<Result<M>>> = dependencies
    .mqttClient
    .let { client ->
        client
            .subscribe(
                topic = topic.topic,
            )
            .map { message ->
                logger.tryOrLog(
                    log = "parsing '$message' from $topic",
                    block = {
                        Json.decodeFromString(
                            deserializer = type.serializer,
                            string = message.payload.decodeToString(),
                        )
                        //TODO(error bubble) if exception
                    }
                ).let(::Ready)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = Loading,
            )
            .stick(scope) { stickableScope, valueOrErrorOrLoading ->
                valueOrErrorOrLoading.fold(
                    ifLoading = { Stickable.predetermined(Loading) },
                    ifReady = { valueOrError ->
                        valueOrError.fold(
                            onSuccess = { initialValue ->
                                Stickable.stateFlow<_, T, _>(
                                    initial = initialValue,
                                    tryUseNext = { valueOrErrorOrLoading ->
                                        valueOrErrorOrLoading.fold(
                                            ifLoading = { None },
                                            ifReady = { valueOrError ->
                                                valueOrError.getOrNull()
                                                    .toOption()
                                            }
                                        )
                                    },
                                    createResult = { values ->

                                        val overwriteValue =
                                            MutableStateFlow<Timestamped<T>?>(
                                                null
                                            )

                                        val valuesOrOverwritten = combineState(
                                            scope = scope,
                                            first = overwriteValue.mapState(
                                                scope = stickableScope
                                            ) { overwrittenOrNull ->
                                                overwrittenOrNull?.takeIf { overwritten ->
                                                    Clock.System.now() - overwritten.timestamp < 3.seconds
                                                }
                                            },
                                            second = values.mapState(
                                                scope = stickableScope,
                                                transform = Timestamped.Companion::now,
                                            ),
                                        ) { overwritten, received ->
                                            when {
                                                overwritten == null -> received.value to false
                                                overwritten.timestamp > received.timestamp -> overwritten.value to true
                                                else -> received.value to false
                                            }
                                        }

                                        valueModelFactory.createValueModel(
                                            scope = stickableScope,
                                            dependencies = dependencies.extractDependencies(),
                                            skeleton = skeleton::value
                                                .toAccessor()
                                                .shrinkType<_, S>()
                                                .getOrInit { createInitialSkeleton() },
                                            value = valuesOrOverwritten.mapState(
                                                stickableScope
                                            ) { it.first },
                                            type = type,
                                            mutable = when (mode) {
                                                PropertyMode.Manual -> true
                                                PropertyMode.Hardware, PropertyMode.Calculated -> false
                                            },
                                            publish = operationOrNullIfExecuting(
                                                stickableScope
                                            ) { valueToSend ->
                                                val payload = logger
                                                    .tryOrLog(
                                                        log = "encoding '$valueToSend' for $topic"
                                                    ) {
                                                        Json.Default
                                                            .encodeToString(
                                                                serializer = type.serializer,
                                                                value = valueToSend
                                                            )
                                                            .encodeToByteArray()
                                                    }
                                                    .getOrNull()
                                                    ?: return@operationOrNullIfExecuting //TODO(error bubble)

                                                overwriteValue.value =
                                                    Timestamped.now(valueToSend)

                                                client.publish(
                                                    topic = topic.topic,
                                                    payload = payload,
                                                    retained = true,
                                                ) //TODO(error bubble) if false

                                                valuesOrOverwritten.first {
                                                    val overwritten = it.second
                                                    !overwritten
                                                }
                                            }

                                        )
                                            .let(Result.Companion::success)
                                            .let(::Ready)
                                    }
                                )
                            },
                            onFailure = { error ->
                                Stickable.predetermined(
                                    Ready(Result.failure(error))
                                )
                            }
                        )
                    }
                )
            }

    }