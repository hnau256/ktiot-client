package org.hnau.ktiot.client.projector.property.value.editable

import androidx.compose.runtime.Composable

sealed interface ContentProjector {

    @Composable
    fun Content()
}