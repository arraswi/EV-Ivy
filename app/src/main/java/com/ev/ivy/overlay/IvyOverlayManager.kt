package com.ev.ivy.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ev.ivy.assistant.AssistantController
import com.ev.ivy.assistant.AssistantState
import com.ev.ivy.data.settings.SettingsRepository
import com.ev.ivy.ui.theme.EvTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class IvyOverlayManager(
    context: Context,
    private val controller: AssistantController,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val appContext = context.applicationContext
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateController.savedStateRegistry

    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var view: ComposeView? = null
    private var collectJob: Job? = null

    fun attach() {
        if (collectJob != null) return
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        collectJob = scope.launch {
            controller.state.collect { state ->
                if (state is AssistantState.Idle) removeView() else addView()
            }
        }
    }

    fun detach() {
        collectJob?.cancel()
        collectJob = null
        removeView()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }

    private fun addView() {
        if (view != null) return
        if (!Settings.canDrawOverlays(appContext)) return
        val composeView = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(this@IvyOverlayManager)
            setViewTreeViewModelStoreOwner(this@IvyOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@IvyOverlayManager)
            setContent {
                val s by settings.state.collectAsState()
                EvTheme(accent = Color(s.accentColor)) {
                    IvyGlassOverlay(controller = controller, settings = settings, s = s)
                }
            }
        }
        val params = buildParams()
        try {
            windowManager.addView(composeView, params)
            view = composeView
        } catch (_: Exception) {
            view = null
        }
    }

    private fun removeView() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        view = null
    }

    private fun buildParams(): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                params.setBlurBehindRadius(dpToPx(settings.state.value.blurRadiusDp))
            } catch (_: Exception) {
            }
        }
        return params
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        appContext.resources.displayMetrics
    ).toInt()
}
