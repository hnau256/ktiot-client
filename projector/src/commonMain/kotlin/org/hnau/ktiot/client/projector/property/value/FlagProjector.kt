package org.hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.coroutines.fold
import org.hnau.commons.kotlin.foldBoolean
import org.hnau.ktiot.client.model.property.value.FlagModel
import org.hnau.ktiot.client.projector.utils.Localization

@Immutable
class FlagProjector(
    scope: CoroutineScope,
    private val model: FlagModel,
    private val dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    @Composable
    override fun Main() {
    }

    @Composable
    override fun Top() {

        val value = model
            .value
            .collectAsState()
            .value

        model
            .mutable
            .foldBoolean(
                ifFalse = {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = Dimens.separation,
                            vertical = Dimens.smallSeparation,
                        ),
                        text = value.foldBoolean(
                            ifTrue = { dependencies.localization.yes },
                            ifFalse = { dependencies.localization.no }
                        ),
                        color = value.foldBoolean(
                            ifTrue = { MaterialTheme.colorScheme.primary },
                            ifFalse = { MaterialTheme.colorScheme.error }
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                ifTrue = {
                    Switch(
                        modifier = Modifier.padding(
                            horizontal = Dimens.separation,
                            vertical = Dimens.smallSeparation,
                        ),
                        checked = value,
                        onCheckedChange = { value ->
                            model.publish.value.fold(
                                ifAction = { publish -> publish.invoke(value) },
                                ifElse = {}
                            )
                        },
                        enabled = model.publish.collectAsState().value != null,
                    )
                }
            )
    }
}