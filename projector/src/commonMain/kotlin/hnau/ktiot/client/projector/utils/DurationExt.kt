package org.hnau.ktiot.client.projector.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.Duration

@Composable
fun Duration.format(
    localization: Localization,
): String {
    val wholeSeconds = inWholeSeconds
    val daysTitle = localization.days_short
    val hoursTitle = localization.hours_short
    val minutesTitle = localization.minutes_short
    val secondsTitle = localization.seconds_short
    return remember(
        wholeSeconds,
        daysTitle,
        hoursTitle,
        minutesTitle,
        secondsTitle,
    ) {
        var seconds = wholeSeconds
        var minutes = seconds / 60
        seconds -= minutes * 60
        var hours = minutes / 60
        minutes -= hours * 60
        val days = hours / 24
        hours -= days * 24
        listOf(
            days to daysTitle,
            hours to hoursTitle,
            minutes to minutesTitle,
            seconds to secondsTitle,
        )
            .withIndex()
            .toList()
            .let { parts ->
                parts.dropWhile { (i, countWithTitle) ->
                    i < parts.lastIndex && countWithTitle.first <= 0
                }
            }
            .map { it.value }
            .joinToString(
                separator = " ",
            ) { (count, title) ->
                "$count$title"
            }
    }
}