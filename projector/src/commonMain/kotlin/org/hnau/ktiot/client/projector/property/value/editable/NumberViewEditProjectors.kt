package org.hnau.ktiot.client.projector.property.value.editable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.app.projector.uikit.TextInput
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.property.value.editable.NumberEditModel
import org.hnau.ktiot.client.model.property.value.editable.NumberViewModel

@Immutable
class NumberViewProjector(
    scope: CoroutineScope,
    private val model: NumberViewModel,
    dependencies: Dependencies,
) : ContentProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun STableScope.Content() {
        Text(
            modifier = Modifier
                .padding(Dimens.separation),
            text = model
                .value
                .collectAsState()
                .value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Immutable
class NumberEditProjector(
    scope: CoroutineScope,
    private val model: NumberEditModel,
    dependencies: Dependencies,
) : ContentProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun STableScope.Content() {
        val focusRequester = remember { FocusRequester() }
        TextInput(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(
                    horizontal = Dimens.separation,
                    vertical = Dimens.smallSeparation,
                ),
            value = model.input,
        )
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }
}