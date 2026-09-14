package de.psaimusic.dienstplaner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.psaimusic.dienstplaner.data.Absence
import de.psaimusic.dienstplaner.data.AppScreen
import de.psaimusic.dienstplaner.data.AppSettings
import de.psaimusic.dienstplaner.data.AppStore
import de.psaimusic.dienstplaner.data.Assignment
import de.psaimusic.dienstplaner.data.Employee
import de.psaimusic.dienstplaner.data.PlannerRules
import de.psaimusic.dienstplaner.data.ShiftTemplate
import de.psaimusic.dienstplaner.planner.AutoPlanner
import de.psaimusic.dienstplaner.planner.PlanResult
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DienstplanApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }

    var employees by remember { mutableStateOf(store.loadEmployees()) }
    var shifts by remember { mutableStateOf(store.loadShifts()) }
    var absences by remember { mutableStateOf(store.loadAbsences()) }
    var rules by remember { mutableStateOf(store.loadRules()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var plan by remember { mutableStateOf(PlanResult(emptyList(), emptyList())) }

    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    val screen = AppScreen.valueOf(screenName)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEmployeeDialog by remember { mutableStateOf(false) }
    var showAbsenceDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Dienstplaner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    "Für jeden Job. Einfach geplant.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                AppScreen.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = screen == item,
                        icon = { Icon(screenIcon(item), null) },
                        onClick = {
                            screenName = item.name
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Text(
                    "0.1 · Offline-Modus aktiv",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screen.title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    }
                )
            },
            floatingActionButton = {
                when (screen) {
                    AppScreen.EMPLOYEES -> FloatingActionButton(onClick = { showEmployeeDialog = true }) {
                        Icon(Icons.Default.Add, "Mitarbeiter hinzufügen")
                    }
                    AppScreen.ABSENCES -> FloatingActionButton(onClick = { showAbsenceDialog = true }) {
                        Icon(Icons.Default.Add, "Abwesenheit hinzufügen")
                    }
                    else -> Unit
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    AppScreen.HOME -> HomeScreen(employees, shifts, absences, plan, settings) {
                        screenName = it.name
                    }
                    AppScreen.SCHEDULE -> ScheduleScreen(plan.assignments, employees, shifts)
                    AppScreen.AUTOPLAN -> AutoPlanScreen(
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        plan = plan,
                        onGenerate = {
                            val monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                            plan = AutoPlanner.generate(monday, 7, employees, shifts, absences, rules)
                        }
                    )
                    AppScreen.EMPLOYEES -> EmployeesScreen(
                        employees = employees,
                        onDelete = { target ->
                            employees = employees.filterNot { it.id == target.id }
                            absences = absences.filterNot { it.employeeId == target.id }
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                        }
                    )
                    AppScreen.SHIFTS -> ShiftsScreen(shifts) { updated ->
                        shifts = shifts.map { if (it.id == updated.id) updated else it }
                        store.saveShifts(shifts)
                    }
                    AppScreen.ABSENCES -> AbsencesScreen(absences, employees) { target ->
                        absences = absences.filterNot { it.id == target.id }
                        store.saveAbsences(absences)
                    }
                    AppScreen.RULES -> RulesScreen(rules) {
                        rules = it
                        store.saveRules(it)
                    }
                    AppScreen.STATS -> StatsScreen(plan, shifts, employees)
                    AppScreen.SETTINGS -> SettingsScreen(settings) {
                        settings = it
                        store.saveSettings(it)
                    }
                }
            }
        }
    }

    if (showEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showEmployeeDialog = false },
            onAdd = { employee ->
                employees = employees + employee
                store.saveEmployees(employees)
                showEmployeeDialog = false
            }
        )
    }

    if (showAbsenceDialog) {
        AddAbsenceDialog(
            employees = employees,
            onDismiss = { showAbsenceDialog = false },
            onAdd = { absence ->
                absences = absences + absence
                store.saveAbsences(absences)
                showAbsenceDialog = false
            }
        )
    }
}

