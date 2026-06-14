@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package org.hnau.ktiot.client.model.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer

class TemplateModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

    }

    @Serializable
    /*data*/ class Skeleton

    val goBackHandler: GoBackHandler = TODO()
}