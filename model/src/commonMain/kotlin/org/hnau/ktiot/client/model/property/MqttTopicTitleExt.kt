package org.hnau.ktiot.client.model.property

import org.hnau.ktiot.client.model.utils.ChildTopic
import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.mqtt.types.topic.TopicParts

fun ChildTopic.toTitle(): String = when (this) {
    is ChildTopic.Absolute -> TopicParts.Companion.Separator + topic.parts.toTitle()
    is ChildTopic.Relative -> child.parts.toTitle()
}

@Deprecated("Use ChildTopic.toTitle")
fun Topic.Relative.toTitle(): String =
    parts.toTitle()

@Deprecated("Use ChildTopic.toTitle")
fun Topic.Absolute.toTitle(): String =
    TopicParts.Companion.Separator + parts.toTitle()

@Deprecated("Use ChildTopic.toTitle")
fun Topic.toTitle(): String = when (this) {
    is Topic.Absolute -> toTitle()
    is Topic.Relative -> toTitle()
}

fun TopicParts.toTitle(): String = parts
    .map {
        it
            .fold(
                initial = "",
            ) { result, char ->
                val nextChar = when {
                    char.isWhitespace() -> ' '
                    char in toTitleDelimiters -> ' '
                    else -> char
                }
                if (result.isEmpty() && nextChar == ' ') {
                    return@fold result
                }
                val casedChar = when (result.isEmpty()) {
                    true -> nextChar.uppercaseChar()
                    false -> nextChar.lowercaseChar()
                }
                result + casedChar
            }
            .trimEnd()
    }
    .joinToString(
        separator = TopicParts.Companion.Separator.toString(),
    )

private val toTitleDelimiters: Set<Char> = setOf('_', '-')