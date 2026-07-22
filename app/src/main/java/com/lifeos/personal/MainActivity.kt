package com.lifeos.personal

import android.accounts.AccountManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.personal.data.google.GoogleSyncRepository
import com.lifeos.personal.data.health.HealthConnectRepository
import com.lifeos.personal.data.health.HealthSnapshot
import com.lifeos.personal.data.local.DayPlanItem
import com.lifeos.personal.data.local.UserProfile
import com.lifeos.personal.ui.theme.LifeOsTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LifeOsTheme { LifeOsApp() } }
    }
}

private enum class Section(val label: String, val icon: String) {
    TODAY("Сегодня", "◉"), HEALTH("Здоровье", "♥"), PLAN("План", "☷"), PROFILE("Профиль", "●"), SETTINGS("Связи", "⚙")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LifeOsApp(vm: MainViewModel = viewModel()) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val plan by vm.dayPlan.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val healthMessage by vm.healthMessage.collectAsStateWithLifecycle()
    val account by vm.googleAccount.collectAsStateWithLifecycle()
    val googleMessage by vm.googleMessage.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(Section.TODAY) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val healthRepo = remember { HealthConnectRepository(context) }
    val googleRepo = remember { GoogleSyncRepository(context) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(healthRepo.permissionContract()) { vm.refreshHealth(healthRepo) }
    val accountLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!name.isNullOrBlank()) { vm.setGoogleAccount(name); googleRepo.accountName = name }
    }
    val googleAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        account?.let { vm.syncGoogle(googleRepo, it, health) }
    }
    LaunchedEffect(account) {
        googleRepo.accountName = account
        if (healthRepo.availability == HealthConnectClient.SDK_AVAILABLE && healthRepo.hasAllPermissions()) {
            vm.refreshHealth(healthRepo)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("LifeOS", fontWeight = FontWeight.Black); Text("персональный ритм", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF101720)),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xF20D1117)) {
                Section.entries.forEach { item ->
                    NavigationBarItem(selected = section == item, onClick = { section = item }, icon = { Text(item.icon) }, label = { Text(item.label) })
                }
            }
        },
    ) { padding ->
        when (section) {
            Section.TODAY -> TodayScreen(profile, plan, health, Modifier.padding(padding))
            Section.HEALTH -> HealthScreen(health, healthMessage, healthRepo.availability, {
                if (healthRepo.availability == HealthConnectClient.SDK_AVAILABLE) healthPermissionLauncher.launch(healthRepo.permissions)
            }, { vm.refreshHealth(healthRepo) }, Modifier.padding(padding))
            Section.PLAN -> PlanScreen(plan, vm::savePlan, Modifier.padding(padding))
            Section.PROFILE -> ProfileScreen(profile, vm::saveProfile, Modifier.padding(padding))
            Section.SETTINGS -> ConnectionsScreen(account, healthMessage, googleMessage,
                { accountLauncher.launch(googleRepo.chooseAccountIntent()) },
                { account?.let { vm.syncGoogle(googleRepo, it, health) } },
                {
                    googleRepo.pendingAuthorizationIntent?.let(googleAuthorizationLauncher::launch)
                        ?: account?.let { vm.syncGoogle(googleRepo, it, health) }
                },
                googleRepo.pendingAuthorizationIntent != null,
                Modifier.padding(padding))
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String? = null, modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        content()
    }
}

@Composable
private fun TodayScreen(profile: UserProfile, plan: List<DayPlanItem>, health: HealthSnapshot, modifier: Modifier) {
    val completed = plan.count { it.done }
    val bmr = (10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + 5).toInt()
    Page("Добрый день", "План, нагрузка и восстановление в одном месте", modifier) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Шаги", "${health.steps}", Modifier.weight(1f)); MetricCard("Сон", fmt(health.sleepHours, "ч"), Modifier.weight(1f)) } }
        item { ProgressCard("План дня", completed, plan.size) }
        item { InfoCard("Восстановление", recoveryText(health), "Рекомендация меняется по данным сна, пульса и активности.") }
        item { InfoCard("Питание", "Базовый обмен ≈ $bmr ккал", "Это ориентир, не готовый лимит. Дефицит будет уточняться по динамике веса.") }
        item { WarningCard("При выраженной слабости, перегреве или ухудшении самочувствия уменьшите нагрузку и ориентируйтесь на рекомендации врача.") }
    }
}

