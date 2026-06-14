package org.hnau.ktiot.client.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import org.hnau.ktiot.client.app.createAppProjector
import org.hnau.ktiot.client.app.createPinFinAppSeed
import org.hnau.ktiot.client.model.init.InitModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.hnau.commons.app.model.app.AppViewModel

class AppActivity : ComponentActivity() {
    private val viewModel: AppViewModel<InitModel, InitModel.Skeleton> by viewModels {
        AppViewModel.Companion.factory(
            context = applicationContext,
            seed = createPinFinAppSeed(),
        )
    }

    private val goBackHandler: StateFlow<(() -> Unit)?>
        get() = viewModel.appModel.model.goBackHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initOnBackPressedDispatcherCallback()
        val projector =
            createAppProjector(
                scope = lifecycleScope,
                model = viewModel.appModel,
            )
        setContent {
            projector.Content()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() {
        if (useOnBackPressedDispatcher) {
            super.onBackPressed()
        }
        goBackHandler
            .value
            ?.invoke()
            ?: super.onBackPressed()
    }

    private fun initOnBackPressedDispatcherCallback() {
        if (!useOnBackPressedDispatcher) {
            return
        }
        val callback =
            object : OnBackPressedCallback(
                enabled = goBackHandler.value != null,
            ) {
                override fun handleOnBackPressed() {
                    goBackHandler.value?.invoke()
                }
            }
        lifecycleScope.launch {
            goBackHandler
                .map { it != null }
                .distinctUntilChanged()
                .collect { goBackIsAvailable ->
                    callback.isEnabled = goBackIsAvailable
                }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    companion object {
        private val useOnBackPressedDispatcher: Boolean = Build.VERSION.SDK_INT >= 33
    }
}
