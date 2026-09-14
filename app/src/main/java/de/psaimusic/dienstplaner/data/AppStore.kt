package de.psaimusic.dienstplaner.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("dienstplaner_store", Context.MODE_PRIVATE)

    fun loadEmployees(): List<Employee> = runCatching {
        val array = JSONArray(prefs.getString("employees", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Employee(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        maxDaysPerWeek = o.optInt("maxDaysPerWeek", 5),
                        allowNight = o.optBoolean("allowNight", true),
                        allowShiftChange = o.optBoolean("allowShiftChange", true),
                        allowWeekend = o.optBoolean("allowWeekend", true)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun saveEmployees(items: List<Employee>) {
        val array = JSONArray()
        items.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("maxDaysPerWeek", e.maxDaysPerWeek)
                    .put("allowNight", e.allowNight)
                    .put("allowShiftChange", e.allowShiftChange)
                    .put("allowWeekend", e.allowWeekend)
            )
        }
        prefs.edit().putString("employees", array.toString()).apply()
    }

    fun loadShifts(): List<ShiftTemplate> = runCatching {
        val raw = prefs.getString("shifts", null) ?: return@runCatching defaultShifts
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    ShiftTemplate(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        code = o.getString("code"),
                        start = o.getString("start"),
                        end = o.getString("end"),
                        requiredEmployees = o.optInt("requiredEmployees", 1),
                        breakMinutes = o.optInt("breakMinutes", 30),
                        enabled = o.optBoolean("enabled", true)
                    )
                )
            }
        }
    }.getOrDefault(defaultShifts)

    fun saveShifts(items: List<ShiftTemplate>) {
        val array = JSONArray()
        items.forEach { s ->
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("code", s.code)
                    .put("start", s.start)
                    .put("end", s.end)
                    .put("requiredEmployees", s.requiredEmployees)
                    .put("breakMinutes", s.breakMinutes)
                    .put("enabled", s.enabled)
            )
        }
        prefs.edit().putString("shifts", array.toString()).apply()
    }

    fun loadAbsences(): List<Absence> = runCatching {
        val array = JSONArray(prefs.getString("absences", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Absence(
                        id = o.getLong("id"),
                        employeeId = o.getLong("employeeId"),
                        type = o.getString("type"),
                        start = LocalDate.parse(o.getString("start")),
                        end = LocalDate.parse(o.getString("end")),
                        note = o.optString("note")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun saveAbsences(items: List<Absence>) {
        val array = JSONArray()
        items.forEach { a ->
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("employeeId", a.employeeId)
                    .put("type", a.type)
                    .put("start", a.start.toString())
                    .put("end", a.end.toString())
                    .put("note", a.note)
            )
        }
        prefs.edit().putString("absences", array.toString()).apply()
    }

    fun loadRules(): PlannerRules = PlannerRules(
        maxConsecutiveDays = prefs.getInt("rule_max_consecutive", 5),
        minRestHours = prefs.getInt("rule_min_rest", 11),
        avoidFastRotation = prefs.getBoolean("rule_no_fast_rotation", true),
        distributeWeekendsFairly = prefs.getBoolean("rule_fair_weekends", true)
    )

    fun saveRules(rules: PlannerRules) {
        prefs.edit()
            .putInt("rule_max_consecutive", rules.maxConsecutiveDays)
            .putInt("rule_min_rest", rules.minRestHours)
            .putBoolean("rule_no_fast_rotation", rules.avoidFastRotation)
            .putBoolean("rule_fair_weekends", rules.distributeWeekendsFairly)
            .apply()
    }

    fun loadSettings(): AppSettings = AppSettings(
        profession = prefs.getString("profession", "Sicherheitsdienst") ?: "Sicherheitsdienst",
        federalState = prefs.getString("federal_state", "Nordrhein-Westfalen") ?: "Nordrhein-Westfalen",
        friendlyHints = prefs.getBoolean("friendly_hints", true)
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("profession", settings.profession)
            .putString("federal_state", settings.federalState)
            .putBoolean("friendly_hints", settings.friendlyHints)
            .apply()
    }
}
