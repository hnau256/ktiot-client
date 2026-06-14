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
import org.hnau.ktiot.client.model.property.value.editable.TextEditModel
import org.hnau.ktiot.client.model.property.value.editable.TextViewModel
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.uikit.TextInput
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.gen.pipe.annotations.Pipe


@Immutable
class TextViewProjector(
    scope: CoroutineScope,
    private val model: TextViewModel,
    dependencies: Dependencies,
) : ContentProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun Content() {
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
class TextEditProjector(
    scope: CoroutineScope,
    private val model: TextEditModel,
    dependencies: Dependencies,
) : ContentProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun Content() {
        val focusRequester = remember { FocusRequester() }
        TextInput(
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.separation,
                    vertical = Dimens.smallSeparation,
                ),
            value = model.input,
        )
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }
}