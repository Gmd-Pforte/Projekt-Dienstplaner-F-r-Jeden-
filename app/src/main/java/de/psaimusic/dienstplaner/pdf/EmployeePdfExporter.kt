package de.psaimusic.dienstplaner.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import de.psaimusic.dienstplaner.data.Absence
import de.psaimusic.dienstplaner.data.Assignment
import de.psaimusic.dienstplaner.data.Employee
import de.psaimusic.dienstplaner.data.ShiftTemplate
import de.psaimusic.dienstplaner.ui.absenceCode
import java.io.FileOutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object EmployeePdfExporter {
    fun printMonthlyPlan(
        context: Context,
        employee: Employee,
        month: YearMonth,
        assignments: List<Assignment>,
        shifts: List<ShiftTemplate>,
        absences: List<Absence>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val safeName = employee.name.replace(Regex("[^A-Za-z0-9ÄÖÜäöüß_-]"), "_")
        val jobName = "Dienstplan_${safeName}_${month}"
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(
            jobName,
            EmployeeSchedulePrintAdapter(context, employee, month, assignments, shifts, absences),
            attributes
        )
    }
}

private class EmployeeSchedulePrintAdapter(
    private val context: Context,
    private val employee: Employee,
    private val month: YearMonth,
    private val assignments: List<Assignment>,
    private val shifts: List<ShiftTemplate>,
    private val absences: List<Absence>
) : PrintDocumentAdapter() {

    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        attributes = newAttributes
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("Dienstplan_${employee.name}_${month}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback?.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        val printAttributes = attributes ?: PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .build()
        val document = PrintedPdfDocument(context, printAttributes)
        try {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                return
            }
            val page = document.startPage(0)
            drawPage(page.canvas, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat())
            document.finishPage(page)
            destination?.let { output ->
                FileOutputStream(output.fileDescriptor).use { document.writeTo(it) }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (t: Throwable) {
            callback?.onWriteFailed(t.message ?: "PDF konnte nicht erstellt werden")
        } finally {
            document.close()
        }
    }

    private fun drawPage(canvas: android.graphics.Canvas, width: Float, height: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val locale = Locale.GERMAN
        val margin = 36f
        val headerHeight = 92f
        val weekdaysHeight = 28f
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2 - headerHeight - weekdaysHeight
        val cellWidth = availableWidth / 7f
        val cellHeight = availableHeight / 6f

        paint.color = Color.rgb(25, 38, 55)
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Dienstplan – ${employee.name}", margin, margin + 24f, paint)

        paint.textSize = 15f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.DKGRAY
        val monthTitle = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        canvas.drawText(monthTitle.replaceFirstChar { it.titlecase(locale) }, margin, margin + 50f, paint)
        canvas.drawText("Max. ${employee.maxDaysPerWeek} Tage/Woche", margin, margin + 72f, paint)

        val monthAssignments = assignments.filter { it.employeeId == employee.id && YearMonth.from(it.date) == month }
        val monthAbsences = absences.filter { it.employeeId == employee.id && YearMonth.from(it.start) <= month && YearMonth.from(it.end) >= month }
        val nights = monthAssignments.count { it.shiftId == "night" }
        val vacationDays = (1..month.lengthOfMonth()).count { day ->
            val date = month.atDay(day)
            monthAbsences.any { it.type.equals("Urlaub", true) && !date.isBefore(it.start) && !date.isAfter(it.end) }
        }
        canvas.drawText("Dienste: ${monthAssignments.size}   •   Nachtdienste: $nights   •   Urlaubstage: $vacationDays", width - margin - 400f, margin + 50f, paint)

        val startY = margin + headerHeight
        val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 12f
        paint.color = Color.rgb(75, 85, 99)
        weekdays.forEachIndexed { index, label ->
            canvas.drawText(label, margin + index * cellWidth + 6f, startY + 18f, paint)
        }

        val gridTop = startY + weekdaysHeight
        val first = month.atDay(1)
        val leading = first.dayOfWeek.value - 1
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(210, 214, 220)
        }

        repeat(6) { row ->
            repeat(7) { column ->
                val left = margin + column * cellWidth
                val top = gridTop + row * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight
                canvas.drawRect(left, top, right, bottom, borderPaint)

                val index = row * 7 + column
                val day = index - leading + 1
                if (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    paint.style = Paint.Style.FILL
                    paint.typeface = Typeface.DEFAULT_BOLD
                    paint.textSize = 12f
                    paint.color = Color.rgb(31, 41, 55)
                    canvas.drawText(day.toString(), left + 6f, top + 16f, paint)

                    val absence = monthAbsences.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }
                    val assignment = monthAssignments.firstOrNull { it.date == date }
                    val label = if (absence != null) {
                        absenceCode(absence.type)
                    } else {
                        assignment?.let { a -> shifts.firstOrNull { it.id == a.shiftId }?.code }
                    }
                    if (!label.isNullOrBlank()) {
                        paint.textSize = 18f
                        paint.color = when {
                            absence != null -> Color.rgb(14, 165, 164)
                            assignment?.shiftId == "early" -> Color.rgb(59, 130, 246)
                            assignment?.shiftId == "late" -> Color.rgb(245, 158, 11)
                            assignment?.shiftId == "night" -> Color.rgb(124, 92, 230)
                            else -> Color.DKGRAY
                        }
                        canvas.drawText(label, left + 6f, top + 42f, paint)
                    }

                    assignment?.let { a ->
                        val shift = shifts.firstOrNull { it.id == a.shiftId }
                        paint.typeface = Typeface.DEFAULT
                        paint.textSize = 9f
                        paint.color = Color.DKGRAY
                        canvas.drawText("${shift?.start ?: ""}–${shift?.end ?: ""}", left + 6f, top + 58f, paint)
                    }
                }
            }
        }

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("F = Früh  •  S = Spät  •  N = Nacht  •  U = Urlaub  •  WF = Wunschfrei  •  K = Krank", margin, height - 16f, paint)
    }
}
