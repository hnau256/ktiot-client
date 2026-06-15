package org.hnau.ktiot.client.model

import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.input.InputModel
import org.hnau.commons.app.model.input.InputSkeleton
import org.hnau.commons.app.model.input.InputType
import org.hnau.commons.app.model.input.factory.InputModelFactory
import org.hnau.commons.app.model.input.factory.createModel
import org.hnau.commons.app.model.input.factory.createSkeleton
import org.hnau.commons.app.model.input.factory.toInputModelFactory
import org.hnau.commons.app.model.input.parser.ParsingMapper
import org.hnau.commons.app.model.utils.Editable
import org.hnau.commons.app.model.utils.combineEditableWith
import org.hnau.commons.kotlin.foldNullable
import org.hnau.commons.kotlin.it
import org.hnau.ktiot.client.model.init.LoginInfo

class LoginAuthModel(
    scope: CoroutineScope,
    skeleton: Skeleton,
) {

    @Serializable
    data class Skeleton(
        val user: InputSkeleton<String, String>,
        val password: InputSkeleton<String, String>,
    ) {

        companion object {

            fun createForEdit(
                auth: LoginInfo.Auth,
            ): Skeleton = Skeleton(
                user = userInputFactory.createSkeleton(
                    value = auth.user,
                    useValueAsInitial = true,
                ),
                password = passwordInputFactory.createSkeleton(
                    value = auth.password,
                    useValueAsInitial = true,
                )
            )

            fun createForNew(): Skeleton = Skeleton(
                user = userInputFactory.createSkeleton(
                    value = "",
                    useValueAsInitial = false,
                ),
                password = passwordInputFactory.createSkeleton(
                    value = "",
                    useValueAsInitial = false,
                )
            )
        }
    }

    val user: InputModel<String, String, Unit, InputType.Edit> =
        userInputFactory.createModel(
            scope = scope,
            skeleton = skeleton.user,
        )

    val password: InputModel<String, String, Unit, InputType.Edit> =
        passwordInputFactory.createModel(
            scope = scope,
            skeleton = skeleton.password,
        )

    val auth: StateFlow<Editable<LoginInfo.Auth>> = user.editable.combineEditableWith(
        scope = scope,
        other = password.editable,
    ) { user, password ->
        LoginInfo.Auth(
            user = user,
            password = password,
        )
    }

    companion object {

        private val userInputFactory: InputModelFactory<String, String, Unit, InputType.Edit> =
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

        private val passwordInputFactory: InputModelFactory<String, String, Unit, InputType.Edit> =
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
    }


}