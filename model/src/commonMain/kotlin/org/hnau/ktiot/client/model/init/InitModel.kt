package org.hnau.ktiot.client.model.init

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.app.model.preferences.Preferences
import org.hnau.commons.app.model.preferences.map
import org.hnau.commons.app.model.preferences.withDefault
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.LoadableStateFlow
import org.hnau.commons.kotlin.Loading
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.foldNullable
import org.hnau.commons.kotlin.getOrInit
import org.hnau.commons.kotlin.mapper.toMapper
import org.hnau.commons.kotlin.shrinkType
import org.hnau.commons.kotlin.toAccessor
import org.hnau.ktiot.client.model.LoggedModel
import org.hnau.ktiot.client.model.LoginModel


class InitModel(
    scope: CoroutineScope,
    private val dependencies: Dependencies,
    private val skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val preferencesFactory: Preferences.Factory

        fun login(
            doLogin: DoLogin,
        ): LoginModel.Dependencies

        fun logged(
            doLogout: DoLogout,
        ): LoggedModel.Dependencies

        companion object
    }

    @Serializable
    data class Skeleton(
        var state: InitStateModel.Skeleton? = null,
    )

    val state: StateFlow<Loadable<InitStateModel>> = LoadableStateFlow(
        scope = scope,
    ) {
        dependencies
            .preferencesFactory
            .createPreferences(scope)
    }
        .scopedInState(scope)
        .flatMapState(scope) { (preferencesScope, preferencesOrLoading) ->
            preferencesOrLoading.fold(
                ifLoading = { Loading.toMutableStateFlowAsInitial() },
                ifReady = { preferences ->
                    withPreferences(
                        scope = preferencesScope,
                        preferences = preferences,
                    ).mapState(preferencesScope, ::Ready)
                }
            )
        }

    private fun withPreferences(
        scope: CoroutineScope,
        preferences: Preferences,
    ): StateFlow<InitStateModel> {

        val loginStatePreference = preferences["login_state"]
            .map(
                scope = scope,
                mapper = Json.toMapper(LoginState.serializer()),
            )
            .withDefault(scope) { LoginState.Logouted() }

        return loginStatePreference
            .value
            .mapWithScope(scope) { stateScope, loginState ->
                when (loginState) {
                    is LoginState.Logouted -> InitStateModel.Login(
                        model = LoginModel(
                            scope = stateScope,
                            dependencies = dependencies.login(
                                doLogin = { loginInfo ->
                                    loginStatePreference.update(
                                        LoginState.Logged(
                                            loginInfo = loginInfo,
                                        )
                                    )
                                }
                            ),
                            skeleton = skeleton::state
                                .toAccessor()
                                .shrinkType<_, InitStateModel.Skeleton.Login>()
                                .getOrInit {
                                    InitStateModel.Skeleton.Login(
                                        loginState
                                            .cachedLoginInfo
                                            .foldNullable(
                                                ifNull = LoginModel.Skeleton::createForNew,
                                                ifNotNull = LoginModel.Skeleton::createForEdit
                                            )
                                    )
                                }
                                .skeleton
                        )
                    )

                    is LoginState.Logged -> InitStateModel.Logged(
                        model = LoggedModel(
                            scope = stateScope,
                            dependencies = dependencies.logged(
                                doLogout = {
                                    loginStatePreference.update(
                                        LoginState.Logouted(
                                            cachedLoginInfo = loginState.loginInfo,
                                        )
                                    )
                                }
                            ),
                            skeleton = skeleton::state
                                .toAccessor()
                                .shrinkType<_, InitStateModel.Skeleton.Logged>()
                                .getOrInit {
                                    InitStateModel.Skeleton.Logged(
                                        LoggedModel.Skeleton(
                                            loginInfo = loginState.loginInfo,
                                        )
                                    )
                                }
                                .skeleton
                        )
                    )
                }
            }
    }

    val goBackHandler: GoBackHandler = state
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, stateOrLoading) ->
            stateOrLoading.fold(
                ifLoading = { NeverGoBackHandler },
                ifReady = { state -> state.goBackHandler }
            )
        }
}