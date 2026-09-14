package de.psaimusic.dienstplaner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.psaimusic.dienstplaner.MainActivity
import de.psaimusic.dienstplaner.R
import de.psaimusic.dienstplaner.data.AppStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NextShiftWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val store = AppStore(context)
        val assignments = store.loadAssignments()
        val shifts = store.loadShifts()
        val employees = store.loadEmployees()
        val next = assignments
            .filter { !it.date.isBefore(LocalDate.now()) }
            .minWithOrNull(compareBy({ it.date }, { it.shiftId }))

        val views = RemoteViews(context.packageName, R.layout.widget_next_shift)
        if (next == null) {
            views.setTextViewText(R.id.widgetShift, "Noch kein Plan")
            views.setTextViewText(R.id.widgetDetail, "App öffnen und Dienstplan erstellen")
        } else {
            val shift = shifts.firstOrNull { it.id == next.shiftId }
            val employee = employees.firstOrNull { it.id == next.employeeId }
            views.setTextViewText(R.id.widgetShift, "${shift?.code ?: "?"} · ${shift?.start ?: "--"}–${shift?.end ?: "--"}")
            views.setTextViewText(
                R.id.widgetDetail,
                "${next.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} · ${employee?.name ?: "Mitarbeiter"}"
            )
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
        manager.updateAppWidget(widgetId, views)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextShiftWidget::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, NextShiftWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
