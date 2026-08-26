package gr.thrylos.news.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import gr.thrylos.news.data.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Real [WidgetUpdater] binding, provided in :app (see [WidgetModule]) since
 *  :core:data can't reference the Glance widget class directly. */
@Singleton
class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun requestUpdate() {
        scope.launch { ThrylosGlanceWidget().updateAll(context) }
    }
}
