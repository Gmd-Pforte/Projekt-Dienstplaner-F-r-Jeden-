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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditCalendar
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchichtPilotV4() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }

    var employees by remember { mutableStateOf(store.loadEmployees()) }
    var shifts by remember { mutableStateOf(store.loadShifts()) }
    var absences by remember { mutableStateOf(store.loadAbsences()) }
    var rules by remember { mutableStateOf(store.loadRules()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var plan by remember { mutableStateOf(PlanResult(store.loadAssignments(), emptyList())) }

    var selectedMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = YearMonth.parse(selectedMonthText)
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = LocalDate.parse(selectedDateText)

    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var selectedEmployeeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val screen = AppScreen.valueOf(screenName)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEmployeeDialog by remember { mutableStateOf(false) }
    var showAbsenceDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showClearMonthDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    fun savePlan(assignments: List<Assignment>, warnings: List<String> = plan.warnings) {
        val sorted = assignments.sortedWith(compareBy<Assignment> { it.date }.thenBy { it.shiftId }.thenBy { it.employeeId })
        plan = PlanResult(sorted, warnings)
        store.saveAssignments(sorted)
        NextShiftWidget.refresh(context)
    }

    fun setMonth(month: YearMonth) {
        selectedMonthText = month.toString()
        selectedDateText = month.atDay(1).toString()
    }

    val navigate: (AppScreen) -> Unit = {
        selectedEmployeeId = null
        screenName = it.name
    }

    val generateSelectedMonth: () -> Unit = {
        val start = selectedMonth.atDay(1)
        val history = plan.assignments.filter { it.date.isBefore(start) }
        val generated = AutoPlanner.generate(
            start = start,
            days = selectedMonth.lengthOfMonth(),
            employees = employees,
            shifts = shifts,
            absences = absences,
            rules = rules,
            historyAssignments = history
        )
        val otherMonths = plan.assignments.filter { YearMonth.from(it.date) != selectedMonth }
        savePlan(otherMonths + generated.assignments, generated.warnings)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 840.dp
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                SidebarV4(screen, navigate)
                ScaffoldV4(
                    modifier = Modifier.weight(1f),
                    screen = screen,
                    showMenu = false,
                    employeeDetail = selectedEmployeeId != null,
                    onBackEmployee = { selectedEmployeeId = null },
                    onMenu = {},
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    ContentV4(
                        modifier = contentModifier,
                        tablet = true,
                        screen = screen,
                        selectedMonth = selectedMonth,
                        selectedDate = selectedDate,
                        selectedEmployeeId = selectedEmployeeId,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        navigate = navigate,
                        onMonthPicker = { showMonthPicker = true },
                        onPreviousMonth = { setMonth(selectedMonth.minusMonths(1)) },
                        onNextMonth = { setMonth(selectedMonth.plusMonths(1)) },
                        onDate = { selectedDateText = it.toString() },
                        onManual = { showManualDialog = true },
                        onClearMonth = { showClearMonthDialog = true },
                        onGenerate = generateSelectedMonth,
                        onEmployeeSelected = { selectedEmployeeId = it },
                        onDeleteAssignment = { target -> savePlan(plan.assignments - target) },
                        onDeleteEmployee = { employee ->
                            employees = employees.filterNot { it.id == employee.id }
                            absences = absences.filterNot { it.employeeId == employee.id }
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            savePlan(plan.assignments.filterNot { it.employeeId == employee.id })
                            selectedEmployeeId = null
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
                        BrandV4()
                        AppScreen.entries.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = screen == item,
                                icon = { Icon(screenIconV4(item), null) },
                                onClick = {
                                    navigate(item)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Text("0.4 Test · Monatsplanung", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
                    }
                }
            ) {
                ScaffoldV4(
                    modifier = Modifier.fillMaxSize(),
                    screen = screen,
                    showMenu = true,
                    employeeDetail = selectedEmployeeId != null,
                    onBackEmployee = { selectedEmployeeId = null },
                    onMenu = { scope.launch { drawerState.open() } },
                    onAddEmployee = { showEmployeeDialog = true },
                    onAddAbsence = { showAbsenceDialog = true }
                ) { contentModifier ->
                    ContentV4(
                        modifier = contentModifier,
                        tablet = false,
                        screen = screen,
                        selectedMonth = selectedMonth,
                        selectedDate = selectedDate,
                        selectedEmployeeId = selectedEmployeeId,
                        employees = employees,
                        shifts = shifts,
                        absences = absences,
                        rules = rules,
                        settings = settings,
                        plan = plan,
                        navigate = navigate,
                        onMonthPicker = { showMonthPicker = true },
                        onPreviousMonth = { setMonth(selectedMonth.minusMonths(1)) },
                        onNextMonth = { setMonth(selectedMonth.plusMonths(1)) },
                        onDate = { selectedDateText = it.toString() },
                        onManual = { showManualDialog = true },
                        onClearMonth = { showClearMonthDialog = true },
                        onGenerate = generateSelectedMonth,
                        onEmployeeSelected = { selectedEmployeeId = it },
                        onDeleteAssignment = { target -> savePlan(plan.assignments - target) },
                        onDeleteEmployee = { employee ->
                            employees = employees.filterNot { it.id == employee.id }
                            absences = absences.filterNot { it.employeeId == employee.id }
                            store.saveEmployees(employees)
                            store.saveAbsences(absences)
                            savePlan(plan.assignments.filterNot { it.employeeId == employee.id })
                            selectedEmployeeId = null
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
        AddEmployeeV4(
            onDismiss = { showEmployeeDialog = false },
            onAdd = {
                employees = employees + it
                store.saveEmployees(employees)
                showEmployeeDialog = false
            }
        )
    }

    if (showAbsenceDialog) {
        AddAbsenceV4(
            employees = employees,
            onDismiss = { showAbsenceDialog = false },
            onAdd = {
                absences = absences + it
                store.saveAbsences(absences)
                showAbsenceDialog = false
            }
        )
    }

    if (showManualDialog) {
        ManualAssignmentDialogV4(
            date = selectedDate,
            employees = employees,
            shifts = shifts.filter { it.enabled },
            onDismiss = { showManualDialog = false },
            onSave = { employeeId, shiftId ->
                val withoutSameEmployeeDay = plan.assignments.filterNot { it.date == selectedDate && it.employeeId == employeeId }
                savePlan(withoutSameEmployeeDay + Assignment(selectedDate, shiftId, employeeId))
                showManualDialog = false
            }
        )
    }

    if (showClearMonthDialog) {
        AlertDialog(
            onDismissRequest = { showClearMonthDialog = false },
            title = { Text("${monthLabelV4(selectedMonth)} leeren?") },
            text = { Text("Es werden nur die Dienste dieses Monats gelöscht. Andere Monate, Mitarbeiter, Urlaub und Einstellungen bleiben erhalten.") },
            confirmButton = {
                Button(onClick = {
                    savePlan(plan.assignments.filter { YearMonth.from(it.date) != selectedMonth }, emptyList())
                    showClearMonthDialog = false
                }) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(6.dp))
                    Text("MONAT LEEREN")
                }
            },
            dismissButton = { TextButton(onClick = { showClearMonthDialog = false }) { Text("Abbrechen") } }
        )
    }

    if (showMonthPicker) {
        MonthPickerV4(
            current = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelect = {
                setMonth(it)
                showMonthPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldV4(
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
                        Text("SchichtPilot 0.4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SidebarV4(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    Surface(Modifier.width(250.dp).fillMaxSize(), tonalElevation = 3.dp) {
        LazyColumn(Modifier.padding(vertical = 18.dp)) {
            item { BrandV4() }
            items(AppScreen.entries) { item ->
                NavigationDrawerItem(
                    label = { Text(item.title) },
                    selected = screen == item,
                    icon = { Icon(screenIconV4(item), null) },
                    onClick = { navigate(item) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            item { HorizontalDivider(); Text("Tablet · 0.4", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
        }
    }
}

@Composable
private fun BrandV4() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text("SchichtPilot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Einfach. Planen. Zusammen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ContentV4(
    modifier: Modifier,
    tablet: Boolean,
    screen: AppScreen,
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    selectedEmployeeId: Long?,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    rules: PlannerRules,
    settings: AppSettings,
    plan: PlanResult,
    navigate: (AppScreen) -> Unit,
    onMonthPicker: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onManual: () -> Unit,
    onClearMonth: () -> Unit,
    onGenerate: () -> Unit,
    onEmployeeSelected: (Long) -> Unit,
    onDeleteAssignment: (Assignment) -> Unit,
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
                    EmployeeDetailV4(employee, selectedMonth, plan.assignments, shifts, absences, onMonthPicker, onPreviousMonth, onNextMonth, onDate)
                }
                return@Box
            }

            when (screen) {
                AppScreen.HOME -> HomeV4(employees, absences, plan, settings, navigate)
                AppScreen.SCHEDULE -> ScheduleV4(tablet, selectedMonth, selectedDate, plan.assignments, employees, shifts, absences, onMonthPicker, onPreviousMonth, onNextMonth, onDate, onManual, onClearMonth, onDeleteAssignment)
                AppScreen.AUTOPLAN -> AutoPlanV4(selectedMonth, employees, shifts, rules, plan, onMonthPicker, onPreviousMonth, onNextMonth, onGenerate, onClearMonth)
                AppScreen.EMPLOYEES -> EmployeesV4(employees, plan.assignments, selectedMonth, onEmployeeSelected, onDeleteEmployee)
                AppScreen.SHIFTS -> ShiftsV4(shifts, onShiftChange)
                AppScreen.ABSENCES -> AbsencesV4(absences, employees, settings.friendlyHints, onDeleteAbsence)
                AppScreen.RULES -> RulesV4(rules, onRulesChange)
                AppScreen.STATS -> StatsV4(plan, employees, selectedMonth)
                AppScreen.SETTINGS -> SettingsV4(settings, onSettingsChange)
            }
        }
    }
}

@Composable
private fun HomeV4(employees: List<Employee>, absences: List<Absence>, plan: PlanResult, settings: AppSettings, navigate: (AppScreen) -> Unit) {
    val next = plan.assignments.filter { !it.date.isBefore(LocalDate.now()) }.minByOrNull { it.date }
    val vacation = absences.filter { it.type.equals("Urlaub", true) && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
    val vacationDays = vacation?.let { ChronoUnit.DAYS.between(LocalDate.now(), it.start) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Hallo 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("${settings.profession} · ${settings.federalState}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nächster Dienst", fontWeight = FontWeight.SemiBold)
                    Text(if (next == null) "Noch kein Plan" else dateLongV4(next.date), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (next == null) "Erstelle oder trage einen Dienst ein." else "Der nächste gespeicherte Dienst ist eingeplant.")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("🏖️ Urlaub", fontWeight = FontWeight.Bold)
                    Text(if (vacationDays == null) "Noch keiner eingetragen" else "Noch $vacationDays Tage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Start und Ende wählst du bequem im Kalender.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCardV4("📅 Kalender", "Monatsplan", Modifier.weight(1f)) { navigate(AppScreen.SCHEDULE) }
                ActionCardV4("✨ Planen", "Monatsautomatik", Modifier.weight(1f)) { navigate(AppScreen.AUTOPLAN) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCardV4("👥 Team", "${employees.size} Personen", Modifier.weight(1f)) { navigate(AppScreen.EMPLOYEES) }
                ActionCardV4("🏖️ Urlaub", "${absences.size} Einträge", Modifier.weight(1f)) { navigate(AppScreen.ABSENCES) }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ActionCardV4(title: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun MonthToolbarV4(month: YearMonth, onPick: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrevious) { Text("‹") }
            TextButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text(monthLabelV4(month), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onNext) { Text("›") }
        }
    }
}

@Composable
private fun ScheduleV4(
    tablet: Boolean,
    month: YearMonth,
    selectedDate: LocalDate,
    assignments: List<Assignment>,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    onMonthPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onManual: () -> Unit,
    onClearMonth: () -> Unit,
    onDeleteAssignment: (Assignment) -> Unit
) {
    val monthAssignments = assignments.filter { YearMonth.from(it.date) == month }
    val calendar: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthToolbarV4(month, onMonthPicker, onPrevious, onNext)
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                MonthCalendar(
                    month = month,
                    assignments = assignments,
                    shifts = shifts,
                    employees = employees,
                    absences = absences,
                    selectedDate = selectedDate,
                    onSelectedDate = onDate,
                    onPreviousMonth = onPrevious,
                    onNextMonth = onNext,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onManual, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.EditCalendar, null); Spacer(Modifier.width(6.dp)); Text("MANUELL")
                }
                OutlinedButton(onClick = onClearMonth, modifier = Modifier.weight(1f), enabled = monthAssignments.isNotEmpty()) {
                    Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(6.dp)); Text("MONAT LEEREN")
                }
            }
        }
    }
    val detail: @Composable () -> Unit = { DayDetailV4(selectedDate, assignments, employees, shifts, absences, onManual, onDeleteAssignment) }

    if (tablet) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1.8f)) { calendar() }
            Box(Modifier.weight(1f)) { detail() }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { calendar() }
            item { detail() }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun DayDetailV4(
    date: LocalDate,
    assignments: List<Assignment>,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    onManual: () -> Unit,
    onDelete: (Assignment) -> Unit
) {
    val dayAssignments = assignments.filter { it.date == date }
    val dayAbsences = absences.filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(dateLongV4(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (dayAssignments.isEmpty() && dayAbsences.isEmpty()) Text("Noch nichts eingetragen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            dayAssignments.forEach { assignment ->
                val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                val employee = employees.firstOrNull { it.id == assignment.employeeId }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(12.dp).background(shiftColorV4(assignment.shiftId), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${shift?.code ?: "?"} · ${employee?.name ?: "Unbekannt"}", fontWeight = FontWeight.SemiBold)
                        Text("${shift?.start ?: "--"} – ${shift?.end ?: "--"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onDelete(assignment) }) { Text("Entfernen") }
                }
            }
            dayAbsences.forEach { absence ->
                val employee = employees.firstOrNull { it.id == absence.employeeId }
                Text("${absenceCodeV4(absence.type)} · ${employee?.name ?: "Mitarbeiter"} · ${absence.type}", color = absenceColorV4(absence.type), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("DIENST EINTRAGEN") }
        }
    }
}

@Composable
private fun AutoPlanV4(
    month: YearMonth,
    employees: List<Employee>,
    shifts: List<ShiftTemplate>,
    rules: PlannerRules,
    plan: PlanResult,
    onMonthPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGenerate: () -> Unit,
    onClearMonth: () -> Unit
) {
    val monthCount = plan.assignments.count { YearMonth.from(it.date) == month }
    val previousMonth = month.minusMonths(1)
    val historyCount = plan.assignments.count { YearMonth.from(it.date) == previousMonth }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthToolbarV4(month, onMonthPicker, onPrevious, onNext) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Monat automatisch planen", fontWeight = FontWeight.Bold)
                    Text("${month.lengthOfMonth()} Kalendertage · Regeln und Urlaub werden berücksichtigt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("↩ Vormonat wird mitgedacht", fontWeight = FontWeight.SemiBold)
                    Text("$historyCount gespeicherte Dienste aus ${monthLabelV4(previousMonth)} stehen als Vorgeschichte zur Verfügung. Dadurch gelten Ruhezeit, Wochenlimit und Arbeitstage am Stück über den Monatswechsel weiter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = employees.isNotEmpty() && shifts.any { it.enabled }) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (monthCount == 0) "MONAT ERSTELLEN" else "MONAT NEU BERECHNEN", fontWeight = FontWeight.Bold)
            }
        }
        item {
            OutlinedButton(onClick = onClearMonth, modifier = Modifier.fillMaxWidth(), enabled = monthCount > 0) {
                Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("NUR ${monthLabelV4(month).uppercase()} LEEREN")
            }
        }
        item { Text("${employees.size} Mitarbeiter · ${shifts.count { it.enabled }} Schichten · max. ${rules.maxConsecutiveDays} Tage am Stück · ${rules.minRestHours} h Ruhezeit", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (monthCount > 0) item { InfoV4("✅ $monthCount Dienste in ${monthLabelV4(month)} gespeichert") }
        if (plan.warnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null); Spacer(Modifier.width(8.dp)); Text("${plan.warnings.size} Konflikte", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(8.dp)); plan.warnings.take(12).forEach { Text("• $it") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeesV4(employees: List<Employee>, assignments: List<Assignment>, month: YearMonth, onSelect: (Long) -> Unit, onDelete: (Employee) -> Unit) {
    if (employees.isEmpty()) { CenterV4("Noch niemand im Team", "Tippe auf + und lege den ersten Mitarbeiter an."); return }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Anzeige für ${monthLabelV4(month)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(employees, key = { it.id }) { employee ->
            val count = assignments.count { it.employeeId == employee.id && YearMonth.from(it.date) == month }
            Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(employee.id) }, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(employee.name.take(1).uppercase(), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(employee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$count Dienste · max. ${employee.maxDaysPerWeek} Tage/Woche", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { onDelete(employee) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun EmployeeDetailV4(
    employee: Employee,
    month: YearMonth,
    assignments: List<Assignment>,
    shifts: List<ShiftTemplate>,
    absences: List<Absence>,
    onMonthPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDate: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selectedDateText by rememberSaveable(employee.id, month.toString()) { mutableStateOf(month.atDay(1).toString()) }
    val selectedDate = LocalDate.parse(selectedDateText)
    val employeeAssignments = assignments.filter { it.employeeId == employee.id }
    val employeeAbsences = absences.filter { it.employeeId == employee.id }
    val monthAssignments = employeeAssignments.filter { YearMonth.from(it.date) == month }
    val monthNights = monthAssignments.count { it.shiftId == "night" }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(employee.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Max. ${employee.maxDaysPerWeek} Tage/Woche", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallStatV4("Dienste", monthAssignments.size.toString(), Modifier.weight(1f))
                        SmallStatV4("Nächte", monthNights.toString(), Modifier.weight(1f))
                        SmallStatV4("Urlaub/Frei", employeeAbsences.count { YearMonth.from(it.start) == month || YearMonth.from(it.end) == month }.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { EmployeePdfExporter.printMonthlyPlan(context, employee, month, employeeAssignments, shifts, employeeAbsences) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Description, null); Spacer(Modifier.width(8.dp)); Text("DIENSTPLAN ALS PDF")
                    }
                }
            }
        }
        item { MonthToolbarV4(month, onMonthPicker, onPrevious, onNext) }
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                MonthCalendar(
                    month = month,
                    assignments = employeeAssignments,
                    shifts = shifts,
                    employees = listOf(employee),
                    absences = employeeAbsences,
                    selectedDate = selectedDate,
                    onSelectedDate = { selectedDateText = it.toString(); onDate(it) },
                    onPreviousMonth = onPrevious,
                    onNextMonth = onNext,
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
                    Text(dateLongV4(selectedDate), fontWeight = FontWeight.Bold)
                    when {
                        absence != null -> Text(absence.type, color = absenceColorV4(absence.type), style = MaterialTheme.typography.titleMedium)
                        assignment != null -> {
                            val shift = shifts.firstOrNull { it.id == assignment.shiftId }
                            Text("${shift?.name ?: "Schicht"} · ${shift?.start ?: "--"}–${shift?.end ?: "--"}", color = shiftColorV4(assignment.shiftId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
private fun SmallStatV4(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun ShiftsV4(shifts: List<ShiftTemplate>, onUpdate: (ShiftTemplate) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(shifts, key = { it.id }) { shift ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(shiftColorV4(shift.id), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(shift.code, color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(shift.name, fontWeight = FontWeight.Bold); Text("${shift.start} – ${shift.end} · ${shift.breakMinutes} Min. Pause", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Switch(checked = shift.enabled, onCheckedChange = { onUpdate(shift.copy(enabled = it)) })
                    }
                    NumberControlV4("Mindestbesetzung", shift.requiredEmployees, 1, 10) { onUpdate(shift.copy(requiredEmployees = it)) }
                    NumberControlV4("Pause (Min.)", shift.breakMinutes, 0, 120, 15) { onUpdate(shift.copy(breakMinutes = it)) }
                }
            }
        }
    }
}

@Composable
private fun AbsencesV4(absences: List<Absence>, employees: List<Employee>, friendly: Boolean, onDelete: (Absence) -> Unit) {
    if (absences.isEmpty()) { CenterV4("Noch kein Urlaub eingetragen", "Tippe auf +. Start und Ende wählst du im Kalender."); return }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (friendly) {
            val next = absences.filter { it.type.equals("Urlaub", true) && !it.start.isBefore(LocalDate.now()) }.minByOrNull { it.start }
            if (next != null) item {
                val remaining = ChronoUnit.DAYS.between(LocalDate.now(), next.start)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) { Text("🏖️ Vorfreude läuft", fontWeight = FontWeight.Bold); Text("Noch $remaining Tage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Dann heißt es: Dienstplan zu, Urlaub an 😎", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        items(absences.sortedBy { it.start }, key = { it.id }) { absence ->
            val employee = employees.firstOrNull { it.id == absence.employeeId }
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(absenceColorV4(absence.type).copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Text(absenceCodeV4(absence.type), color = absenceColorV4(absence.type), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("${absence.type} · ${employee?.name ?: "Mitarbeiter"}", fontWeight = FontWeight.Bold); Text("${dateShortV4(absence.start)} – ${dateShortV4(absence.end)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { onDelete(absence) }) { Text("Löschen") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RulesV4(rules: PlannerRules, onChange: (PlannerRules) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NumberCardV4("Max. Arbeitstage am Stück", rules.maxConsecutiveDays, 1, 7) { onChange(rules.copy(maxConsecutiveDays = it)) } }
        item { NumberCardV4("Mindestruhezeit (Stunden)", rules.minRestHours, 8, 16) { onChange(rules.copy(minRestHours = it)) } }
        item { ToggleV4("Schnelle Wechsel vermeiden", "Keine problematischen Nacht/Spät → Früh-Folgen.", rules.avoidFastRotation) { onChange(rules.copy(avoidFastRotation = it)) } }
        item { ToggleV4("Wochenenden fair verteilen", "Wochenenddienste möglichst gleichmäßig verteilen.", rules.distributeWeekendsFairly) { onChange(rules.copy(distributeWeekendsFairly = it)) } }
        item { InfoV4("Monatswechsel werden ab 0.4 automatisch mit den letzten Diensten des Vormonats geprüft.") }
    }
}

@Composable
private fun StatsV4(plan: PlanResult, employees: List<Employee>, month: YearMonth) {
    val monthAssignments = plan.assignments.filter { YearMonth.from(it.date) == month }
    val nights = monthAssignments.count { it.shiftId == "night" }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(monthLabelV4(month), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { MetricV4("Mitarbeiter", employees.size.toString()) }
        item { MetricV4("Dienste", monthAssignments.size.toString()) }
        item { MetricV4("Nachtdienste", nights.toString()) }
        item { MetricV4("Konflikte letzter Lauf", plan.warnings.size.toString()) }
    }
}

@Composable
private fun SettingsV4(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val professions = listOf("Sicherheitsdienst", "Pflege", "Gastronomie", "Logistik", "Produktion", "Einzelhandel", "Reinigung", "Sonstige")
    val states = listOf("Nordrhein-Westfalen", "Bayern", "Baden-Württemberg", "Hessen", "Niedersachsen", "Berlin", "Hamburg")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Berufsgruppe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(professions) { profession -> FilterChip(selected = settings.profession == profession, onClick = { onChange(settings.copy(profession = profession)) }, label = { Text(profession) }) }
        item { Spacer(Modifier.height(8.dp)); Text("Bundesland", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(states) { state -> FilterChip(selected = settings.federalState == state, onClick = { onChange(settings.copy(federalState = state)) }, label = { Text(state) }) }
        item { ToggleV4("Freundliche Hinweise", "Urlaubs-Countdown und kleine motivierende Texte anzeigen.", settings.friendlyHints) { onChange(settings.copy(friendlyHints = it)) } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAbsenceV4(employees: List<Employee>, onDismiss: () -> Unit, onAdd: (Absence) -> Unit) {
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
                if (employees.isEmpty()) Text("Lege zuerst einen Mitarbeiter an.")
                else {
                    Text("Mitarbeiter", fontWeight = FontWeight.Bold)
                    employees.forEach { e -> FilterChip(selected = selectedId == e.id, onClick = { selectedId = e.id }, label = { Text(e.name) }) }
                    Text("Art", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Urlaub", "Frei", "Wunschfrei", "Krank").forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) }) }
                    }
                    OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.fillMaxWidth()) { Text("Start: ${dateShortV4(start)}") }
                    OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.fillMaxWidth()) { Text("Ende: ${dateShortV4(end)}") }
                }
            }
        },
        confirmButton = { Button(onClick = { selectedId?.let { onAdd(Absence(employeeId = it, type = type, start = start, end = if (end.isBefore(start)) start else end)) } }, enabled = selectedId != null) { Text("SPEICHERN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )

    if (pickStart) DatePickerPopupV4("Startdatum", start, { pickStart = false }) { start = it; if (end.isBefore(it)) end = it; pickStart = false }
    if (pickEnd) DatePickerPopupV4("Enddatum", end, { pickEnd = false }) { end = if (it.isBefore(start)) start else it; pickEnd = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopupV4(title: String, initial: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = {
            val millis = state.selectedDateMillis
            if (millis != null) onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
        }) { Text("Übernehmen") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }) {
        Column { Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 0.dp)); DatePicker(state = state) }
    }
}

@Composable
private fun ManualAssignmentDialogV4(date: LocalDate, employees: List<Employee>, shifts: List<ShiftTemplate>, onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
    var employeeId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var shiftId by remember { mutableStateOf(shifts.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dienst manuell eintragen") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(dateLongV4(date), fontWeight = FontWeight.Bold) }
                item { Text("Mitarbeiter", fontWeight = FontWeight.Bold) }
                items(employees) { employee -> FilterChip(selected = employeeId == employee.id, onClick = { employeeId = employee.id }, label = { Text(employee.name) }) }
                item { Text("Schicht", fontWeight = FontWeight.Bold) }
                items(shifts) { shift -> FilterChip(selected = shiftId == shift.id, onClick = { shiftId = shift.id }, label = { Text("${shift.code} · ${shift.name} ${shift.start}–${shift.end}") }) }
            }
        },
        confirmButton = { Button(onClick = { if (employeeId != null && shiftId != null) onSave(employeeId!!, shiftId!!) }, enabled = employeeId != null && shiftId != null) { Text("EINTRAGEN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun MonthPickerV4(current: YearMonth, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    var year by remember(current) { mutableIntStateOf(current.year) }
    val names = java.time.Month.values()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monat auswählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { year-- }) { Text("‹") }
                    Text(year.toString(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { year++ }) { Text("›") }
                }
                names.toList().chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { month ->
                            val ym = YearMonth.of(year, month)
                            FilterChip(selected = ym == current, onClick = { onSelect(ym) }, label = { Text(month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.GERMAN)) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Schließen") } }
    )
}

@Composable
private fun AddEmployeeV4(onDismiss: () -> Unit, onAdd: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var maxDays by remember { mutableIntStateOf(5) }
    var allowNight by remember { mutableStateOf(true) }
    var allowChange by remember { mutableStateOf(true) }
    var allowWeekend by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitarbeiter anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                NumberControlV4("Max. Tage pro Woche", maxDays, 1, 7) { maxDays = it }
                ToggleV4("Nachtschicht erlaubt", "", allowNight) { allowNight = it }
                ToggleV4("Wechselschicht erlaubt", "", allowChange) { allowChange = it }
                ToggleV4("Wochenende erlaubt", "", allowWeekend) { allowWeekend = it }
            }
        },
        confirmButton = { Button(onClick = { onAdd(Employee(name = name.trim(), maxDaysPerWeek = maxDays, allowNight = allowNight, allowShiftChange = allowChange, allowWeekend = allowWeekend)) }, enabled = name.isNotBlank()) { Text("ANLEGEN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun ToggleV4(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun NumberCardV4(title: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { NumberControlV4(title, value, min, max, 1, onValue) } }
}

@Composable
private fun NumberControlV4(title: String, value: Int, min: Int, max: Int, step: Int = 1, onValue: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, modifier = Modifier.weight(1f))
        TextButton(onClick = { onValue((value - step).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
        Text(value.toString(), fontWeight = FontWeight.Bold)
        TextButton(onClick = { onValue((value + step).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
    }
}

@Composable
private fun MetricV4(label: String, value: String) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun InfoV4(text: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text(text, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun CenterV4(title: String, detail: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

private fun screenIconV4(screen: AppScreen): ImageVector = when (screen) {
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

private fun shiftColorV4(id: String): Color = when (id) {
    "early" -> Color(0xFF3F83F8)
    "late" -> Color(0xFFF59E0B)
    "night" -> Color(0xFF7C3AED)
    else -> Color(0xFF64748B)
}

private fun absenceColorV4(type: String): Color = when (type.lowercase()) {
    "urlaub" -> Color(0xFF0F9D7A)
    "krank" -> Color(0xFFD64545)
    "frei" -> Color(0xFF3F83F8)
    "wunschfrei" -> Color(0xFF7C3AED)
    else -> Color(0xFF64748B)
}

private fun absenceCodeV4(type: String): String = when (type.lowercase()) {
    "urlaub" -> "U"
    "krank" -> "K"
    "frei" -> "Frei"
    "wunschfrei" -> "WF"
    else -> "A"
}

private fun monthLabelV4(month: YearMonth): String = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)).replaceFirstChar { it.uppercase() }
private fun dateShortV4(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun dateLongV4(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)).replaceFirstChar { it.uppercase() }
