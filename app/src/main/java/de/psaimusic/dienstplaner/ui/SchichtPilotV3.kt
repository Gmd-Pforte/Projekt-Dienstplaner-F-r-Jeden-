package de.psaimusic.dienstplaner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.psaimusic.dienstplaner.data.Absence
import de.psaimusic.dienstplaner.data.AppScreen
import de.psaimusic.dienstplaner.data.AppSettings
import de.psaimusic.dienstplaner.data.AppStore
import de.psaimusic.dienstplaner.data.Assignment
import de.psaimusic.dienstplaner.data.Employee
import de.psaimusic.dienstplaner.data.PlannerRules
import de.psaimusic.dienstplaner.data.ShiftTemplate
import de.psaimusic.dienstplaner.pdf.EmployeePdfExporter
import de.psaimusic.dienstplaner.planner.AutoPlanner
import de.psaimusic.dienstplaner.planner.PlanResult
import de.psaimusic.dienstplaner.widget.NextShiftWidget
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchichtPilotV3() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }

    var employees by remember { mutableStateOf(store.loadEmployees()) }
    var shifts by remember { mutableStateOf(store.loadShifts()) }
    var absences by remember { mutableStateOf(store.loadAbsences()) }
    var rules by remember { mutableStateOf(store.loadRules()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var plan by remember { mutableStateOf(PlanResult(store.loadAssignments(), emptyList())) }
    var planDays by rememberSaveable { mutableIntStateOf(28) }

    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var selectedEmployeeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val screen = AppScreen.valueOf(screenName)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEmployeeDialog by remember { mutableStateOf(false) }
    var showAbsenceDialog by remember { mutableStateOf(false) }

    val navigate: (AppScreen) -> Unit = {
        selectedEmployeeId = null
        screenName = it.name
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 840.dp
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                NavigationPanelV3(screen, navigate)
                MainScaffoldV3(
                    modifier = Modifier.weight(1f),
                    screen = screen,
                    showMenu = false,
                    employeeDetail = selectedEmployeeId != null,
                    onBackEmployee = { selectedEmployeeId = null },
                    onMenu = {},
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    ContentV3(
                        modifier = contentModifier,
                        tablet = true,
                        screen = screen,
                        selectedEmployeeId = selectedEmployeeId,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        planDays = planDays,
                        navigate = navigate,
                        onEmployeeSelected = { selectedEmployeeId = it },
                        onBackEmployee = { selectedEmployeeId = null },
                        onPlanDays = { planDays = it },
                        onGenerate = {
                            val start = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                            plan = AutoPlanner.generate(start, planDays, employees, shifts, absences, rules)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteEmployee = { employee ->
                            employees = employees.filterNot { it.id == employee.id }
                            absences = absences.filterNot { it.employeeId == employee.id }
                            plan = plan.copy(assignments = plan.assignments.filterNot { it.employeeId == employee.id })
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            store.saveAssignments(plan.assignments)
                            selectedEmployeeId = null
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
                        BrandHeaderV3()
                        AppScreen.entries.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = screen == item,
                                icon = { Icon(screenIconV3(item), null) },
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
                            "0.3 Test · Handy & Tablet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            ) {
                MainScaffoldV3(
                    modifier = Modifier.fillMaxSize(),
                    screen = screen,
                    showMenu = true,
                    employeeDetail = selectedEmployeeId != null,
                    onBackEmployee = { selectedEmployeeId = null },
                    onMenu = { scope.launch { drawerState.open() } },
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    ContentV3(
                        modifier = contentModifier,
                        tablet = false,
                        screen = screen,
                        selectedEmployeeId = selectedEmployeeId,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        planDays = planDays,
                        navigate = navigate,
                        onEmployeeSelected = { selectedEmployeeId = it },
                        onBackEmployee = { selectedEmployeeId = null },
                        onPlanDays = { planDays = it },
                        onGenerate = {
                            val start = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                            plan = AutoPlanner.generate(start, planDays, employees, shifts, absences, rules)
                            store.saveAssignments(plan.assignments)
                            NextShiftWidget.refresh(context)
                        },
                        onDeleteEmployee = { employee ->
                            employees = employees.filterNot { it.id == employee.id }
                            absences = absences.filterNot { it.employeeId == employee.id }
                            plan = plan.copy(assignments = plan.assignments.filterNot { it.employeeId == employee.id })
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            store.saveAssignments(plan.assignments)
                            selectedEmployeeId = null
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
        AddEmployeeDialogV3(
            onDismiss = { showEmployeeDialog = false },
            onAdd = { employee ->
                employees = employees + employee
                store.saveEmployees(employees)
                showEmployeeDialog = false
            }
        )
    }

    if (showAbsenceDialog) {
        AddAbsenceDialogV3(
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
private fun MainScaffoldV3(
    modifier: Modifier,
    screen: AppScreen,
    showMenu: Boolean,
    employeeDetail: Boolean,
    onBackEmployee: () -> Unit,
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
                        Text(if (employeeDetail) "Mitarbeiterplan" else screen.title, fontWeight = FontWeight.SemiBold)
                        Text("SchichtPilot 0.3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    when {
                        employeeDetail -> IconButton(onClick = onBackEmployee) { Icon(Icons.Default.ArrowBack, "Zurück") }
                        showMenu -> IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Menü") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!employeeDetail) {
                when (screen) {
                    AppScreen.EMPLOYEES -> FloatingActionButton(onClick = onAddEmployee) { Icon(Icons.Default.Add, "Mitarbeiter hinzufügen") }
                    AppScreen.ABSENCES -> FloatingActionButton(onClick = onAddAbsence) { Icon(Icons.Default.Add, "Abwesenheit hinzufügen") }
                    else -> Unit
                }
            }
        }
    ) { padding -> content(Modifier.fillMaxSize().padding(padding)) }
}

@Composable
private fun NavigationPanelV3(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    Surface(Modifier.width(250.dp).fillMaxSize(), tonalElevation = 3.dp) {
        LazyColumn(Modifier.padding(vertical = 18.dp)) {
            item { BrandHeaderV3() }
            items(AppScreen.entries) { item ->
                NavigationDrawerItem(
                    label = { Text(item.title) },
                    selected = screen == item,
                    icon = { Icon(screenIconV3(item), null) },
                    onClick = { navigate(item) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Text("Tablet-Modus · 0.3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun BrandHeaderV3() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text("SchichtPilot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Einfach. Planen. Zusammen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ContentV3(
    modifier: Modifier,
    tablet: Boolean,
    screen: AppScreen,
    selectedEmployeeId: Long?,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    rules: PlannerRules,
    settings: AppSettings,
    plan: PlanResult,
    planDays: Int,
    navigate: (AppScreen) -> Unit,
    onEmployeeSelected: (Long) -> Unit,
    onBackEmployee: () -> Unit,
    onPlanDays: (Int) -> Unit,
    onGenerate: () -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onShiftChange: (ShiftTemplate) -> Unit,
    onDeleteAbsence: (Absence) -> Unit,
    onRulesChange: (PlannerRules) -> Unit,
    onSettingsChange: (AppSettings) -> Unit
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth().widthIn(max = if (tablet) 1220.dp else 720.dp)) {
            if (screen == AppScreen.EMPLOYEES && selectedEmployeeId != null) {
                val employee = employees.firstOrNull { it.id == selectedEmployeeId }
                if (employee != null) {
                    EmployeeDetailV3(employee, plan.assignments, shifts, absences, tablet, onBackEmployee)
                } else {
                    onBackEmployee()
                }
                return@Box
            }

            when (screen) {
                AppScreen.HOME -> HomeV3(tablet, employees, shifts, absences, plan, settings, navigate)
                AppScreen.SCHEDULE -> TeamCalendarV3(tablet, plan.assignments, employees, shifts, absences)
                AppScreen.AUTOPLAN -> AutoPlanV3(employees, shifts, rules, plan, planDays, onPlanDays, onGenerate)
                AppScreen.EMPLOYEES -> EmployeesV3(employees, plan.assignments, onEmployeeSelected, onDeleteEmployee)
                AppScreen.SHIFTS -> ShiftsV3(shifts, onShiftChange)
                AppScreen.ABSENCES -> AbsencesV3(absences, employees, settings.friendlyHints, onDeleteAbsence)
                AppScreen.RULES -> RulesV3(rules, onRulesChange)
                AppScreen.STATS -> StatsV3(tablet, plan, employees)
                AppScreen.SETTINGS -> SettingsV3(settings, onSettingsChange)
            }
        }
    }
}

@Composable
private fun HomeV3(
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
    val vacation = absences.filter { it.type.equals("Urlaub", true) && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
    val days = vacation?.let { ChronoUnit.DAYS.between(LocalDate.now(), it.start) }

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
                    Text(if (next == null) "Erstelle deinen ersten Plan." else "${dateLabel(next.date)} · ${nextEmployee?.name ?: "Mitarbeiter"}")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("🏖️ Nächster Urlaub", fontWeight = FontWeight.Bold)
                    Text(if (days == null) "Noch keiner eingetragen" else "Noch $days Tage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(if (vacation == null) "Über Frei & Urlaub direkt im Kalender eintragen." else "Start: ${dateLabel(vacation.start)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            if (tablet) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeAction("📅 Dienstplan", "Monatskalender", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                    HomeAction("✨ Automatik", "Plan erstellen", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
                    HomeAction("👥 Team", "${employees.size} Mitarbeiter", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeAction("📅 Kalender", "Dienstplan", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                    HomeAction("✨ Planen", "Automatik", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeAction("👥 Team", "${employees.size} Personen", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
                    HomeAction("🏖️ Urlaub", "${absences.size} Einträge", Modifier.weight(1f)) { navigate(AppScreen.ABSENCES) }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun HomeAction(title: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TeamCalendarV3(
    tablet: Boolean,
    assignments: List<Assignment>,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>
) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthText)
    val selectedDate = LocalDate.parse(selectedDateText)

    val calendar: @Composable () -> Unit = {
        Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            MonthCalendar(
                month = month,
                assignments = assignments,
                shifts = shifts,
                employees = employees,
                absences = absences,
                selectedDate = selectedDate,
                onSelectedDate = { selectedDateText = it.toString() },
                onPreviousMonth = { monthText = month.minusMonths(1).toString() },
                onNextMonth = { monthText = month.plusMonths(1).toString() },
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    val details: @Composable () -> Unit = {
        DayDetailV3(selectedDate, assignments, employees, shifts, absences)
    }

    if (tablet) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1.8f)) { calendar() }
            Box(Modifier.weight(1f)) { details() }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { calendar() }
            item { details() }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun DayDetailV3(
    date: LocalDate,
    assignments: List<Assignment>,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>
) {
    val dayAssignments = assignments.filter { it.date == date }
    val dayAbsences = absences.filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM", Locale.GERMAN)).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (dayAssignments.isEmpty() && dayAbsences.isEmpty()) {
                Text("An diesem Tag ist noch nichts eingetragen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            dayAssignments.forEach { assignment ->
                val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                val employee = employees.firstOrNull { it.id == assignment.employeeId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(shiftColorV3(assignment.shiftId), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("${shift?.code ?: "?"} · ${employee?.name ?: "Unbekannt"}", fontWeight = FontWeight.SemiBold)
                        Text("${shift?.start ?: "--"} – ${shift?.end ?: "--"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            dayAbsences.forEach { absence ->
                val employee = employees.firstOrNull { it.id == absence.employeeId }
                Text("${absenceCode(absence.type)} · ${employee?.name ?: "Mitarbeiter"} · ${absence.type}", color = absenceColor(absence.type), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmployeesV3(
    employees: List<Employee>,
    assignments: List<Assignment>,
    onSelect: (Long) -> Unit,
    onDelete: (Employee) -> Unit
) {
    if (employees.isEmpty()) {
        CenterMessageV3("Noch niemand im Team", "Tippe auf + und lege den ersten Mitarbeiter an.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(employees, key = { it.id }) { employee ->
            val monthCount = assignments.count { it.employeeId == employee.id && YearMonth.from(it.date) == YearMonth.now() }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(employee.id) },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Text(employee.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(employee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$monthCount Dienste diesen Monat · max. ${employee.maxDaysPerWeek} Tage/Woche", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { onDelete(employee) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun EmployeeDetailV3(
    employee: Employee,
    assignments: List<Assignment>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    tablet: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var monthText by rememberSaveable(employee.id) { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateText by rememberSaveable(employee.id) { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthText)
    val selectedDate = LocalDate.parse(selectedDateText)
    val employeeAssignments = assignments.filter { it.employeeId == employee.id }
    val employeeAbsences = absences.filter { it.employeeId == employee.id }
    val monthAssignments = employeeAssignments.filter { YearMonth.from(it.date) == month }
    val monthNights = monthAssignments.count { it.shiftId == "night" }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                            Text(employee.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(employee.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Max. ${employee.maxDaysPerWeek} Tage/Woche", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPillV3("Dienste", monthAssignments.size.toString(), Modifier.weight(1f))
                        StatPillV3("Nächte", monthNights.toString(), Modifier.weight(1f))
                        StatPillV3("Abwesenheit", employeeAbsences.size.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { EmployeePdfExporter.printMonthlyPlan(context, employee, month, employeeAssignments, shifts, employeeAbsences) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, null)
                        Spacer(Modifier.width(8.dp))
                        Text("DIENSTPLAN ALS PDF")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                MonthCalendar(
                    month = month,
                    assignments = employeeAssignments,
                    shifts = shifts,
                    employees = listOf(employee),
                    absences = employeeAbsences,
                    selectedDate = selectedDate,
                    onSelectedDate = { selectedDateText = it.toString() },
                    onPreviousMonth = { monthText = month.minusMonths(1).toString() },
                    onNextMonth = { monthText = month.plusMonths(1).toString() },
                    employeeId = employee.id,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        item {
            val assignment = employeeAssignments.firstOrNull { it.date == selectedDate }
            val absence = employeeAbsences.firstOrNull { !selectedDate.isBefore(it.start) && !selectedDate.isAfter(it.end) }
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(dateLabelLong(selectedDate), fontWeight = FontWeight.Bold)
                    when {
                        absence != null -> Text("${absence.type} (${absenceCode(absence.type)})", color = absenceColor(absence.type), style = MaterialTheme.typography.titleMedium)
                        assignment != null -> {
                            val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                            Text("${shift?.name ?: "Schicht"} · ${shift?.start ?: "--"}–${shift?.end ?: "--"}", color = shiftColorV3(assignment.shiftId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        else -> Text("Frei / noch nicht geplant", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun StatPillV3(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AutoPlanV3(
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Plan-Assistent", fontWeight = FontWeight.Bold)
                    Text("Regeln, Urlaub und Schichtverbote werden automatisch berücksichtigt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = employees.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("DIENSTPLAN ERSTELLEN", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text("${employees.size} Mitarbeiter · ${shifts.count { it.enabled }} aktive Schichten · max. ${rules.maxConsecutiveDays} Tage am Stück", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (plan.assignments.isNotEmpty()) {
            item { SimpleInfoV3("✅ ${plan.assignments.size} Dienste im aktuellen Plan gespeichert") }
        }
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
    }
}

@Composable
private fun ShiftsV3(shifts: List<ShiftTemplate>, onUpdate: (ShiftTemplate) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(shifts, key = { it.id }) { shift ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(shiftColorV3(shift.id), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(shift.code, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shift.name, fontWeight = FontWeight.Bold)
                            Text("${shift.start} – ${shift.end} · ${shift.breakMinutes} Min. Pause", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = shift.enabled, onCheckedChange = { onUpdate(shift.copy(enabled = it)) })
                    }
                    NumberControlV3("Mindestbesetzung", shift.requiredEmployees, 1, 10) { onUpdate(shift.copy(requiredEmployees = it)) }
                    NumberControlV3("Pause (Min.)", shift.breakMinutes, 0, 120, 15) { onUpdate(shift.copy(breakMinutes = it)) }
                }
            }
        }
    }
}

@Composable
private fun AbsencesV3(absences: List<Absence>, employees: List<Employee>, friendly: Boolean, onDelete: (Absence) -> Unit) {
    if (absences.isEmpty()) {
        CenterMessageV3("Noch kein Urlaub eingetragen", "Tippe auf +. Start und Ende wählst du jetzt direkt im Kalender.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (friendly) {
            val next = absences.filter { it.type.equals("Urlaub", true) && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
            if (next != null) {
                item {
                    val remaining = ChronoUnit.DAYS.between(LocalDate.now(), next.start)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("🏖️ Vorfreude läuft", fontWeight = FontWeight.Bold)
                            Text("Noch $remaining Tage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Dann heißt es: Dienstplan zu, Urlaub an 😎", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        items(absences.sortedBy { it.start }, key = { it.id }) { absence ->
            val employee = employees.firstOrNull { it.id == absence.employeeId }
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(absenceColor(absence.type).copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(absenceCode(absence.type), color = absenceColor(absence.type), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${absence.type} · ${employee?.name ?: "Mitarbeiter"}", fontWeight = FontWeight.Bold)
                        Text("${dateLabel(absence.start)} – ${dateLabel(absence.end)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onDelete(absence) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RulesV3(rules: PlannerRules, onChange: (PlannerRules) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NumberControlCardV3("Max. Arbeitstage am Stück", rules.maxConsecutiveDays, 1, 7) { onChange(rules.copy(maxConsecutiveDays = it)) } }
        item { NumberControlCardV3("Mindestruhezeit (Stunden)", rules.minRestHours, 8, 16) { onChange(rules.copy(minRestHours = it)) } }
        item { ToggleCardV3("Schnelle Wechsel vermeiden", "Keine problematischen Nacht/Spät → Früh-Folgen.", rules.avoidFastRotation) { onChange(rules.copy(avoidFastRotation = it)) } }
        item { ToggleCardV3("Wochenenden fair verteilen", "Wochenenddienste möglichst gleichmäßig verteilen.", rules.distributeWeekendsFairly) { onChange(rules.copy(distributeWeekendsFairly = it)) } }
    }
}

@Composable
private fun StatsV3(tablet: Boolean, plan: PlanResult, employees: List<Employee>) {
    val nights = plan.assignments.count { it.shiftId == "night" }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (tablet) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricV3("Mitarbeiter", employees.size.toString(), Modifier.weight(1f))
                    MetricV3("Dienste", plan.assignments.size.toString(), Modifier.weight(1f))
                    MetricV3("Nachtdienste", nights.toString(), Modifier.weight(1f))
                    MetricV3("Konflikte", plan.warnings.size.toString(), Modifier.weight(1f))
                }
            } else {
                MetricV3("Mitarbeiter", employees.size.toString(), Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                MetricV3("Geplante Dienste", plan.assignments.size.toString(), Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                MetricV3("Nachtdienste", nights.toString(), Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                MetricV3("Konflikte", plan.warnings.size.toString(), Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SettingsV3(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val professions = listOf("Sicherheitsdienst", "Pflege", "Gastronomie", "Logistik", "Produktion", "Einzelhandel", "Reinigung", "Sonstige")
    val states = listOf("Nordrhein-Westfalen", "Bayern", "Baden-Württemberg", "Hessen", "Niedersachsen", "Berlin", "Hamburg")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Berufsgruppe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(professions) { profession ->
            FilterChip(selected = settings.profession == profession, onClick = { onChange(settings.copy(profession = profession)) }, label = { Text(profession) })
        }
        item { Spacer(Modifier.height(8.dp)); Text("Bundesland", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(states) { state ->
            FilterChip(selected = settings.federalState == state, onClick = { onChange(settings.copy(federalState = state)) }, label = { Text(state) })
        }
        item { ToggleCardV3("Freundliche Hinweise", "Urlaubs-Countdown und kleine motivierende Texte anzeigen.", settings.friendlyHints) { onChange(settings.copy(friendlyHints = it)) } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAbsenceDialogV3(employees: List<Employee>, onDismiss: () -> Unit, onAdd: (Absence) -> Unit) {
    var selectedId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var type by remember { mutableStateOf("Urlaub") }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf(LocalDate.now()) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frei / Urlaub eintragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (employees.isEmpty()) {
                    Text("Lege zuerst einen Mitarbeiter an.")
                } else {
                    Text("Mitarbeiter", fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.heightIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(employees, key = { it.id }) { employee ->
                            FilterChip(
                                selected = selectedId == employee.id,
                                onClick = { selectedId = employee.id },
                                label = { Text(employee.name) }
                            )
                        }
                    }
                    Text("Art", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Urlaub", "Frei", "Wunschfrei").forEach { option ->
                            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Krank", "Sonstiges").forEach { option ->
                            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
                        }
                    }
                    Text("Zeitraum", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
                            Column { Text("Von", style = MaterialTheme.typography.labelSmall); Text(dateLabel(start), fontWeight = FontWeight.Bold) }
                        }
                        OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.weight(1f)) {
                            Column { Text("Bis", style = MaterialTheme.typography.labelSmall); Text(dateLabel(end), fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text("Die Tage werden direkt im Kalender gewählt – kein Datum mehr abtippen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedId != null && employees.isNotEmpty(),
                onClick = {
                    selectedId?.let { id -> onAdd(Absence(employeeId = id, type = type, start = start, end = end)) }
                }
            ) { Text("Eintragen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )

    if (pickStart) {
        DatePickerPopupV3(start, onDismiss = { pickStart = false }) { chosen ->
            start = chosen
            if (end.isBefore(start)) end = start
            pickStart = false
        }
    }
    if (pickEnd) {
        DatePickerPopupV3(end, onDismiss = { pickEnd = false }) { chosen ->
            end = if (chosen.isBefore(start)) start else chosen
            pickEnd = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopupV3(initial: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    ) { DatePicker(state = state) }
}

@Composable
private fun AddEmployeeDialogV3(onDismiss: () -> Unit, onAdd: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(5) }
    var night by remember { mutableStateOf(true) }
    var rotation by remember { mutableStateOf(true) }
    var weekend by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitarbeiter anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                NumberControlV3("Max. Tage pro Woche", days, 1, 7) { days = it }
                ToggleRowV3("Nachtschicht erlaubt", night) { night = it }
                ToggleRowV3("Wechselschichten erlaubt", rotation) { rotation = it }
                ToggleRowV3("Wochenende erlaubt", weekend) { weekend = it }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onAdd(Employee(name = name.trim(), maxDaysPerWeek = days, allowNight = night, allowShiftChange = rotation, allowWeekend = weekend)) }) {
                Text("Anlegen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun NumberControlV3(label: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
    }
}

@Composable
private fun NumberControlCardV3(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { NumberControlV3(label, value, min, max, onChange = onChange) }
    }
}

@Composable
private fun ToggleRowV3(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ToggleCardV3(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun MetricV3(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SimpleInfoV3(text: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun CenterMessageV3(title: String, detail: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun screenIconV3(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.HOME -> Icons.Default.Home
    AppScreen.SCHEDULE -> Icons.Default.CalendarMonth
    AppScreen.AUTOPLAN -> Icons.Default.PlayArrow
    AppScreen.EMPLOYEES -> Icons.Default.People
    AppScreen.SHIFTS -> Icons.Default.Schedule
    AppScreen.ABSENCES -> Icons.Default.BeachAccess
    AppScreen.RULES -> Icons.Default.Tune
    AppScreen.STATS -> Icons.Default.BarChart
    AppScreen.SETTINGS -> Icons.Default.Settings
}

private fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun dateLabelLong(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)).replaceFirstChar { it.uppercase() }
