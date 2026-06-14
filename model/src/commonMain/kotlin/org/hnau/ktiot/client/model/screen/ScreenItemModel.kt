package org.hnau.ktiot.client.model.screen

import org.hnau.ktiot.client.model.property.PropertyModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler

sealed interface ScreenItemModel {

    val key: Int

    val goBackHandler: GoBackHandler

    data class Property(
        val model: PropertyModel,
    ) : ScreenItemModel {

        override val key: Int
            get() = 0

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    data class ChildButton(
        val title: String,
    ): ScreenItemModel {

        override val key: Int
            get() = 1

        override val goBackHandler: GoBackHandler
            get() = NeverGoBackHandler

    }

    @Serializable
    sealed interface Skeleton {

        @Serializable
        @SerialName("property")
        data class Property(
            val skeleton: PropertyModel.Skeleton,
        ): Skeleton
    }
}