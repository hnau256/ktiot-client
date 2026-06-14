package org.hnau.ktiot.client.model.init

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LoginState {

    @Serializable
    @SerialName("logged")
    data class Logged(
        val loginInfo: LoginInfo,
    ): LoginState

    @Serializable
    @SerialName("logouted")
    data class Logouted(
        val cachedLoginInfo: LoginInfo? = null,
    ): LoginState
}