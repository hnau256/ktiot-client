package org.hnau.ktiot.client.projector.property.value.editable

import androidx.compose.runtime.Composable
import org.hnau.commons.app.projector.fractal.table.STableScope

sealed interface ContentProjector {

    @Composable
    fun STableScope.Content()
}