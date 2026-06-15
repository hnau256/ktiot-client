package org.hnau.ktiot.client.projector.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.hnau.commons.app.projector.uikit.utils.Dimens
import org.hnau.commons.app.projector.utils.Icon
import org.hnau.ktiot.client.model.utils.ChildTopic
import org.hnau.ktiot.client.projector.property.PropertyProjector

@Immutable
sealed interface ScreenItemProjector {

    @Composable
    fun Content(
        modifier: Modifier,
    )

    val key: Int

    @Immutable
    data class Property(
        private val projector: PropertyProjector,
    ) : ScreenItemProjector {

        @Composable
        override fun Content(
            modifier: Modifier,
        ) {
            projector.Content(
                modifier = modifier,
            )
        }

        override val key: Int
            get() = 0
    }

    @Immutable
    data class ChildButton(
        val topic: ChildTopic,
        val title: String,
        val onClick: () -> Unit,
    ) : ScreenItemProjector {

        @Composable
        override fun Content(
            modifier: Modifier,
        ) {
            Button(
                modifier = modifier,
                onClick = onClick,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.separation),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ChevronRight)
                }
            }
        }

        override val key: Int
            get() = 1
    }
}