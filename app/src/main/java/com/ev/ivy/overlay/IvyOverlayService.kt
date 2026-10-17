package com.ev.ivy.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import com.ev.ivy.ui.IvyOrb

class IvyOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateController.savedStateRegistry
    private var overlay: ComposeView? = null
    private lateinit var manager: WindowManager

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) return START_NOT_STICKY
        if (overlay != null) return START_NOT_STICKY
        overlay = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IvyOverlayService)
            setViewTreeViewModelStoreOwner(this@IvyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IvyOverlayService)
            setContent {
                MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF7DBBFF))) {
                    Row(Modifier.fillMaxWidth().background(Color(0xF20C1423), RoundedCornerShape(30.dp)).padding(14.dp)) {
                        IvyOrb(Modifier.size(70.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("E.V  /  IVY", color = Color.White)
                            Text("Prévia visual • toque para fechar", color = Color(0xFF91A6C4), style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { stopSelf() })
                        }
                        Text("✕", color = Color.White, modifier = Modifier.clickable { stopSelf() }.padding(8.dp))
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM; x = 0; y = 85 }
        manager.addView(overlay, params)
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        overlay?.let { manager.removeView(it) }
        overlay = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
