package gr.thrylos.news.data.widget

/** Lets :core:data (which the home-screen widget lives outside of, in :app) ask the
 *  widget to re-render after a sync changes the article list — the real implementation
 *  is bound in :app, since :core:data can't reference the widget class directly. */
interface WidgetUpdater {
    fun requestUpdate()
}
