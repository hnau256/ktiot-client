package org.hnau.ktiot.client.model.property.value

import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress

sealed interface ValueModel {

    @Serializable
    sealed interface Skeleton

    val goBackHandler: GoBackHandler

    fun interface Factory<T, P: PropertyType.State<T>, D, S: Skeleton, M: ValueModel> {

        fun createValueModel(
            scope: CoroutineScope,
            dependencies: D,
            skeleton: S,
            value: StateFlow<T>,
            publish: StateFlow<ActionOrElse<T, CancelOrInProgress.InProgress>>,
            type: P,
            mutable: Boolean
        ): M
    }
}