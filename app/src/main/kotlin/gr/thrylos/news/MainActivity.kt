package gr.thrylos.news

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import gr.thrylos.news.data.notifications.EXTRA_ARTICLE_ID
import gr.thrylos.news.model.AppThemeMode
import gr.thrylos.news.navigation.ThrylosNavGraph
import gr.thrylos.news.theme.AppThemeViewModel
import gr.thrylos.news.theme.ThrylosTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingArticleId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingArticleId.value = intent?.getStringExtra(EXTRA_ARTICLE_ID)
        setContent {
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val darkTheme = when (mode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val articleId by pendingArticleId
            ThrylosTheme(darkTheme = darkTheme) {
                ThrylosNavGraph(
                    pendingArticleId = articleId,
                    onPendingArticleConsumed = { pendingArticleId.value = null },
                )
            }
        }
    }

    /** A notification tap while the app is already running delivers here instead of a
     *  fresh onCreate — re-point pendingArticleId so the nav graph's LaunchedEffect
     *  fires again even if it's the same article id as before. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingArticleId.value = intent.getStringExtra(EXTRA_ARTICLE_ID)
    }
}
