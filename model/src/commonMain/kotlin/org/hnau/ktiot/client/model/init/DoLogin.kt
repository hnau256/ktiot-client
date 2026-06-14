package org.hnau.ktiot.client.model.init

fun interface DoLogin {

    suspend fun doLogin(
        loginInfo: LoginInfo,
    )
}