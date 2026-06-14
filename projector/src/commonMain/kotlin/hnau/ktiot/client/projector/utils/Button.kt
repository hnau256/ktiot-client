package org.hnau.ktiot.client.projector.utils

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.app.projector.uikit.ItemsRow
import org.hnau.commons.kotlin.foldNullable
import androidx.compose.material3.Button as MaterialButton

@Composable
fun StateFlow<(() -> Unit)?>?.Button(
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable () -> Unit,
) {
    Button { leading, onClick, enabled ->
        MaterialButton(
            shape = shape,
            modifier = modifier,
            onClick = { onClick?.invoke() },
            enabled = enabled,
        ) {
            ItemsRow {
                leading?.invoke()
                content()
            }
        }
    }
}

@Composable
fun StateFlow<(() -> Unit)?>?.Button(
    button: @Composable (
        leading: (@Composable () -> Unit)?,
        onClick: (() -> Unit)?,
        enabled: Boolean,
    ) -> Unit,
) {
    foldNullable(
        ifNull = {
            key("disabled") {
                button(null, null, false)
            }
        },
        ifNotNull = { onClickOrLoading ->
            onClickOrLoading
                .collectAsState()
                .value
                .foldNullable(
                    ifNull = {
                        key("in_progress") {
                            button(progressLeading, null, true)
                        }
                    },
                    ifNotNull = { onClick ->
                        key("enabled") {
                            button(null, onClick, true)
                        }
                    }
                )
        }
    )
}

private val progressLeading: @Composable () -> Unit = {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 3.dp,
        trackColor = LocalContentColor.current,
        strokeCap = StrokeCap.Round,
    )
}