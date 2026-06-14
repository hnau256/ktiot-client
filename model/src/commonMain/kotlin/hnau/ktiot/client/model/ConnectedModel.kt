@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package org.hnau.ktiot.client.model

import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.client.model.screen.ScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.getOrInit
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.commons.kotlin.toAccessor

class ConnectedModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        fun screen(): ScreenModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var rootScreen: ScreenModel.Skeleton? = null,
    )

    val rootScreen = ScreenModel(
        scope = scope,
        topic = Topic.Absolute.root,
        dependencies = dependencies.screen(
        ),
        skeleton = skeleton::rootScreen
            .toAccessor()
            .getOrInit { ScreenModel.Skeleton() },
    )

    val goBackHandler: GoBackHandler
        get() = rootScreen.goBackHandler
}