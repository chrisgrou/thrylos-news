package gr.thrylos.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import gr.thrylos.news.model.AppThemeMode
import gr.thrylos.news.navigation.ThrylosNavGraph
import gr.thrylos.news.theme.AppThemeViewModel
import gr.thrylos.news.theme.ThrylosTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val darkTheme = when (mode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            ThrylosTheme(darkTheme = darkTheme) {
                ThrylosNavGraph()
            }
        }
    }
}
