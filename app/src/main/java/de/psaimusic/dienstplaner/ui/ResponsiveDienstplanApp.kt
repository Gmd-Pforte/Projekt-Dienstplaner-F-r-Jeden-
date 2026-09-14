package de.psaimusic.dienstplaner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import de.psaimusic.dienstplaner.widget.NextShiftWidget
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveDienstplanApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }

    var employees by remember { mutableStateOf(store.loadEmployees()) }
    var shifts by remember { mutableStateOf(store.loadShifts()) }
    var absences by remember { mutableStateOf(store.loadAbsences()) }
    var rules by remember { mutableStateOf(store.loadRules()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var plan by remember { mutableStateOf(PlanResult(store.loadAssignments(), emptyList())) }
    var planDays by rememberSaveable { mutableIntStateOf(14) }

    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    val screen = AppScreen.valueOf(screenName)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEmployeeDialog by remember { mutableStateOf(false) }
    var showAbsenceDialog by remember { mutableStateOf(false) }

    val navigate: (AppScreen) -> Unit = { screenName = it.name }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 840.dp

        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                TabletSidebar(screen = screen, navigate = navigate)
                AppScaffold(
                    modifier = Modifier.weight(1f),
                    screen = screen,
                    showMenu = false,
                    onMenu = {},
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    AppContent(
                        modifier = contentModifier,
                        tablet = true,
                        screen = screen,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        planDays = planDays,
                        navigate = navigate,
                        onPlanDays = { planDays = it },
                        onGenerate = {
                            val monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                            plan = AutoPlanner.generate(monday, planDays, employees, shifts, absences, rules)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteEmployee = { target ->
                            employees = employees.filterNot { it.id == target.id }
                            absences = absences.filterNot { it.employeeId == target.id }
                            plan = plan.copy(assignments = plan.assignments.filterNot { it.employeeId == target.id })
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onShiftChange = { updated ->
                            shifts = shifts.map { if (it.id == updated.id) updated else it }
                            store.saveShifts(shifts)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteAbsence = { target ->
                            absences = absences.filterNot { it.id == target.id }
                            store.saveAbsences(absences)
                        },
                        onRulesChange = { rules = it; store.saveRules(it) },
                        onSettingsChange = { settings = it; store.saveSettings(it) }
                    )
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerHeader()
                        AppScreen.entries.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = screen == item,
                                icon = { Icon(screenIconV2(item), null) },
                                onClick = {
                                    navigate(item)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Text(
                            "0.2 Test · Offlinefähig",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            ) {
                AppScaffold(
                    modifier = Modifier.fillMaxSize(),
                    screen = screen,
                    showMenu = true,
                    onMenu = { scope.launch { drawerState.open() } },
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    AppContent(
                        modifier = contentModifier,
                        tablet = false,
                        screen = screen,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        planDays = planDays,
                        navigate = navigate,
                        onPlanDays = { planDays = it },
                        onGenerate = {
                            val monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                            plan = AutoPlanner.generate(monday, planDays, employees, shifts, absences, rules)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteEmployee = { target ->
                            employees = employees.filterNot { it.id == target.id }
                            absences = absences.filterNot { it.employeeId == target.id }
                            plan = plan.copy(assignments = plan.assignments.filterNot { it.employeeId == target.id })
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onShiftChange = { updated ->
                            shifts = shifts.map { if (it.id == updated.id) updated else it }
                            store.saveShifts(shifts)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteAbsence = { target ->
                            absences = absences.filterNot { it.id == target.id }
                            store.saveAbsences(absences)
                        },
                        onRulesChange = { rules = it; store.saveRules(it) },
                        onSettingsChange = { settings = it; store.saveSettings(it) }
                    )
                }
            }
        }
    }

    if (showEmployeeDialog) {
        AddEmployeeDialogV2(
            onDismiss = { showEmployeeDialog = false },
            onAdd = { employee ->
                employees = employees + employee
                store.saveEmployees(employees)
                showEmployeeDialog = false
            }
        )
    }

    if (showAbsenceDialog) {
        AddAbsenceDialogV2(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    modifier: Modifier,
    screen: AppScreen,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onAddEmployee: () -> Unit,
    onAddAbsence: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screen.title, fontWeight = FontWeight.SemiBold)
                        Text("Dienstplaner für Jeden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (showMenu) {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Menü") }
                    }
                }
            )
        },
        floatingActionButton = {
            when (screen) {
                AppScreen.EMPLOYEES -> FloatingActionButton(onClick = onAddEmployee) { Icon(Icons.Default.Add, "Mitarbeiter hinzufügen") }
                AppScreen.ABSENCES -> FloatingActionButton(onClick = onAddAbsence) { Icon(Icons.Default.Add, "Abwesenheit hinzufügen") }
                else -> Unit
            }
        }
    ) { padding ->
        content(Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun TabletSidebar(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    Surface(Modifier.width(250.dp).fillMaxSize(), tonalElevation = 3.dp) {
        LazyColumn(Modifier.padding(vertical = 18.dp)) {
            item { DrawerHeader() }
            items(AppScreen.entries) { item ->
                NavigationDrawerItem(
                    label = { Text(item.title) },
                    selected = screen == item,
                    icon = { Icon(screenIconV2(item), null) },
                    onClick = { navigate(item) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Text("Tablet-Modus · 0.2 Test", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text("SchichtPilot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Einfach. Planen. Zusammen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun AppContent(
    modifier: Modifier,
    tablet: Boolean,
    screen: AppScreen,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    rules: PlannerRules,
    settings: AppSettings,
    plan: PlanResult,
    planDays: Int,
    navigate: (AppScreen) -> Unit,
    onPlanDays: (Int) -> Unit,
    onGenerate: () -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onShiftChange: (ShiftTemplate) -> Unit,
    onDeleteAbsence: (Absence) -> Unit,
    onRulesChange: (PlannerRules) -> Unit,
    onSettingsChange: (AppSettings) -> Unit
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth().widthIn(max = if (tablet) 1180.dp else 720.dp)) {
            when (screen) {
                AppScreen.HOME -> HomeV2(tablet, employees, shifts, absences, plan, settings, navigate)
                AppScreen.SCHEDULE -> ScheduleV2(plan.assignments, employees, shifts)
                AppScreen.AUTOPLAN -> AutoPlanV2(employees, shifts, rules, plan, planDays, onPlanDays, onGenerate)
                AppScreen.EMPLOYEES -> EmployeesV2(employees, onDeleteEmployee)
                AppScreen.SHIFTS -> ShiftsV2(shifts, onShiftChange)
                AppScreen.ABSENCES -> AbsencesV2(absences, employees, settings.friendlyHints, onDeleteAbsence)
                AppScreen.RULES -> RulesV2(rules, onRulesChange)
                AppScreen.STATS -> StatsV2(tablet, plan, employees)
                AppScreen.SETTINGS -> SettingsV2(settings, onSettingsChange)
            }
        }
    }
}

@Composable
private fun HomeV2(
    tablet: Boolean,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    plan: PlanResult,
    settings: AppSettings,
    navigate: (AppScreen) -> Unit
) {
    val next = plan.assignments.filter { !it.date.isBefore(LocalDate.now()) }.minByOrNull { it.date }
    val nextShift = shifts.firstOrNull { it.id == next?.shiftId }
    val nextEmployee = employees.firstOrNull { it.id == next?.employeeId }
    val vacation = absences.filter { it.type == "Urlaub" && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
    val countdown = vacation?.let { vacationCountdown(it.start) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Hallo 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${settings.profession} · ${settings.federalState}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nächster geplanter Dienst", fontWeight = FontWeight.SemiBold)
                    Text(if (nextShift == null) "Noch kein Plan" else "${nextShift.name} · ${nextShift.start}–${nextShift.end}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (next == null) "Erstelle deinen ersten Plan." else "${formatDateV2(next.date)} · ${nextEmployee?.name ?: "Mitarbeiter"}")
                }
            }
        }
        item {
            if (tablet) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeQuick("✨ Automatisch planen", "Regeln anwenden", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
                    HomeQuick("👥 Mitarbeiter", "${employees.size} Personen", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
                    HomeQuick("📅 Dienstplan", "${plan.assignments.size} Dienste", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeQuick("✨ Planen", "Automatik", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
                    HomeQuick("👥 Team", "${employees.size} Personen", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
                }
            }
        }
        if (!tablet) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeQuick("📅 Plan", "${plan.assignments.size} Dienste", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                    HomeQuick("🏖️ Frei", "${absences.size} Einträge", Modifier.weight(1f)) { navigate(AppScreen.ABSENCES) }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("🏖️ Urlaub", fontWeight = FontWeight.Bold)
                    Text(countdown ?: "Noch kein zukünftiger Urlaub eingetragen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (vacation != null) Text("Start: ${formatDateV2(vacation.start)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (tablet) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeQuick("🏖️ Urlaub & Frei", "${absences.size} Einträge", Modifier.weight(1f)) { navigate(AppScreen.ABSENCES) }
                    HomeQuick("⚙️ Regeln", "${rulesSummary(plan)}", Modifier.weight(1f)) { navigate(AppScreen.RULES) }
                    HomeQuick("📊 Auswertung", "${plan.warnings.size} Konflikte", Modifier.weight(1f)) { navigate(AppScreen.STATS) }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun HomeQuick(title: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScheduleV2(assignments: List<Assignment>, employees: List<Employee>, shifts: List<ShiftTemplate>) {
    if (assignments.isEmpty()) { EmptyV2("Noch kein Dienstplan", "Öffne Automatisch planen und erstelle einen Vorschlag."); return }
    val grouped = assignments.groupBy { it.date }.toSortedMap()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { (date, day) ->
            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatDateLongV2(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        day.sortedBy { it.shiftId }.forEach { assignment ->
                            val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                            val employee = employees.firstOrNull { it.id == assignment.employeeId }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).background(shiftColorV2(assignment.shiftId), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(shift?.code ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(employee?.name ?: "Unbekannt", fontWeight = FontWeight.SemiBold)
                                    Text("${shift?.start ?: "--"} – ${shift?.end ?: "--"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AutoPlanV2(
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    rules: PlannerRules,
    plan: PlanResult,
    planDays: Int,
    onPlanDays: (Int) -> Unit,
    onGenerate: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Plan-Assistent", fontWeight = FontWeight.Bold)
                    Text("Die App verteilt Dienste nach Mitarbeiterregeln, Abwesenheiten, Ruhezeit und fairer Wochenendlast.")
                }
            }
        }
        item {
            Text("Planungszeitraum", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 28).forEach { days ->
                    FilterChip(selected = planDays == days, onClick = { onPlanDays(days) }, label = { Text("$days Tage") })
                }
            }
        }
        item {
            Text("${employees.size} Mitarbeiter · ${shifts.count { it.enabled }} aktive Schichten · ${rules.minRestHours} h Ruhezeit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Button(onClick = onGenerate, enabled = employees.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("DIENSTPLAN ERSTELLEN", fontWeight = FontWeight.Bold)
            }
        }
        if (plan.assignments.isNotEmpty()) item { Text("${plan.assignments.size} Dienste verteilt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (plan.warnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null)
                            Spacer(Modifier.width(8.dp))
                            Text("${plan.warnings.size} Konflikte", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        plan.warnings.take(10).forEach { Text("• $it") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun EmployeesV2(employees: List<Employee>, onDelete: (Employee) -> Unit) {
    if (employees.isEmpty()) { EmptyV2("Noch niemand im Team", "Tippe auf + und lege den ersten Mitarbeiter an."); return }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(employees, key = { it.id }) { employee ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(employee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Max. ${employee.maxDaysPerWeek} Tage/Woche")
                        Text(
                            listOfNotNull(
                                if (!employee.allowNight) "keine Nacht" else null,
                                if (!employee.allowShiftChange) "keine Wechselschicht" else null,
                                if (!employee.allowWeekend) "kein Wochenende" else null
                            ).ifEmpty { listOf("alle Standarddienste erlaubt") }.joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { onDelete(employee) }) { Text("Entfernen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ShiftsV2(shifts: List<ShiftTemplate>, onChange: (ShiftTemplate) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Schichten & Pausen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Aktiviere Schichten und passe Besetzung sowie Pausen an.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(shifts, key = { it.id }) { shift ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(shiftColorV2(shift.id), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(shift.code, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shift.name, fontWeight = FontWeight.Bold)
                            Text("${shift.start} – ${shift.end}")
                        }
                        Switch(checked = shift.enabled, onCheckedChange = { onChange(shift.copy(enabled = it)) })
                    }
                    StepperV2("Mindestbesetzung", shift.requiredEmployees, 1, 10) { onChange(shift.copy(requiredEmployees = it)) }
                    StepperV2("Pause", shift.breakMinutes, 0, 120, 15, " Min.") { onChange(shift.copy(breakMinutes = it)) }
                }
            }
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AbsencesV2(absences: List<Absence>, employees: List<Employee>, friendly: Boolean, onDelete: (Absence) -> Unit) {
    val vacation = absences.filter { it.type == "Urlaub" && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("🏖️ Dein nächster Urlaub", fontWeight = FontWeight.Bold)
                    Text(vacation?.let { vacationCountdown(it.start) } ?: "Noch nichts eingetragen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (friendly && vacation != null) Text("Du hast es fast geschafft 😎")
                }
            }
        }
        if (absences.isEmpty()) item { Text("Tippe auf + und trage Urlaub, Freiwunsch oder Abwesenheit ein.") }
        items(absences.sortedBy { it.start }, key = { it.id }) { absence ->
            val name = employees.firstOrNull { it.id == absence.employeeId }?.name ?: "Mitarbeiter"
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (absence.type == "Urlaub") "🏖️" else "📌", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${absence.type} · $name", fontWeight = FontWeight.Bold)
                        Text("${formatDateV2(absence.start)} – ${formatDateV2(absence.end)}")
                    }
                    TextButton(onClick = { onDelete(absence) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RulesV2(rules: PlannerRules, onChange: (PlannerRules) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepperV2("Max. Arbeitstage am Stück", rules.maxConsecutiveDays, 1, 7) { onChange(rules.copy(maxConsecutiveDays = it)) } }
        item { StepperV2("Mindestruhezeit", rules.minRestHours, 8, 16, 1, " h") { onChange(rules.copy(minRestHours = it)) } }
        item { ToggleV2("Schnelle Wechsel vermeiden", "Verhindert z. B. Nacht → Früh und Spät → Früh.", rules.avoidFastRotation) { onChange(rules.copy(avoidFastRotation = it)) } }
        item { ToggleV2("Wochenenden fair verteilen", "Bevorzugt Mitarbeiter mit weniger bisherigen Wochenenddiensten.", rules.distributeWeekendsFairly) { onChange(rules.copy(distributeWeekendsFairly = it)) } }
    }
}

@Composable
private fun StatsV2(tablet: Boolean, plan: PlanResult, employees: List<Employee>) {
    val nights = plan.assignments.count { it.shiftId == "night" }
    val cards = listOf(
        Triple("Mitarbeiter", employees.size.toString(), "im Team"),
        Triple("Geplante Dienste", plan.assignments.size.toString(), "im gespeicherten Plan"),
        Triple("Nachtdienste", nights.toString(), "aktuell verteilt"),
        Triple("Konflikte", plan.warnings.size.toString(), if (plan.warnings.isEmpty()) "sieht gut aus 👍" else "bitte prüfen")
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tablet) {
            items(cards.chunked(2)) { rowCards ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowCards.forEach { item -> MetricV2(item.first, item.second, item.third, Modifier.weight(1f)) }
                    if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(cards) { item -> MetricV2(item.first, item.second, item.third, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun SettingsV2(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val professions = listOf("Sicherheitsdienst", "Pflege", "Gastronomie", "Hotel", "Logistik", "Produktion", "Einzelhandel", "Reinigung", "Rettungsdienst", "Tankstelle", "Sonstige")
    val states = listOf("Baden-Württemberg", "Bayern", "Berlin", "Brandenburg", "Bremen", "Hamburg", "Hessen", "Mecklenburg-Vorpommern", "Niedersachsen", "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland", "Sachsen", "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Berufsgruppe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(professions) { profession -> FilterChip(selected = settings.profession == profession, onClick = { onChange(settings.copy(profession = profession)) }, label = { Text(profession) }) }
        item { Spacer(Modifier.height(6.dp)); Text("Bundesland", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(states) { state -> FilterChip(selected = settings.federalState == state, onClick = { onChange(settings.copy(federalState = state)) }, label = { Text(state) }) }
        item { ToggleV2("Freundliche Hinweise", "Urlaubs-Countdown und kleine motivierende Texte anzeigen.", settings.friendlyHints) { onChange(settings.copy(friendlyHints = it)) } }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AddEmployeeDialogV2(onDismiss: () -> Unit, onAdd: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var maxDays by remember { mutableIntStateOf(5) }
    var allowNight by remember { mutableStateOf(true) }
    var allowShiftChange by remember { mutableStateOf(true) }
    var allowWeekend by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitarbeiter anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                StepperV2("Max. Tage/Woche", maxDays, 1, 7) { maxDays = it }
                ToggleV2("Nachtschicht erlaubt", null, allowNight) { allowNight = it }
                ToggleV2("Wechselschicht erlaubt", null, allowShiftChange) { allowShiftChange = it }
                ToggleV2("Wochenende erlaubt", null, allowWeekend) { allowWeekend = it }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(Employee(name = name.trim(), maxDaysPerWeek = maxDays, allowNight = allowNight, allowShiftChange = allowShiftChange, allowWeekend = allowWeekend)) }, enabled = name.isNotBlank()) { Text("Anlegen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun AddAbsenceDialogV2(employees: List<Employee>, onDismiss: () -> Unit, onAdd: (Absence) -> Unit) {
    var selectedId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var type by remember { mutableStateOf("Urlaub") }
    var start by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    val parsedStart = runCatching { LocalDate.parse(start) }.getOrNull()
    val parsedEnd = runCatching { LocalDate.parse(end) }.getOrNull()
    val valid = selectedId != null && parsedStart != null && parsedEnd != null && !parsedEnd.isBefore(parsedStart)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Urlaub / Frei eintragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (employees.isEmpty()) {
                    Text("Lege zuerst einen Mitarbeiter an.")
                } else {
                    Text("Mitarbeiter", fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.heightIn(max = 150.dp)) {
                        items(employees) { employee ->
                            FilterChip(selected = selectedId == employee.id, onClick = { selectedId = employee.id }, label = { Text(employee.name) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Urlaub", "Freiwunsch", "Abwesend").forEach { value ->
                            FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) })
                        }
                    }
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Von (JJJJ-MM-TT)") }, singleLine = true)
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("Bis (JJJJ-MM-TT)") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(Absence(employeeId = selectedId!!, type = type, start = parsedStart!!, end = parsedEnd!!)) }, enabled = valid) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun StepperV2(label: String, value: Int, min: Int, max: Int, step: Int = 1, suffix: String = "", onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
            Text("$value$suffix", modifier = Modifier.width(76.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
        }
    }
}

@Composable
private fun ToggleV2(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (detail != null) Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun MetricV2(title: String, value: String, detail: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyV2(title: String, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp).widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun screenIconV2(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.HOME -> Icons.Default.Home
    AppScreen.SCHEDULE -> Icons.Default.DateRange
    AppScreen.AUTOPLAN -> Icons.Default.PlayArrow
    AppScreen.EMPLOYEES -> Icons.Default.People
    AppScreen.SHIFTS -> Icons.Default.Schedule
    AppScreen.ABSENCES -> Icons.Default.BeachAccess
    AppScreen.RULES -> Icons.Default.Tune
    AppScreen.STATS -> Icons.Default.BarChart
    AppScreen.SETTINGS -> Icons.Default.Settings
}

private fun shiftColorV2(id: String): Color = when (id) {
    "early" -> Color(0xFF3B82F6)
    "late" -> Color(0xFFF59E0B)
    "night" -> Color(0xFF7C3AED)
    else -> Color(0xFF64748B)
}

private fun formatDateV2(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun formatDateLongV2(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy"))

private fun vacationCountdown(start: LocalDate): String {
    val minutes = Duration.between(LocalDateTime.now(), start.atStartOfDay()).toMinutes().coerceAtLeast(0)
    val days = minutes / (24 * 60)
    val hours = (minutes % (24 * 60)) / 60
    val mins = minutes % 60
    return "Noch $days Tage · $hours Std. · $mins Min."
}

private fun rulesSummary(plan: PlanResult): String = if (plan.warnings.isEmpty()) "keine offenen Konflikte" else "${plan.warnings.size} Konflikte"
