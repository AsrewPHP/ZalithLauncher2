package com.movtery.zalithlauncher.ui.base

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.CallSuper
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.context.refreshContext
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils.Companion.checkPermissionsForInit
import org.lwjgl.glfw.CallbackBridge
import kotlin.math.min

open class BaseComponentActivity : FullScreenComponentActivity() {
    private var notchSize = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshContext(this)
        refreshDisplayMetrics()
        checkStoragePermissions()

        //加载渲染器
        Renderers.init()
        //加载插件
        PluginLoader.loadAllPlugins(this, false)
        //刷新游戏目录
        GamePathManager.reloadPath()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        checkStoragePermissions()
        AccountsManager.reloadAccounts()
    }

    override fun onAttachedToWindow() {
        computeNotchSize()
    }

    private fun checkStoragePermissions() {
        //检查所有文件管理权限
        checkPermissionsForInit(this)
    }

    protected fun refreshDisplayMetrics() {
        ZLApplication.DISPLAY_METRICS = getDisplayMetrics()
        CallbackBridge.physicalWidth = ZLApplication.DISPLAY_METRICS.widthPixels
        CallbackBridge.physicalHeight = ZLApplication.DISPLAY_METRICS.heightPixels
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L598-L620)
     */
    @Suppress("DEPRECATION")
    private fun getDisplayMetrics(): DisplayMetrics {
        var displayMetrics = DisplayMetrics()

        if (isInMultiWindowMode || isInPictureInPictureMode) {
            //For devices with free form/split screen, we need window size, not screen size.
            displayMetrics = resources.displayMetrics
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display.getRealMetrics(displayMetrics)
            } else { // Removed the clause for devices with unofficial notch support, since it also ruins all devices with virtual nav bars before P
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            if (!shouldIgnoreNotch()) {
                //Remove notch width when it isn't ignored.
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) displayMetrics.heightPixels -= notchSize
                else displayMetrics.widthPixels -= notchSize
            }
        }
        return displayMetrics
    }

    /**
     * Compute the notch size to avoid being out of bounds
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/5de6822/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java#L196-L219)
     */
    private fun computeNotchSize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        runCatching {
            val cutout: Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                windowManager.currentWindowMetrics.getWindowInsets().displayCutout!!.getBoundingRects()[0]
            } else {
                window.decorView.getRootWindowInsets().displayCutout!!.getBoundingRects()[0]
            }

            // Notch values are rotation sensitive, handle all cases
            val orientation: Int = resources.configuration.orientation
            notchSize = when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> cutout.height()
                Configuration.ORIENTATION_LANDSCAPE -> cutout.width()
                else -> min(cutout.width(), cutout.height())
            }
        }.onFailure {
            Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode")
            notchSize = -1
        }
    }

    protected fun runFinish() = run { finish() }
}