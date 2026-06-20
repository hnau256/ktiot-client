package org.hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable
import org.hnau.commons.app.projector.fractal.table.STableScope

interface ValueProjector {

    @Composable
    fun STableScope.Top() {}

    @Composable
    fun STableScope.Main() {}
}