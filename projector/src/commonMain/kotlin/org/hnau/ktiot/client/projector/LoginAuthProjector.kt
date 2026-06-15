package org.hnau.ktiot.client.projector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.projector.fractal.input.InputProjector
import org.hnau.commons.app.projector.fractal.input.type.toInputProjectorPrototype
import org.hnau.commons.app.projector.fractal.table.STableScope
import org.hnau.commons.app.projector.utils.Drawable
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.ktiot.client.model.LoginAuthModel
import org.hnau.ktiot.client.projector.utils.Localization

class LoginAuthProjector(
    scope: CoroutineScope,
    dependencies: Dependencies,
    model: LoginAuthModel,
) {

    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    private val user: InputProjector = model
        .user
        .toInputProjectorPrototype(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Ascii,
        )
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.user,
            icon = Drawable.Vector(Icons.Default.Person),
        ) { _, _ -> dependencies.localization.userIsEmptyError }

    private val password: InputProjector = model
        .password
        .toInputProjectorPrototype(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password,
        )
        .createInputProjector(
            scope = scope,
            title = dependencies.localization.password,
            icon = Drawable.Vector(Icons.Default.Password),
        ) { _, _ -> dependencies.localization.passwordIsEmptyError }

    @Composable
    fun STableScope.Content() {
        SCell { user.Content() }
        SCell { password.Content() }
    }
}