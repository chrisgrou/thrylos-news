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
import gr.thrylos.news.matches.MatchesScreen
import gr.thrylos.news.matches.MatchesSettingsScreen
import gr.thrylos.news.profile.AuthorProfileScreen
import gr.thrylos.news.profile.SourceProfileScreen
import gr.thrylos.news.reader.ReaderScreen
import gr.thrylos.news.reader.media.MediaViewerScreen
import gr.thrylos.news.settings.SettingsScreen
import gr.thrylos.news.settings.backup.BackupScreen
import gr.thrylos.news.settings.filters.FilterEditorScreen
import gr.thrylos.news.settings.filters.FilterMatchesScreen
import gr.thrylos.news.settings.filters.FiltersScreen
import gr.thrylos.news.settings.sources.AddYouTubeChannelScreen
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
            // Explicitly clears back to Feed first: after the process was killed in the
            // background and gets recreated by the notification tap, NavHost's saved-state
            // restoration can put back whatever reader screen was open before it died —
            // landing on top of that restored stack isn't enough to guarantee the tapped
            // article is what's actually visible, so reset the stack outright instead of
            // just pushing on top of whatever might already be there.
            //
            // Deliberately no launchSingleTop here: NavController checks it against
            // whatever destination is on top of the back stack *before* popUpTo is
            // applied, so with the app already open on some other article, the reader
            // destination was already on top — singleTop matched it and reused that
            // existing back stack entry instead of pushing a new one, silently keeping
            // its old articleId argument (and the ReaderViewModel/SavedStateHandle built
            // from it). That's what made tapping a notification for a different article
            // bring the app forward without ever switching off the one already open.
            // popUpTo(FEED) alone already guarantees a single, fresh reader entry.
            navController.navigate(Routes.reader(pendingArticleId)) {
                popUpTo(Routes.FEED)
            }
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
                onOpenMatches = { navController.navigate(Routes.MATCHES) },
            )
        }
        composable(Routes.MATCHES) {
            MatchesScreen(onBack = { navController.popBackStack() })
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
                onEditSource = { navController.navigate(Routes.sourceEditor(it)) },
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
                onOpenMatches = { navController.navigate(Routes.SETTINGS_MATCHES) },
            )
        }
        composable(Routes.SETTINGS_MATCHES) {
            MatchesSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_SOURCES) {
            SourcesScreen(
                onBack = { navController.popBackStack() },
                onAddSource = { kind ->
                    // A YouTube channel goes through a resolve step first — see
                    // Routes.SETTINGS_SOURCES_ADD_YOUTUBE — rather than straight into
                    // the JSON editor like every other kind.
                    if (kind == "youtube") navController.navigate(Routes.SETTINGS_SOURCES_ADD_YOUTUBE)
                    else navController.navigate(Routes.sourceEditor(kind = kind))
                },
                onOpenSourceProfile = { name -> navController.navigate(Routes.sourceProfile(name)) },
            )
        }
        composable(Routes.SETTINGS_SOURCES_ADD_YOUTUBE) {
            AddYouTubeChannelScreen(
                onBack = { navController.popBackStack() },
                onResolved = { name, channelId, showShorts ->
                    // Pops this resolve step off the stack too, not just itself: from
                    // the editor, back should return straight to the sources list
                    // (where "Νέα πηγή" was tapped), the same as every other kind —
                    // not back through a resolve screen there's nothing left to do on.
                    navController.navigate(
                        Routes.sourceEditor(kind = "youtube", ytName = name, ytChannelId = channelId, ytExcludeShorts = !showShorts),
                    ) {
                        popUpTo(Routes.SETTINGS_SOURCES)
                    }
                },
                onManual = { showShorts ->
                    navController.navigate(Routes.sourceEditor(kind = "youtube", ytExcludeShorts = !showShorts)) {
                        popUpTo(Routes.SETTINGS_SOURCES)
                    }
                },
            )
        }
        composable(
            Routes.SETTINGS_SOURCE_EDITOR,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("kind") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("ytName") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("ytChannelId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("ytExcludeShorts") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            SourceEditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_FILTERS) {
            FiltersScreen(
                onBack = { navController.popBackStack() },
                onOpenEditor = { ruleId, defaultAction ->
                    navController.navigate(Routes.filterEditor(ruleId, defaultAction?.name))
                },
            )
        }
        // Its own destination rather than a boolean inside FiltersScreen: as an inline
        // branch, a swipe back popped the whole Φίλτρα destination and landed on the
        // Settings root instead of returning to the rule list.
        composable(
            Routes.SETTINGS_FILTER_EDITOR,
            arguments = listOf(
                navArgument("ruleId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("defaultAction") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            FilterEditorScreen(
                onBack = { navController.popBackStack() },
                onOpenMatches = { navController.navigate(Routes.SETTINGS_FILTER_MATCHES) },
            )
        }
        composable(Routes.SETTINGS_FILTER_MATCHES) {
            FilterMatchesScreen(
                onBack = { navController.popBackStack() },
                onOpenArticle = { navController.navigate(Routes.reader(it)) },
            )
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
