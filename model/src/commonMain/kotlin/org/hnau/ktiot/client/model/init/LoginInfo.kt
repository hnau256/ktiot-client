package org.hnau.ktiot.client.model.init

import kotlinx.serialization.Serializable
import org.hnau.ktiot.mqtt.types.BrokerConfig
import org.hnau.ktiot.mqtt.types.ServerHost

@Serializable
data class LoginInfo(
    val host: ServerHost = ServerHost.default,
    val port: Int = BrokerConfig.Connection.defaultPort,
    val clientId: String,
    val protocol: BrokerConfig.Connection.Protocol = BrokerConfig.Connection.Protocol.default,
    val auth: Auth? = null,
) {

    @Serializable
    data class Auth(
        val user: String,
        val password: String,
    )
}