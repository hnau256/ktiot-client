package org.hnau.ktiot.client.projector.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.fractal.SIcon
import org.hnau.commons.app.projector.fractal.SPanel
import org.hnau.commons.app.projector.fractal.SText
import org.hnau.commons.app.projector.fractal.context.FContext
import org.hnau.commons.app.projector.fractal.distance.LocalDistance
import org.hnau.commons.app.projector.fractal.size.units
import org.hnau.commons.app.projector.fractal.table.STable
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.app.projector.fractal.table.Subtable
import org.hnau.commons.app.projector.fractal.utils.Mood
import org.hnau.commons.app.projector.utils.Drawable
import org.hnau.commons.app.projector.utils.Orientation
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.foldBoolean
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
        top: @Composable STableScope.() -> Unit = {},
        main: @Composable STableScope.() -> Unit = {},
        titleIsLoading: Boolean = false,
    ) {
        STable(
            orientation = Orientation.Vertical,
            modifier = modifier,
        ) {
            Subtable {
                SCell {
                    SPanel(
                        actionOrElseOrDisabled = titleIsLoading.foldBoolean(
                            ifFalse = { null },
                            ifTrue = { ActionOrElse.Else(CancelOrInProgress.InProgress) }
                        )
                    ) {
                        SText(model.title)
                    }
                }
                top(this)
            }
            SCell {
                SPanel {
                    main()
                }
            }
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
                    titleIsLoading = true,
                )
            },
            ifReady = { valueProjectorOrError ->
                valueProjectorOrError.fold(
                    onFailure = {
                        FContext(
                            update = {
                                copy(
                                    mood = Mood.Error,
                                )
                            }
                        ) {
                            Content(
                                modifier = modifier,
                                top = {
                                    SIcon(
                                        drawable = Drawable.Vector(Icons.Filled.Error),
                                    )
                                }
                            )
                        }
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