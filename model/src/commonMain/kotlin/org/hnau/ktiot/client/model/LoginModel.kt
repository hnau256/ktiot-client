@file:UseSerializers(
    MutableStateFlowSerializer::class,
    EditingString.Serializer::class,
)
@file:OptIn(ExperimentalUuidApi::class)

package org.hnau.ktiot.client.model

import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.plus
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.app.model.EditingString
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.app.model.input.InputModel
import org.hnau.commons.app.model.input.InputSkeleton
import org.hnau.commons.app.model.input.InputStateHolder
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.app.model.input.factory.InputModelFactory
import org.hnau.commons.app.model.input.factory.createModel
import org.hnau.commons.app.model.input.factory.createSkeleton
import org.hnau.commons.app.model.input.factory.toInputModelFactory
import org.hnau.commons.app.model.input.parser.ParsingMapper
import org.hnau.commons.app.model.utils.Editable
import org.hnau.commons.app.model.utils.combineEditableWith
import org.hnau.commons.app.model.utils.fold
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.KeyValue
import org.hnau.commons.kotlin.coroutines.ActionOrElse
import org.hnau.commons.kotlin.coroutines.CancelOrInProgress
import org.hnau.commons.kotlin.coroutines.actionOrCancelIfExecuting
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.foldNullable
import org.hnau.commons.kotlin.ifTrue
import org.hnau.commons.kotlin.it
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.ktiot.client.model.init.DoLogin
import org.hnau.ktiot.client.model.init.LoginInfo
import org.hnau.ktiot.mqtt.types.BrokerConfig
import org.hnau.ktiot.mqtt.types.ServerHost
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LoginModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val doLogin: DoLogin
    }

    @Serializable
    data class Skeleton(
        val host: InputSkeleton<String, ServerHost>,
        val port: InputSkeleton<String, Int>,
        val clientId: InputSkeleton<String, String>,
        val protocol: InputSkeleton<BrokerConfig.Connection.Protocol, BrokerConfig.Connection.Protocol>,
        val auth: MutableStateFlow<LoginAuthModel.Skeleton?>,
    ) {

        companion object {

            fun createForEdit(
                loginInfo: LoginInfo,
            ): Skeleton = Skeleton(
                host = hostInputFactory.createSkeleton(
                    value = loginInfo.host,
                    useValueAsInitial = true,
                ),
                port = portInputFactory.createSkeleton(
                    value = loginInfo.port,
                    useValueAsInitial = true,
                ),
                clientId = clientIdInputFactory.createSkeleton(
                    value = loginInfo.clientId,
                    useValueAsInitial = true,
                ),
                protocol = protocolInputFactory.createSkeleton(
                    value = loginInfo.protocol,
                    useValueAsInitial = true,
                ),
                auth = loginInfo
                    .auth
                    ?.let(LoginAuthModel.Skeleton::createForEdit)
                    .toMutableStateFlowAsInitial(),
            )

            fun createForNew(): Skeleton = Skeleton(
                host = hostInputFactory.createSkeleton(
                    value = ServerHost.default,
                    useValueAsInitial = false,
                ),
                port = portInputFactory.createSkeleton(
                    value = BrokerConfig.Connection.defaultPort,
                    useValueAsInitial = false,
                ),
                clientId = clientIdInputFactory.createSkeleton(
                    value = Uuid.random().toString(),
                    useValueAsInitial = false,
                ),
                protocol = protocolInputFactory.createSkeleton(
                    value = BrokerConfig.Connection.Protocol.default,
                    useValueAsInitial = false,
                ),
                auth = null.toMutableStateFlowAsInitial(),
            )
        }
    }

    val host: InputModel<String, ServerHost, Unit, InputType.Edit> = hostInputFactory.createModel(
        scope = scope,
        skeleton = skeleton.host,
    )

    val port: InputModel<String, Int, Unit, InputType.Edit> = portInputFactory.createModel(
        scope = scope,
        skeleton = skeleton.port,
    )

    val clientId: InputModel<String, String, Unit, InputType.Edit> =
        clientIdInputFactory.createModel(
            scope = scope,
            skeleton = skeleton.clientId,
        )

    val protocol: InputModel<BrokerConfig.Connection.Protocol, BrokerConfig.Connection.Protocol, Nothing, InputType.Variant<BrokerConfig.Connection.Protocol>> =
        protocolInputFactory.createModel(
            scope = scope,
            skeleton = skeleton.protocol,
        )

    val useCredentials: InputStateHolder<Boolean, Nothing, InputType.Flag> =
        object : InputStateHolder<Boolean, Nothing, InputType.Flag> {
            override val enabled: StateFlow<Boolean> =
                true.toMutableStateFlowAsInitial()
            override val type: InputType.Flag
                get() = InputType.Flag

            override val stateWithErrorOrNone: StateFlow<KeyValue<Boolean, Option<Nothing>>> =
                skeleton.auth.mapState(scope) { authOrNull ->
                    KeyValue(
                        key = authOrNull != null,
                        value = None,
                    )
                }

            override fun updateState(
                newState: Boolean,
            ) {
                skeleton.auth.update { authOrNull ->
                    val currentState = authOrNull != null
                    if (currentState == newState) {
                        return
                    }
                    newState.ifTrue { LoginAuthModel.Skeleton.createForNew() }
                }
            }

        }

    val auth: StateFlow<LoginAuthModel?> = skeleton.auth.mapWithScope(scope) { scope, authOrNull ->
        authOrNull?.let { auth ->
            LoginAuthModel(
                scope = scope,
                skeleton = auth,
            )
        }
    }

    private val loginInfo: StateFlow<Editable<LoginInfo>> = host
        .editable
        .combineEditableWith(
            scope = scope,
            other = port.editable,
            combine = ::Pair,
        )
        .combineEditableWith(
            scope = scope,
            other = clientId.editable,
            combine = Pair<ServerHost, Int>::plus,
        )
        .combineEditableWith(
            scope = scope,
            other = protocol.editable,
            combine = Triple<ServerHost, Int, String>::plus,
        )
        .combineEditableWith(
            scope = scope,
            other = auth.flatMapState(scope) { authOrNull ->
                authOrNull.foldNullable(
                    ifNull = {
                        Editable.Value(
                            value = null,
                            changed = false,
                        ).toMutableStateFlowAsInitial()
                    },
                    ifNotNull = LoginAuthModel::auth
                )
            },
        ) { (host, port, clientId, protocol), auth ->
            LoginInfo(
                host = host,
                port = port,
                clientId = clientId,
                protocol = protocol,
                auth = auth,
            )
        }

    val login: StateFlow<ActionOrElse<Unit, CancelOrInProgress.Cancel>?> = loginInfo
        .flatMapWithScope(
            scope = scope,
        ) { scope, loginInfoEditable ->
            loginInfoEditable.fold(
                ifIncorrect = { null.toMutableStateFlowAsInitial() },
                ifValue = { loginInfo, _ ->
                    actionOrCancelIfExecuting(
                        scope = scope,
                    ) {
                        dependencies
                            .doLogin
                            .doLogin(loginInfo)
                    }
                }
            )
        }

    val goBackHandler: GoBackHandler = NeverGoBackHandler

    companion object {

        private val hostInputFactory: InputModelFactory<String, ServerHost, Unit, InputType.Edit> =
            InputType.Edit.toInputModelFactory(
                parsingMapper = ParsingMapper(
                    encode = ServerHost::host,
                    parse = { input ->
                        input
                            .trim()
                            .let(ServerHost.Companion::createOrNull)
                            .foldNullable(
                                ifNull = { Unit.left() },
                                ifNotNull = ServerHost::right,
                            )
                    }
                )
            )

        private val portInputFactory: InputModelFactory<String, Int, Unit, InputType.Edit> =
            InputType.Edit.toInputModelFactory(
                parsingMapper = ParsingMapper(
                    encode = Int::toString,
                    parse = { input ->
                        input
                            .trim()
                            .toIntOrNull()
                            .foldNullable(
                                ifNull = { Unit.left() },
                                ifNotNull = Int::right,
                            )
                    }
                )
            )

        private val clientIdInputFactory: InputModelFactory<String, String, Unit, InputType.Edit> =
            InputType.Edit.toInputModelFactory(
                parsingMapper = ParsingMapper(
                    encode = ::it,
                    parse = { input ->
                        input
                            .trim()
                            .takeIf(String::isNotEmpty)
                            .foldNullable(
                                ifNull = { Unit.left() },
                                ifNotNull = String::right,
                            )
                    }
                )
            )

        private val protocolInputFactory: InputModelFactory<BrokerConfig.Connection.Protocol, BrokerConfig.Connection.Protocol, Nothing, InputType.Variant<BrokerConfig.Connection.Protocol>> =
            InputType.Variant(
                variants = BrokerConfig.Connection.Protocol.entries.toNonEmptyListOrThrow()
            ).toInputModelFactory()
    }
}