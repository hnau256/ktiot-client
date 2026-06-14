package org.hnau.ktiot.client.model.utils

import org.hnau.ktiot.mqtt.types.topic.Topic

sealed interface ChildTopic {

    val topic: Topic.Absolute

    data class Relative(
        val parent: Topic.Absolute,
        val child: Topic.Relative,
    ): ChildTopic {

        override val topic: Topic.Absolute =
            parent + child
    }

    data class Absolute(
        override val topic: Topic.Absolute
    ): ChildTopic

    companion object
}