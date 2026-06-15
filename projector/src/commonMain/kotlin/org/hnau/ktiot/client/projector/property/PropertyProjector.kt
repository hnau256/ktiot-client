package org.hnau.ktiot.client.projector.property

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.app.projector.utils.Icon
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.map
import org.hnau.ktiot.client.model.property.PropertyModel
import org.hnau.ktiot.client.model.property.value.EditableModel
import org.hnau.ktiot.client.model.property.value.FlagModel
import org.hnau.ktiot.client.model.property.value.FractionModel
import org.hnau.ktiot.client.projector.property.value.EditableProjector
import org.hnau.ktiot.client.projector.property.value.FlagProjector
import org.hnau.ktiot.client.projector.property.value.FractionProjector
import org.hnau.ktiot.client.projector.property.value.ValueProjector

@Immutable
class PropertyProjector(
    scope: CoroutineScope,
    private val model: PropertyModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun flag(): FlagProjector.Dependencies

        fun fraction(): FractionProjector.Dependencies

        fun editable(): EditableProjector.Dependencies
    }

    private val valueProjector: StateFlow<Loadable<Result<ValueProjector>>> = model
        .value
        .scopedInState(scope)
        .mapState(
            scope = scope,
        ) { (valueScope, valueOrErrorOrLoading) ->
            valueOrErrorOrLoading
                .map { valueOrError ->
                    valueOrError.map { value ->
                        when (value) {
                            is FractionModel -> FractionProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.fraction(),
                            )

                            is FlagModel -> FlagProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.flag(),
                            )

                            is EditableModel<*, *, *, *, *, *, *, *> -> EditableProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.editable(),
                            )
                        }
                    }
                }
        }


    @Composable
    private fun Content(
        modifier: Modifier,
        top: @Composable () -> Unit,
        main: @Composable () -> Unit = {},
    ) {
        Card(
            modifier = modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = Dimens.separation,
                            vertical = Dimens.smallSeparation,
                        ),
                    text = model.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                top()
            }
            main()
        }
    }

    @Composable
    fun Content(
        modifier: Modifier,
    ) {
        val valueProjectorOrLoading by valueProjector.collectAsState()
        valueProjectorOrLoading.fold(
            ifLoading = {
                Content(
                    modifier = modifier,
                    top = {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(Dimens.smallSeparation)
                                .size(32.dp)
                        )
                    }
                )
            },
            ifReady = { valueProjectorOrError ->
                valueProjectorOrError.fold(
                    onFailure = {
                        Content(
                            modifier = modifier,
                            top = {
                                Icon(
                                    modifier = Modifier
                                        .padding(Dimens.smallSeparation)
                                        .size(32.dp),
                                    icon = Icons.Filled.Error,
                                )
                            }
                        )
                    },
                    onSuccess = { valueProjector ->
                        Content(
                            modifier = modifier,
                            top = { with(valueProjector) { Top() } },
                            main = { with(valueProjector) { Main() } },
                        )
                    }
                )
            }
        )
    }
}