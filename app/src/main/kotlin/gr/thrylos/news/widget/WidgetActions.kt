package gr.thrylos.news.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors

/** The widget's refresh button: triggers a real sync (same WorkManager job as
 *  pull-to-refresh in the app) and immediately redraws with whatever's already in the
 *  DB — the widget picks up the newly-synced articles a bit later via
 *  [gr.thrylos.news.data.widget.WidgetUpdater], called by SyncWorker on completion. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        entryPoint.syncScheduler().syncNow()
        ThrylosGlanceWidget().updateAll(context)
    }
}
