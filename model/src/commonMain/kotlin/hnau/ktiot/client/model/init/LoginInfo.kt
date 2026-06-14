package org.hnau.ktiot.client.model.init

import kotlinx.serialization.Serializable

@Serializable
data class LoginInfo(
    val address: String,
    val clientId: String,
    val port: Int,
    val auth: Auth?,
) {

    @Serializable
    data class Auth(
        val user: String,
        val password: String,
    )
}