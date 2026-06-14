package org.hnau.ktiot.client.projector.init

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.hnau.ktiot.client.projector.LoggedProjector
import org.hnau.ktiot.client.projector.LoginProjector

@Immutable
sealed interface InitStateProjector {

    @Composable
    fun Content()

    val key: Int

    @Immutable
    data class Login(
        private val projector: LoginProjector,
    ) : InitStateProjector {

        @Composable
        override fun Content() {
            projector.Content()
        }

        override val key: Int
            get() = 0
    }

    @Immutable
    data class Logged(
        private val projector: LoggedProjector,
    ) : InitStateProjector {

        @Composable
        override fun Content() {
            projector.Content()
        }

        override val key: Int
            get() = 1
    }
}