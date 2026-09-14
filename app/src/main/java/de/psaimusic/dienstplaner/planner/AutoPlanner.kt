package de.psaimusic.dienstplaner.planner

import de.psaimusic.dienstplaner.data.Absence
import de.psaimusic.dienstplaner.data.Assignment
import de.psaimusic.dienstplaner.data.Employee
import de.psaimusic.dienstplaner.data.PlannerRules
import de.psaimusic.dienstplaner.data.ShiftTemplate
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PlanResult(
    val assignments: List<Assignment>,
    val warnings: List<String>
)

object AutoPlanner {
    fun generate(
        start: LocalDate,
        days: Int,
        employees: List<Employee>,
        shifts: List<ShiftTemplate>,
        absences: List<Absence>,
        rules: PlannerRules
    ): PlanResult {
        if (employees.isEmpty()) {
            return PlanResult(emptyList(), listOf("Noch keine Mitarbeiter angelegt."))
        }

        val assignments = mutableListOf<Assignment>()
        val warnings = mutableListOf<String>()
        val enabledShifts = shifts.filter { it.enabled }

        repeat(days.coerceIn(1, 31)) { offset ->
            val date = start.plusDays(offset.toLong())
            enabledShifts.forEach { shift ->
                repeat(shift.requiredEmployees.coerceAtLeast(1)) { slot ->
                    val eligible = employees
                        .filter { employee ->
                            canWork(
                                employee = employee,
                                date = date,
                                shift = shift,
                                assignments = assignments,
                                shifts = enabledShifts,
                                absences = absences,
                                rules = rules
                            )
                        }
                        .sortedWith(
                            compareBy<Employee> { employee ->
                                if (rules.distributeWeekendsFairly && isWeekend(date)) {
                                    assignments.count { a ->
                                        a.employeeId == employee.id && isWeekend(a.date)
                                    }
                                } else 0
                            }.thenBy { employee ->
                                assignments.count { it.employeeId == employee.id }
                            }.thenBy { it.name.lowercase() }
                        )

                    val picked = eligible.firstOrNull()
                    if (picked == null) {
                        warnings += "${date.dayOfMonth}.${date.monthValue}. · ${shift.name}: Platz ${slot + 1} unbesetzt"
                    } else {
                        assignments += Assignment(date, shift.id, picked.id)
                    }
                }
            }
        }

        return PlanResult(assignments, warnings.distinct())
    }

    private fun canWork(
        employee: Employee,
        date: LocalDate,
        shift: ShiftTemplate,
        assignments: List<Assignment>,
        shifts: List<ShiftTemplate>,
        absences: List<Absence>,
        rules: PlannerRules
    ): Boolean {
        if (absences.any { it.employeeId == employee.id && !date.isBefore(it.start) && !date.isAfter(it.end) }) {
            return false
        }

        if (isWeekend(date) && !employee.allowWeekend) return false
        if (shift.id == "night" && !employee.allowNight) return false
        if (assignments.any { it.employeeId == employee.id && it.date == date }) return false

        val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        val uniqueWorkDaysThisWeek = assignments
            .filter { it.employeeId == employee.id && !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
            .map { it.date }
            .distinct()
            .size
        if (uniqueWorkDaysThisWeek >= employee.maxDaysPerWeek) return false

        if (!employee.allowShiftChange) {
            val existingShift = assignments.firstOrNull { it.employeeId == employee.id }?.shiftId
            if (existingShift != null && existingShift != shift.id) return false
        }

        if (rules.avoidFastRotation) {
            val yesterday = assignments.firstOrNull { it.employeeId == employee.id && it.date == date.minusDays(1) }
            if (yesterday != null) {
                val badChange = (yesterday.shiftId == "night" && shift.id == "early") ||
                    (yesterday.shiftId == "late" && shift.id == "early")
                if (badChange) return false
            }
        }

        val candidateStart = shiftStart(date, shift)
        val previousEnd = assignments
            .filter { it.employeeId == employee.id }
            .mapNotNull { assignment ->
                shifts.firstOrNull { it.id == assignment.shiftId }?.let { previousShift ->
                    shiftEnd(assignment.date, previousShift)
                }
            }
            .filter { !it.isAfter(candidateStart) }
            .maxOrNull()

        if (previousEnd != null) {
            val restHours = Duration.between(previousEnd, candidateStart).toMinutes() / 60.0
            if (restHours < rules.minRestHours) return false
        }

        val workedPreviousDays = generateSequence(date.minusDays(1)) { it.minusDays(1) }
            .take(rules.maxConsecutiveDays)
            .takeWhile { previous -> assignments.any { it.employeeId == employee.id && it.date == previous } }
            .count()
        if (workedPreviousDays >= rules.maxConsecutiveDays) return false

        return true
    }

    private fun shiftStart(date: LocalDate, shift: ShiftTemplate): LocalDateTime =
        date.atTime(LocalTime.parse(shift.start))

    private fun shiftEnd(date: LocalDate, shift: ShiftTemplate): LocalDateTime {
        val start = LocalTime.parse(shift.start)
        val end = LocalTime.parse(shift.end)
        return if (end <= start) date.plusDays(1).atTime(end) else date.atTime(end)
    }

    private fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}
