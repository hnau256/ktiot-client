package org.hnau.ktiot.client.model.utils

import kotlin.time.Clock
import kotlin.time.Instant

data class Timestamped<out T>(
    val timestamp: Instant,
    val value: T,
) {

    companion object {

        fun <T> now(
            value: T,
        ): Timestamped<T> = Timestamped(
            value = value,
            timestamp = Clock.System.now(),
        )
    }
}