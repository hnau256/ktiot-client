package org.hnau.ktiot.client.model.init

import org.hnau.ktiot.client.model.LoggedModel
import org.hnau.ktiot.client.model.LoginModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler

sealed interface InitStateModel {

    val id: Int

    val goBackHandler: GoBackHandler

    data class Login(
        val model: LoginModel,
    ) : InitStateModel {

        override val id: Int
            get() = 0

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    data class Logged(
        val model: LoggedModel,
    ) : InitStateModel {

        override val id: Int
            get() = 1

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    @Serializable
    sealed interface Skeleton {

        @Serializable
        @SerialName("login")
        data class Login(
            val skeleton: LoginModel.Skeleton,
        ) : Skeleton

        @Serializable
        @SerialName("logged")
        data class Logged(
            val skeleton: LoggedModel.Skeleton,
        ) : Skeleton
    }
}