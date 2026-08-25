package gr.thrylos.news.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.thrylos.news.feed.BookmarksScreen
import gr.thrylos.news.feed.FeedScreen
import gr.thrylos.news.profile.AuthorProfileScreen
import gr.thrylos.news.profile.SourceProfileScreen
import gr.thrylos.news.reader.ReaderScreen
import gr.thrylos.news.reader.media.MediaViewerScreen
import gr.thrylos.news.settings.SettingsScreen
import gr.thrylos.news.settings.backup.BackupScreen
import gr.thrylos.news.settings.filters.FiltersScreen
import gr.thrylos.news.settings.sources.SourceEditorScreen
import gr.thrylos.news.settings.sources.SourcesScreen
import gr.thrylos.news.settings.sync.SyncSettingsScreen
import gr.thrylos.news.update.UpdateHistoryScreen

@Composable
fun ThrylosNavGraph(
    pendingArticleId: String? = null,
    onPendingArticleConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingArticleId) {
        if (pendingArticleId != null) {
            navController.navigate(Routes.reader(pendingArticleId))
            onPendingArticleConsumed()
        }
    }

    NavHost(navController = navController, startDestination = Routes.FEED) {
        composable(Routes.FEED) {
            FeedScreen(
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSourceProfile = { name -> navController.navigate(Routes.sourceProfile(name)) },
            )
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.READER,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
        ) {
            ReaderScreen(
                onBack = { navController.popBackStack() },
                onOpenMedia = { articleId, index -> navController.navigate(Routes.mediaViewer(articleId, index)) },
                onOpenSourceProfile = { name -> navController.navigate(Routes.sourceProfile(name)) },
                onOpenAuthorProfile = { author -> navController.navigate(Routes.authorProfile(author)) },
            )
        }
        dialog(
            Routes.MEDIA_VIEWER,
            arguments = listOf(
                navArgument("articleId") { type = NavType.StringType },
                navArgument("index") { type = NavType.StringType },
            ),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            MediaViewerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.SOURCE_PROFILE,
            arguments = listOf(navArgument("sourceName") { type = NavType.StringType }),
        ) {
            SourceProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
            )
        }
        composable(
            Routes.AUTHOR_PROFILE,
            arguments = listOf(navArgument("author") { type = NavType.StringType }),
        ) {
            AuthorProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSources = { navController.navigate(Routes.SETTINGS_SOURCES) },
                onOpenFilters = { navController.navigate(Routes.SETTINGS_FILTERS) },
                onOpenSync = { navController.navigate(Routes.SETTINGS_SYNC) },
                onOpenBackup = { navController.navigate(Routes.SETTINGS_BACKUP) },
                onOpenUpdateHistory = { navController.navigate(Routes.SETTINGS_UPDATE_HISTORY) },
            )
        }
        composable(Routes.SETTINGS_SOURCES) {
            SourcesScreen(
                onBack = { navController.popBackStack() },
                onAddSource = { navController.navigate(Routes.sourceEditor()) },
                onEditSource = { navController.navigate(Routes.sourceEditor(it)) },
                onOpenSourceProfile = { name -> navController.navigate(Routes.sourceProfile(name)) },
            )
        }
        composable(
            Routes.SETTINGS_SOURCE_EDITOR,
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            SourceEditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_FILTERS) {
            FiltersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_SYNC) {
            SyncSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_UPDATE_HISTORY) {
            UpdateHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