@Composable
private fun HomeScreen(
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    plan: PlanResult,
    settings: AppSettings,
    navigate: (AppScreen) -> Unit
) {
    val upcomingVacation = absences
        .filter { it.type == "Urlaub" && !it.start.isBefore(LocalDate.now()) }
        .minByOrNull { it.start }
    val vacationDays = upcomingVacation?.let { ChronoUnit.DAYS.between(LocalDate.now(), it.start) }
    val nextAssignment = plan.assignments.filter { !it.date.isBefore(LocalDate.now()) }.minByOrNull { it.date }
    val nextShift = shifts.firstOrNull { it.id == nextAssignment?.shiftId }
    val nextEmployee = employees.firstOrNull { it.id == nextAssignment?.employeeId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Hallo 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${settings.profession} · ${settings.federalState}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            FriendlyCard(
                title = "Nächster geplanter Dienst",
                big = if (nextShift == null) "Noch kein Plan" else "${nextShift.code} · ${nextShift.start}–${nextShift.end}",
                detail = if (nextAssignment == null) "Erstelle deinen ersten Wochenplan." else "${formatDate(nextAssignment.date)} · ${nextEmployee?.name ?: "Mitarbeiter"}"
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("🏖️ Urlaub", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (vacationDays == null) "Noch kein Urlaub eingetragen" else "Noch $vacationDays Tage",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (upcomingVacation == null) "Eintragen und Vorfreude einschalten 😎" else "Start: ${formatDate(upcomingVacation.start)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickCard("✨ Planen", "Automatik", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
                QuickCard("👥 Team", "${employees.size} Personen", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickCard("📅 Plan", "${plan.assignments.size} Dienste", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                QuickCard("🏖️ Frei", "${absences.size} Einträge", Modifier.weight(1f)) { navigate(AppScreen.ABSENCES) }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bereit für Team-Sync", fontWeight = FontWeight.SemiBold)
                    Text(
                        "0.1 speichert alles lokal. Die Struktur ist bereits so angelegt, dass später Chef- und Mitarbeiterkonten synchronisiert werden können.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ScheduleScreen(assignments: List<Assignment>, employees: List<Employee>, shifts: List<ShiftTemplate>) {
    if (assignments.isEmpty()) {
        EmptyState("Noch kein Dienstplan", "Öffne „Automatisch planen“ und erstelle einen ersten Vorschlag.")
        return
    }
    val grouped = assignments.groupBy { it.date }.toSortedMap()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (date, dayAssignments) ->
            item {
                Text(formatDateLong(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(dayAssignments) { assignment ->
                val employee = employees.firstOrNull { it.id == assignment.employeeId }
                val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                ShiftAssignmentCard(employee?.name ?: "Unbekannt", shift)
            }
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AutoPlanScreen(
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    rules: PlannerRules,
    plan: PlanResult,
    onGenerate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FriendlyCard(
                title = "Plan-Assistent",
                big = "Eine Woche mit einem Tipp",
                detail = "Berücksichtigt Mitarbeiterregeln, Urlaub, Wochenenden und Schichtwechsel."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${employees.size} Mitarbeiter") })
                AssistChip(onClick = {}, label = { Text("${shifts.count { it.enabled }} Schichten") })
            }
        }
        item {
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("DIENSTPLAN ERSTELLEN", fontWeight = FontWeight.Bold)
            }
        }
        if (plan.assignments.isNotEmpty()) {
            item {
                Text("Vorschlag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${plan.assignments.size} Dienste wurden verteilt.")
            }
        }
        if (plan.warnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null)
                            Spacer(Modifier.width(8.dp))
                            Text("${plan.warnings.size} Konflikte", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        plan.warnings.take(8).forEach { Text("• $it") }
                        if (plan.warnings.size > 8) Text("… und ${plan.warnings.size - 8} weitere")
                    }
                }
            }
        }
        if (employees.isEmpty()) {
            item { EmptyInline("Lege zuerst Mitarbeiter an, damit die Automatik etwas verteilen kann.") }
        }
        item {
            Text(
                "Regeln aktiv: max. ${rules.maxConsecutiveDays} Tage am Stück · ${rules.minRestHours} h Ruhezeit",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun EmployeesScreen(employees: List<Employee>, onDelete: (Employee) -> Unit) {
    if (employees.isEmpty()) {
        EmptyState("Noch niemand im Team", "Tippe auf + und lege den ersten Mitarbeiter mit seinen Regeln an.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(employees, key = { it.id }) { employee ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(employee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Max. ${employee.maxDaysPerWeek} Tage/Woche", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onDelete(employee) }) { Text("Entfernen") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOfNotNull(
                            if (!employee.allowNight) "keine Nacht" else null,
                            if (!employee.allowShiftChange) "keine Wechselschicht" else null,
                            if (!employee.allowWeekend) "kein Wochenende" else null
                        ).ifEmpty { listOf("alle Schichten erlaubt") }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ShiftsScreen(shifts: List<ShiftTemplate>, onUpdate: (ShiftTemplate) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Schichten & Pausen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Besetzung und Pausen direkt am Schichttyp einstellen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(shifts, key = { it.id }) { shift ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).background(shiftColor(shift.id), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(shift.code, color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shift.name, fontWeight = FontWeight.Bold)
                            Text("${shift.start} – ${shift.end}")
                        }
                        Switch(checked = shift.enabled, onCheckedChange = { onUpdate(shift.copy(enabled = it)) })
                    }
                    StepperRow("Mindestbesetzung", shift.requiredEmployees, 1, 10) {
                        onUpdate(shift.copy(requiredEmployees = it))
                    }
                    StepperRow("Pause (Min.)", shift.breakMinutes, 0, 120, step = 15) {
                        onUpdate(shift.copy(breakMinutes = it))
                    }
                }
            }
        }
        item {
            EmptyInline("Eigene Schichttypen und getrennte Pausenfenster kommen als nächster Ausbau hinzu.")
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AbsencesScreen(absences: List<Absence>, employees: List<Employee>, onDelete: (Absence) -> Unit) {
    if (absences.isEmpty()) {
        EmptyState("Noch kein Frei oder Urlaub", "Tippe auf + und trage Urlaub, Freiwunsch oder Abwesenheit ein.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(absences.sortedBy { it.start }, key = { it.id }) { absence ->
            val name = employees.firstOrNull { it.id == absence.employeeId }?.name ?: "Mitarbeiter"
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (absence.type == "Urlaub") "🏖️" else "📌", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${absence.type} · $name", fontWeight = FontWeight.Bold)
                        Text("${formatDate(absence.start)} – ${formatDate(absence.end)}")
                    }
                    TextButton(onClick = { onDelete(absence) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RulesScreen(rules: PlannerRules, onChange: (PlannerRules) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { RuleSlider("Max. Arbeitstage am Stück", rules.maxConsecutiveDays, 1f..7f) { onChange(rules.copy(maxConsecutiveDays = it)) } }
        item { RuleSlider("Mindestruhezeit", rules.minRestHours, 8f..16f, suffix = " h") { onChange(rules.copy(minRestHours = it)) } }
        item {
            ToggleCard("Schnelle Wechsel vermeiden", "Keine Nacht→Früh- oder Spät→Früh-Folge.", rules.avoidFastRotation) {
                onChange(rules.copy(avoidFastRotation = it))
            }
        }
        item {
            ToggleCard("Wochenenden fair verteilen", "Für spätere Planungsstufen vorbereitet.", rules.distributeWeekendsFairly) {
                onChange(rules.copy(distributeWeekendsFairly = it))
            }
        }
        item { EmptyInline("Individuelle Regeln pro Mitarbeiter werden zusätzlich direkt beim Mitarbeiter gespeichert.") }
    }
}

@Composable
private fun StatsScreen(plan: PlanResult, shifts: List<ShiftTemplate>, employees: List<Employee>) {
    val nights = plan.assignments.count { it.shiftId == "night" }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MetricCard("Mitarbeiter", employees.size.toString(), "im aktuellen Team") }
        item { MetricCard("Geplante Dienste", plan.assignments.size.toString(), "im aktuellen Vorschlag") }
        item { MetricCard("Nachtdienste", nights.toString(), "aktuell verteilt") }
        item { MetricCard("Offene Konflikte", plan.warnings.size.toString(), if (plan.warnings.isEmpty()) "sieht gut aus 👍" else "bitte im Plan-Assistenten prüfen") }
        item { EmptyInline("Lohnstunden, Zuschläge und Gehaltsvergleich werden auf dieser Basis ergänzt.") }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val professions = listOf("Sicherheitsdienst", "Pflege", "Gastronomie", "Logistik", "Produktion", "Einzelhandel", "Reinigung")
    val states = listOf("Nordrhein-Westfalen", "Bayern", "Baden-Württemberg", "Hessen", "Niedersachsen", "Berlin", "Hamburg")
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Berufsgruppe", fontWeight = FontWeight.Bold)
            Text("Damit können später passende Schichten und Gehaltswerte vorgeschlagen werden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(professions) { profession ->
            FilterChip(
                selected = settings.profession == profession,
                onClick = { onChange(settings.copy(profession = profession)) },
                label = { Text(profession) }
            )
        }
        item { Spacer(Modifier.height(6.dp)); Text("Bundesland", fontWeight = FontWeight.Bold) }
        items(states) { state ->
            FilterChip(
                selected = settings.federalState == state,
                onClick = { onChange(settings.copy(federalState = state)) },
                label = { Text(state) }
            )
        }
        item {
            ToggleCard("Freundliche Hinweise", "Urlaubs-Countdown und kleine motivierende Texte anzeigen.", settings.friendlyHints) {
                onChange(settings.copy(friendlyHints = it))
            }
        }
        item {
            EmptyInline("Feiertage je Bundesland, offizielle Entgeltwerte und Team-Synchronisation sind für die nächsten Versionen vorbereitet.")
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AddEmployeeDialog(onDismiss: () -> Unit, onAdd: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var maxDays by remember { mutableStateOf(5) }
    var allowNight by remember { mutableStateOf(true) }
    var allowShiftChange by remember { mutableStateOf(true) }
    var allowWeekend by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitarbeiter anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                StepperRow("Max. Tage/Woche", maxDays, 1, 7) { maxDays = it }
                SwitchLine("Nachtschicht erlaubt", allowNight) { allowNight = it }
                SwitchLine("Wechselschichten erlaubt", allowShiftChange) { allowShiftChange = it }
                SwitchLine("Wochenende erlaubt", allowWeekend) { allowWeekend = it }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onAdd(Employee(name = name.trim(), maxDaysPerWeek = maxDays, allowNight = allowNight, allowShiftChange = allowShiftChange, allowWeekend = allowWeekend))
                }
            ) { Text("Anlegen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun AddAbsenceDialog(employees: List<Employee>, onDismiss: () -> Unit, onAdd: (Absence) -> Unit) {
    var selectedId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var type by remember { mutableStateOf("Urlaub") }
    var startText by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var endText by remember { mutableStateOf(LocalDate.now().plusDays(13).toString()) }
    val start = runCatching { LocalDate.parse(startText) }.getOrNull()
    val end = runCatching { LocalDate.parse(endText) }.getOrNull()
    val valid = selectedId != null && start != null && end != null && !end.isBefore(start)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frei / Urlaub eintragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (employees.isEmpty()) {
                    Text("Lege zuerst einen Mitarbeiter an.")
                } else {
                    Text("Mitarbeiter", fontWeight = FontWeight.Bold)
                    employees.forEach { employee ->
                        FilterChip(
                            selected = selectedId == employee.id,
                            onClick = { selectedId = employee.id },
                            label = { Text(employee.name) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "Urlaub", onClick = { type = "Urlaub" }, label = { Text("🏖️ Urlaub") })
                        FilterChip(selected = type == "Freiwunsch", onClick = { type = "Freiwunsch" }, label = { Text("📌 Freiwunsch") })
                    }
                    OutlinedTextField(startText, { startText = it }, label = { Text("Von (JJJJ-MM-TT)") }, singleLine = true)
                    OutlinedTextField(endText, { endText = it }, label = { Text("Bis (JJJJ-MM-TT)") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onAdd(Absence(employeeId = selectedId!!, type = type, start = start!!, end = end!!)) }
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun FriendlyCard(title: String, big: String, detail: String) {
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(big, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickCard(title: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShiftAssignmentCard(name: String, shift: ShiftTemplate?) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(shiftColor(shift?.id ?: ""), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) { Text(shift?.code ?: "?", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text("${shift?.name ?: "Schicht"} · ${shift?.start ?: "--"}–${shift?.end ?: "--"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
    }
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ToggleCard(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun RuleSlider(title: String, value: Int, range: ClosedFloatingPointRange<Float>, suffix: String = " Tage", onValue: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("$value$suffix", style = MaterialTheme.typography.headlineSmall)
            Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = range, steps = (range.endInclusive - range.start - 1).toInt().coerceAtLeast(0))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("☕", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyInline(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun shiftColor(id: String): Color = when (id) {
    "early" -> Color(0xFF3C78D8)
    "late" -> Color(0xFFE69138)
    "night" -> Color(0xFF674EA7)
    else -> Color(0xFF607D8B)
}

private fun screenIcon(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.HOME -> Icons.Default.Home
    AppScreen.SCHEDULE -> Icons.Default.DateRange
    AppScreen.AUTOPLAN -> Icons.Default.PlayArrow
    AppScreen.EMPLOYEES -> Icons.Default.People
    AppScreen.SHIFTS -> Icons.Default.Schedule
    AppScreen.ABSENCES -> Icons.Default.BeachAccess
    AppScreen.RULES -> Icons.Default.List
    AppScreen.STATS -> Icons.Default.BarChart
    AppScreen.SETTINGS -> Icons.Default.Settings
}

private val shortFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val longFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.")
private fun formatDate(date: LocalDate): String = date.format(shortFormatter)
private fun formatDateLong(date: LocalDate): String = date.format(longFormatter)
