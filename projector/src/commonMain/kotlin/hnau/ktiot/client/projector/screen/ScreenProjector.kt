package org.hnau.ktiot.client.projector.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.fractal.padding.LocalContentPadding
import org.hnau.commons.app.projector.uikit.state.LoadableContent
import org.hnau.commons.app.projector.uikit.state.StateContent
import org.hnau.commons.app.projector.uikit.transition.TransitionSpec
import org.hnau.commons.app.projector.uikit.transition.getTransitionSpecForSlide
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.app.projector.utils.Orientation
import org.hnau.commons.app.projector.utils.plus
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.createChild
import org.hnau.commons.kotlin.coroutines.flow.state.runningFoldState
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.ifNull
import org.hnau.commons.kotlin.map
import org.hnau.commons.kotlin.valueOrElse
import org.hnau.ktiot.client.model.screen.ScreenItemModel
import org.hnau.ktiot.client.model.screen.ScreenModel
import org.hnau.ktiot.client.model.utils.ChildTopic
import org.hnau.ktiot.client.projector.property.PropertyProjector
import org.hnau.ktiot.client.projector.screen.ScreenItemProjector.ChildButton
import org.hnau.ktiot.client.projector.screen.ScreenItemProjector.Property
import kotlin.math.sign

@Immutable
class ScreenProjector(
    scope: CoroutineScope,
    private val model: ScreenModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun property(): PropertyProjector.Dependencies
    }

    @Immutable
    private sealed interface State {

        @Immutable
        data class Child(
            val scope: CoroutineScope,
            val projector: ScreenProjector,
        ) : State

        @Immutable
        data class Items(
            val items: List<Item>,
        ) : State {
            data class Item(
                val scope: CoroutineScope,
                val topic: ChildTopic,
                val projector: ScreenItemProjector,
            )
        }
    }

    private fun createState(
        scope: CoroutineScope,
        dependencies: Dependencies,
        cache: State?,
        itemsOrChildOrLoading: Loadable<Either<List<ScreenModel.Item>, ScreenModel>>,
    ): Loadable<State> = itemsOrChildOrLoading.map { itemsOrChild ->
        itemsOrChild.fold(
            ifRight = { childModel ->
                val (fromCache, itemsToCancel) = when (cache) {
                    is State.Child -> cache to null
                    is State.Items -> null to cache.items
                    null -> null to null
                }
                itemsToCancel?.forEach { it.scope.cancel() }
                fromCache.ifNull {
                    val childScope = scope.createChild()
                    State.Child(
                        scope = childScope,
                        projector = ScreenProjector(
                            scope = childScope,
                            dependencies = dependencies,
                            model = childModel,
                        )
                    )
                }
            },
            ifLeft = { itemsModels ->
                val (fromCache, childToCancel) = when (cache) {
                    is State.Child -> null to cache
                    is State.Items -> cache.items.associateBy { it.topic }.toMutableMap() to null
                    null -> null to null
                }
                childToCancel?.scope?.cancel()
                val result = State.Items(
                    items = itemsModels
                        .map { itemModel ->
                            fromCache?.remove(itemModel.topic).ifNull {
                                val itemScope = scope.createChild()
                                val topic = itemModel.topic
                                State.Items.Item(
                                    scope = itemScope,
                                    topic = topic,
                                    projector = when (val itemModel = itemModel.model) {
                                        is ScreenItemModel.Property -> Property(
                                            projector = PropertyProjector(
                                                scope = itemScope,
                                                dependencies = dependencies.property(),
                                                model = itemModel.model,
                                            )
                                        )

                                        is ScreenItemModel.ChildButton -> ChildButton(
                                            topic = topic,
                                            title = itemModel.title,
                                            onClick = { model.openChild(topic) },
                                        )
                                    }
                                )
                            }
                        },
                )
                fromCache?.forEach { (_, item) -> item.scope.cancel() }
                result
            },
        )
    }

    private val itemsOrChild: StateFlow<Loadable<State>> =
        model
            .itemsOrChild
            .runningFoldState(
                scope = scope,
                createInitial = { itemsOrChildOrLoading: Loadable<Either<List<ScreenModel.Item>, ScreenModel>> ->
                    createState(
                        scope = scope,
                        dependencies = dependencies,
                        cache = null,
                        itemsOrChildOrLoading = itemsOrChildOrLoading,
                    )
                },
                operation = { cache, itemsOrChildOrLoading ->
                    createState(
                        scope = scope,
                        dependencies = dependencies,
                        cache = cache.valueOrElse { null },
                        itemsOrChildOrLoading = itemsOrChildOrLoading,
                    )
                }
            )

    @Composable
    fun Content() {
        itemsOrChild
            .collectAsState()
            .value
            .LoadableContent(
                modifier = Modifier.fillMaxSize(),
                transitionSpec = TransitionSpec.crossfade(),
            ) { state ->
                state.StateContent(
                    modifier = Modifier.fillMaxSize(),
                    label = "ItemsOrChild",
                    contentKey = { it.zIndex },
                    transitionSpec = getTransitionSpecForSlide(
                        orientation = Orientation.Horizontal,
                    ) {
                        (targetState.zIndex - initialState.zIndex).sign * 0.5f
                    },
                ) { stateLocal ->
                    when (stateLocal) {
                        is State.Child -> stateLocal
                            .projector
                            .Content()

                        is State.Items -> Items(
                            items = stateLocal.items,
                        )
                    }
                }
            }
    }

    private val Loadable<*>.zIndex: Int
        get() = fold(
            ifLoading = { 0 },
            ifReady = { 1 }
        )

    private val State.zIndex: Int
        get() = when (this) {
            is State.Items -> 0
            is State.Child -> 1
        }

    @Composable
    private fun Items(
        items: List<State.Items.Item>,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(
                minSize = 256.dp,
            ),
            contentPadding = LocalContentPadding.current + PaddingValues(Dimens.separation),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation),
            horizontalArrangement = Arrangement.spacedBy(Dimens.separation),
        ) {
            items(
                items = items,
                key = { it.topic.topic.toString() },
            ) { item ->
                item.projector.Content(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}