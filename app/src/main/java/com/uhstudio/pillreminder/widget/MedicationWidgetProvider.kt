package com.uhstudio.pillreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.uhstudio.pillreminder.MainActivity
import com.uhstudio.pillreminder.R

/**
 * Implementation of App Widget functionality.
 */
class MedicationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // updates each AppWidget with RemoteViews
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_medication)

    // Set Text (Mock Data for now as per plan/design)
    views.setTextViewText(R.id.text_next_dose_time, "10:30 AM") // Mock
    views.setTextViewText(R.id.text_next_dose_name, "Multivitamin • 1 capsule") // Mock
    
    // Progress
    views.setTextViewText(R.id.text_progress_count, "2/4") // Mock
    views.setTextViewText(R.id.text_progress_label, "Done Today")
    views.setTextViewText(R.id.text_progress_status, "Feeling Great!")

    // Click handler to open app
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context, 
        0, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