@Composable private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) = Card(modifier, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun InfoCard(title: String, value: String, note: String? = null) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun WarningCard(text: String) = Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2517)), shape = RoundedCornerShape(16.dp)) { Text(text, Modifier.padding(16.dp), color = Color(0xFFFFE8AA)) }
@Composable private fun ProgressCard(title: String, done: Int, total: Int) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Text(title); Text("$done из $total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, Modifier.fillMaxWidth().padding(top = 10.dp)) } }

@Composable
private fun HealthScreen(h: HealthSnapshot, status: String, availability: Int, connect: () -> Unit, refresh: () -> Unit, modifier: Modifier) {
    Page("Здоровье", "Данные Samsung Health через Health Connect", modifier) {
        item { InfoCard("Состояние", status, if (availability == HealthConnectClient.SDK_AVAILABLE) "Доступ можно изменить в Health Connect." else "Health Connect недоступен или требует установки/обновления.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Шаги · 24 ч", "${h.steps}", Modifier.weight(1f)); MetricCard("Расстояние", fmt(h.distanceKm, "км"), Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Сон", fmt(h.sleepHours, "ч"), Modifier.weight(1f)); MetricCard("Активность", fmt(h.activeCalories, "ккал"), Modifier.weight(1f)) } }
        item { InfoCard("Пульс", "Средний: ${h.averageHeartRate ?: "—"} · покой: ${h.restingHeartRate ?: "—"}", "Последние доступные данные Health Connect") }
        item { InfoCard("Состав тела", "Вес: ${h.weightKg?.let { fmt(it, "кг") } ?: "—"} · жир: ${h.bodyFatPercent?.let { fmt(it, "%") } ?: "—"}") }
        item { Button(connect, Modifier.fillMaxWidth()) { Text("Подключить / изменить доступ") } }
        item { OutlinedButton(refresh, Modifier.fillMaxWidth()) { Text("Обновить данные") } }
    }
}

@Composable
private fun PlanScreen(plan: List<DayPlanItem>, save: (List<DayPlanItem>) -> Unit, modifier: Modifier) {
    var time by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }
    Page("План дня", "LifeOS предлагает основу — вы корректируете её по ходу дня", modifier) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(time, { time = it }, modifier = Modifier.width(105.dp), label = { Text("Время") }, singleLine = true); OutlinedTextField(title, { title = it }, modifier = Modifier.weight(1f), label = { Text("Новый блок") }, singleLine = true) } }
        item { Button({ if (title.isNotBlank()) { save(plan + DayPlanItem(System.currentTimeMillis(), time.ifBlank { "—" }, title.trim())); time = ""; title = "" } }, Modifier.fillMaxWidth()) { Text("Добавить") } }
        items(plan, key = { it.id }) { item -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Checkbox(item.done, { checked -> save(plan.map { if (it.id == item.id) it.copy(done = checked) else it }) }); Column(Modifier.weight(1f)) { Text(item.time, color = MaterialTheme.colorScheme.primary); Text(item.title, fontWeight = FontWeight.SemiBold) }; TextButton({ save(plan.filterNot { it.id == item.id }) }) { Text("Удалить") } } } }
        item { OutlinedButton({ save(plan.map { it.copy(done = false) }) }, Modifier.fillMaxWidth()) { Text("Новый день: сбросить отметки") } }
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile, save: (UserProfile) -> Unit, modifier: Modifier) {
    var age by remember(profile.age) { mutableStateOf(profile.age.toString()) }; var height by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }; var weight by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }; var goal by remember(profile.goal) { mutableStateOf(profile.goal) }
    Page("Профиль", "Исходные данные для локальных расчётов", modifier) {
        item { NumberField("Возраст", age) { age = it } }; item { NumberField("Рост, см", height) { height = it } }; item { NumberField("Вес, кг", weight) { weight = it } }
        item { OutlinedTextField(goal, { goal = it }, label = { Text("Основная цель") }, modifier = Modifier.fillMaxWidth()) }
        item { InfoCard("Учитываем", "Без молочных продуктов", "Низкая начальная активность, гиперлордоз, РС и недопущение перегрева.") }
        item { Button({ save(UserProfile(age.toIntOrNull() ?: 29, height.toIntOrNull() ?: 170, weight.toIntOrNull() ?: 98, goal)) }, Modifier.fillMaxWidth()) { Text("Сохранить") } }
    }
}

@Composable private fun NumberField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, { onChange(it.filter(Char::isDigit)) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)

@Composable
private fun ConnectionsScreen(account: String?, healthStatus: String, googleStatus: String, choose: () -> Unit, sync: () -> Unit, authorize: () -> Unit, needsAuthorization: Boolean, modifier: Modifier) {
    Page("Мастер подключения", "Пройдите шаги один раз — затем LifeOS сможет обновлять данные", modifier) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16251F)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Первоначальная настройка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("1. Разрешите Samsung Health передавать данные в Health Connect.")
                    Text("2. На вкладке «Здоровье» разрешите LifeOS чтение выбранных категорий.")
                    Text("3. Выберите Google-аккаунт и подтвердите доступ.")
                    Text("4. Запустите синхронизацию — папка и отдельный календарь LifeOS будут найдены или созданы автоматически.")
                }
            }
        }
        item { InfoCard("Health Connect", healthStatus, "LifeOS только читает выбранные вами категории. Пароль Samsung не требуется.") }
        item { InfoCard("Google", account ?: "Аккаунт не выбран", googleStatus) }
        item { Button(choose, Modifier.fillMaxWidth()) { Text(if (account == null) "Выбрать Google-аккаунт" else "Сменить аккаунт") } }
        if (needsAuthorization) item { Button(authorize, Modifier.fillMaxWidth()) { Text("Подтвердить доступ Google") } }
        item { OutlinedButton(sync, Modifier.fillMaxWidth(), enabled = account != null) { Text("Синхронизировать Drive и календарь") } }
        item { InfoCard("Отдельный календарь LifeOS", "Основной календарь не изменяется", "При первой синхронизации создаётся отдельный календарь. Экран «План дня» остаётся редактируемым в приложении.") }
        item { InfoCard("Версия", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", BuildConfig.APPLICATION_ID) }
    }
}

private fun fmt(value: Double, suffix: String) = "%.1f %s".format(Locale.getDefault(), value, suffix)
private fun recoveryText(h: HealthSnapshot) = when { h.sleepHours in 0.1..5.9 -> "Сон короткий — облегчите нагрузку"; h.sleepHours >= 7.0 -> "Сон достаточный — ориентируйтесь на самочувствие"; else -> "Нужно больше данных" }
