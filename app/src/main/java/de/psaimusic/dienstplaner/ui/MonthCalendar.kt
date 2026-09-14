package de.psaimusic.dienstplaner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.psaimusic.dienstplaner.data.Absence
import de.psaimusic.dienstplaner.data.Assignment
import de.psaimusic.dienstplaner.data.Employee
import de.psaimusic.dienstplaner.data.ShiftTemplate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthCalendar(
    month: YearMonth,
    assignments: List<Assignment>,
    shifts: List<ShiftTemplate>,
    employees: List<Employee>,
    absences: List<Absence>,
    selectedDate: LocalDate?,
    onSelectedDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    employeeId: Long? = null,
    modifier: Modifier = Modifier
) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val cellCount = leading + month.lengthOfMonth()
    val weekCount = (cellCount + 6) / 7
    val locale = Locale.GERMAN

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Monat")
            }
            Text(
                month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { label ->
                Box(Modifier.weight(1f).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        repeat(weekCount) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { column ->
                    val index = week * 7 + column
                    val dayNumber = index - leading + 1
                    if (dayNumber !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).aspectRatio(0.92f))
                    } else {
                        val date = month.atDay(dayNumber)
                        CalendarDay(
                            date = date,
                            assignments = assignments,
                            shifts = shifts,
                            employees = employees,
                            absences = absences,
                            employeeId = employeeId,
                            selected = selectedDate == date,
                            onClick = { onSelectedDate(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    assignments: List<Assignment>,
    shifts: List<ShiftTemplate>,
    employees: List<Employee>,
    absences: List<Absence>,
    employeeId: Long?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val dayAssignments = assignments.filter { it.date == date && (employeeId == null || it.employeeId == employeeId) }
    val dayAbsences = absences.filter {
        !date.isBefore(it.start) && !date.isAfter(it.end) && (employeeId == null || it.employeeId == employeeId)
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        date == LocalDate.now() -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.92f)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = background,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(Modifier.padding(6.dp)) {
            Text(
                date.dayOfMonth.toString(),
                fontWeight = if (date == LocalDate.now() || selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(3.dp))

            if (employeeId != null) {
                val absence = dayAbsences.firstOrNull()
                if (absence != null) {
                    MiniBadge(
                        text = absenceCode(absence.type),
                        color = absenceColor(absence.type)
                    )
                } else {
                    dayAssignments.firstOrNull()?.let { assignment ->
                        val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                        MiniBadge(text = shift?.code ?: "?", color = shiftColorV3(assignment.shiftId))
                    }
                }
            } else {
                val grouped = dayAssignments.groupBy { it.shiftId }
                grouped.entries.take(3).forEach { (shiftId, items) ->
                    val shift = shifts.firstOrNull { it.id == shiftId }
                    MiniBadge(
                        text = "${shift?.code ?: "?"} ${items.size}",
                        color = shiftColorV3(shiftId)
                    )
                    Spacer(Modifier.height(2.dp))
                }
                if (dayAbsences.isNotEmpty()) {
                    MiniBadge(text = "A ${dayAbsences.size}", color = Color(0xFF27A98C))
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

fun shiftColorV3(shiftId: String): Color = when (shiftId) {
    "early" -> Color(0xFF3B82F6)
    "late" -> Color(0xFFF59E0B)
    "night" -> Color(0xFF7C5CE6)
    else -> Color(0xFF64748B)
}

fun absenceColor(type: String): Color = when (type.lowercase()) {
    "urlaub" -> Color(0xFF0EA5A4)
    "frei", "wunschfrei" -> Color(0xFF22A447)
    "krank" -> Color(0xFFE05252)
    else -> Color(0xFF64748B)
}

fun absenceCode(type: String): String = when (type.lowercase()) {
    "urlaub" -> "U"
    "frei" -> "Frei"
    "wunschfrei" -> "WF"
    "krank" -> "K"
    else -> type.take(2).uppercase()
}
