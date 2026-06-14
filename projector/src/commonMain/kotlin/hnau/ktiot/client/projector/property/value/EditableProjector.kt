package org.hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import org.hnau.ktiot.client.model.property.value.EditableModel
import org.hnau.ktiot.client.model.property.value.editable.EditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberEditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberViewModel
import org.hnau.ktiot.client.model.property.value.editable.TextEditModel
import org.hnau.ktiot.client.model.property.value.editable.TextViewModel
import org.hnau.ktiot.client.model.property.value.editable.ViewModel
import org.hnau.ktiot.client.projector.property.value.editable.ContentProjector
import org.hnau.ktiot.client.projector.property.value.editable.NumberEditProjector
import org.hnau.ktiot.client.projector.property.value.editable.NumberViewProjector
import org.hnau.ktiot.client.projector.property.value.editable.TextEditProjector
import org.hnau.ktiot.client.projector.property.value.editable.TextViewProjector
import org.hnau.ktiot.client.projector.utils.Button
import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.utils.Icon
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope

@Immutable
class EditableProjector<
        T, P : PropertyType.State<T>,
        V : ViewModel, VS : ViewModel.Skeleton, VD,
        E : EditModel<T>, ES : EditModel.Skeleton, ED,
        >(
    scope: CoroutineScope,
    model: EditableModel<T, P, V, VS, VD, E, ES, ED>,
    dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        fun textView(): TextViewProjector.Dependencies

        fun textEdit(): TextEditProjector.Dependencies

        fun numberView(): NumberViewProjector.Dependencies

        fun numberEdit(): NumberEditProjector.Dependencies

    }

    sealed interface State {

        val projector: ContentProjector

        data class View(
            override val projector: ContentProjector,
            val edit: (() -> Unit)?,
        ) : State

        data class Edit(
            override val projector: ContentProjector,
            val save: StateFlow<StateFlow<(() -> Unit)?>?>,
            val cancel: () -> Unit,
        ) : State
    }

    private val state: StateFlow<State> = model
        .state
        .mapWithScope(scope) { stateScope, state ->
            when (state) {
                is EditableModel.State.View -> State.View(
                    edit = state.edit,
                    projector = when (val model = state.model) {
                        is TextViewModel -> TextViewProjector(
                            scope = stateScope,
                            dependencies = dependencies.textView(),
                            model = model
                        )

                        is NumberViewModel -> NumberViewProjector(
                            scope = stateScope,
                            dependencies = dependencies.numberView(),
                            model = model
                        )
                    }
                )

                is EditableModel.State.Edit -> State.Edit(
                    save = state.save,
                    cancel = state.cancel,
                    projector = when (val model = state.model) {
                        is TextEditModel -> TextEditProjector(
                            scope = stateScope,
                            dependencies = dependencies.textEdit(),
                            model = model
                        )

                        is NumberEditModel -> NumberEditProjector(
                            scope = stateScope,
                            dependencies = dependencies.numberEdit(),
                            model = model
                        )
                    }
                )
            }
        }

    @Composable
    override fun Top() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val state by state.collectAsState()
            when (val state = state) {
                is State.Edit -> EditTop(
                    state = state,
                )

                is State.View -> ViewTop(
                    state = state,
                )
            }
        }
    }

    @Composable
    override fun Main() {
        val state by state.collectAsState()
        with(state.projector) { Content() }
    }

    @Composable
    private fun ViewTop(
        state: State.View,
    ) {
        val editOrNull = state.edit
        editOrNull?.let { edit ->
            TextButton(
                onClick = edit,
            ) {
                Icon(Icons.Filled.Edit)
            }
        }
    }

    @Composable
    private fun EditTop(
        state: State.Edit,
    ) {
        TextButton(
            onClick = state.cancel,
        ) {
            Icon(Icons.Filled.Cancel)
        }

        val saveOrCancel by state.save.collectAsState()
        saveOrCancel.Button { leading, onClick, enabled ->
            TextButton(
                onClick = { onClick?.invoke() },
                enabled = enabled,
            ) { Icon(Icons.Filled.Done) }
        }
    }
}