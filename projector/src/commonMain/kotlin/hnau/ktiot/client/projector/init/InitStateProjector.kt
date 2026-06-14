package org.hnau.ktiot.client.projector.init

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.hnau.ktiot.client.projector.LoggedProjector
import org.hnau.ktiot.client.projector.LoginProjector

@Immutable
sealed interface InitStateProjector {

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    )

    val key: Int

    @Immutable
    data class Login(
        private val projector: LoginProjector,
    ) : InitStateProjector {

        @Composable
        override fun Content(
            contentPadding: PaddingValues,
        ) {
            projector.Content(
                contentPadding = contentPadding,
            )
        }

        override val key: Int
            get() = 0
    }

    @Immutable
    data class Logged(
        private val projector: LoggedProjector,
    ) : InitStateProjector {

        @Composable
        override fun Content(
            contentPadding: PaddingValues,
        ) {
            projector.Content(
                contentPadding = contentPadding,
            )
        }

        override val key: Int
            get() = 1
    }
}