package gr.thrylos.news.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.thrylos.news.feed.BookmarksScreen
import gr.thrylos.news.feed.FeedScreen
import gr.thrylos.news.reader.ReaderScreen
import gr.thrylos.news.settings.SettingsScreen
import gr.thrylos.news.settings.backup.BackupScreen
import gr.thrylos.news.settings.filters.FiltersScreen
import gr.thrylos.news.settings.sources.SourceEditorScreen
import gr.thrylos.news.settings.sources.SourcesScreen
import gr.thrylos.news.settings.sync.SyncSettingsScreen

@Composable
fun ThrylosNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.FEED) {
        composable(Routes.FEED) {
            FeedScreen(
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
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
            ReaderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSources = { navController.navigate(Routes.SETTINGS_SOURCES) },
                onOpenFilters = { navController.navigate(Routes.SETTINGS_FILTERS) },
                onOpenSync = { navController.navigate(Routes.SETTINGS_SYNC) },
                onOpenBackup = { navController.navigate(Routes.SETTINGS_BACKUP) },
            )
        }
        composable(Routes.SETTINGS_SOURCES) {
            SourcesScreen(
                onBack = { navController.popBackStack() },
                onAddSource = { navController.navigate(Routes.sourceEditor()) },
                onEditSource = { navController.navigate(Routes.sourceEditor(it)) },
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
    }
}
