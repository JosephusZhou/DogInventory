package com.doginventory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        configureSplashWindow()
        setContentView(R.layout.activity_splash)

        val shareId = extractShareId(intent)

        lifecycleScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            val next = Intent(this@SplashActivity, MainActivity::class.java).apply {
                if (shareId != null) {
                    putExtra(MainActivity.EXTRA_PENDING_SHARE_ID, shareId)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
            startActivity(next)
            finish()
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        val shareId = extractShareId(newIntent) ?: return
        val next = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_PENDING_SHARE_ID, shareId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(next)
        finish()
    }

    private fun extractShareId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "https" && data.scheme != "doginv") return null
        val segments = data.pathSegments
        if (segments.size < 2 || segments[0] != "s") return null
        val id = segments[1].take(64).filter { it.isLetterOrDigit() }
        return id.ifBlank { null }
    }

    companion object {
        private const val SPLASH_DURATION_MILLIS = 2000L
    }

    private fun configureSplashWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

