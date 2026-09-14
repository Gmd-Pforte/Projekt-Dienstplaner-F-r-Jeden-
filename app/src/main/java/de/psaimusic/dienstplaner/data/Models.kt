package de.psaimusic.dienstplaner.data

import java.time.LocalDate

enum class AppScreen(val title: String) {
    HOME("Übersicht"),
    SCHEDULE("Dienstplan"),
    AUTOPLAN("Automatisch planen"),
    EMPLOYEES("Mitarbeiter"),
    SHIFTS("Schichten & Pausen"),
    ABSENCES("Frei & Urlaub"),
    RULES("Planungsregeln"),
    STATS("Auswertung"),
    SETTINGS("Einstellungen")
}

data class Employee(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val maxDaysPerWeek: Int = 5,
    val allowNight: Boolean = true,
    val allowShiftChange: Boolean = true,
    val allowWeekend: Boolean = true
)

data class ShiftTemplate(
    val id: String,
    val name: String,
    val code: String,
    val start: String,
    val end: String,
    val requiredEmployees: Int,
    val breakMinutes: Int,
    val enabled: Boolean = true
)

data class Absence(
    val id: Long = System.nanoTime(),
    val employeeId: Long,
    val type: String,
    val start: LocalDate,
    val end: LocalDate,
    val note: String = ""
)

data class Assignment(
    val date: LocalDate,
    val shiftId: String,
    val employeeId: Long
)

data class PlannerRules(
    val maxConsecutiveDays: Int = 5,
    val minRestHours: Int = 11,
    val avoidFastRotation: Boolean = true,
    val distributeWeekendsFairly: Boolean = true
)

data class AppSettings(
    val profession: String = "Sicherheitsdienst",
    val federalState: String = "Nordrhein-Westfalen",
    val friendlyHints: Boolean = true
)

val defaultShifts = listOf(
    ShiftTemplate("early", "Frühschicht", "F", "06:00", "14:00", 2, 30),
    ShiftTemplate("late", "Spätschicht", "S", "14:00", "22:00", 2, 30),
    ShiftTemplate("night", "Nachtschicht", "N", "22:00", "06:00", 1, 45)
)
