package com.lifeos.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.personal.data.local.DayPlanItem
import com.lifeos.personal.data.local.UserProfile
import com.lifeos.personal.ui.theme.LifeOsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LifeOsTheme { LifeOsApp() } }
    }
}

private enum class Section(val label: String) { TODAY("Сегодня"), PLAN("План дня"), PROFILE("Профиль"), SETTINGS("Настройки") }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LifeOsApp(viewModel: MainViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val plan by viewModel.dayPlan.collectAsStateWithLifecycle()
    val testValue by viewModel.updateTestValue.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(Section.TODAY) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("LifeOS", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { Text(if (section == item) "●" else "○") },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (section) {
            Section.TODAY -> TodayScreen(profile, plan, Modifier.padding(padding))
            Section.PLAN -> PlanScreen(plan, viewModel::savePlan, Modifier.padding(padding))
            Section.PROFILE -> ProfileScreen(profile, viewModel::saveProfile, Modifier.padding(padding))
            Section.SETTINGS -> SettingsScreen(testValue, viewModel::incrementUpdateTestValue, Modifier.padding(padding))
        }
    }
}

@Composable
private fun TodayScreen(profile: UserProfile, plan: List<DayPlanItem>, modifier: Modifier = Modifier) {
    val completed = plan.count { it.done }
    val bmr = (10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + 5).toInt()
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Ваш день", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("План можно менять по ходу дня на отдельной странице.")
        }
        item { InfoCard("План", "$completed из ${plan.size} блоков выполнено") }
        item { InfoCard("Ориентир базового обмена", "$bmr ккал/сутки", "Это не готовая норма питания — активность и динамика веса будут учитываться отдельно.") }
        item { InfoCard("Восстановление", "Пока данных недостаточно", "Позже здесь появятся сон, пульс, усталость и рекомендации Health Connect.") }
        item { InfoCard("Сегодня важно", "Регулярность без перегрева", "При выраженной слабости или ухудшении самочувствия уменьшите нагрузку.") }
    }
}

@Composable
private fun InfoCard(title: String, value: String, note: String? = null) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun PlanScreen(plan: List<DayPlanItem>, save: (List<DayPlanItem>) -> Unit, modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("План дня", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("LifeOS предлагает основу. Вы корректируете её в любой момент.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(time, { time = it }, label = { Text("Время") }, modifier = Modifier.width(105.dp), singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Новый блок") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        save(plan + DayPlanItem(System.currentTimeMillis(), time.ifBlank { "—" }, title.trim()))
                        time = ""; title = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Добавить в план") }
        }
        items(plan, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(item.done, { checked -> save(plan.map { if (it.id == item.id) it.copy(done = checked) else it }) })
                    Column(Modifier.weight(1f)) {
                        Text(item.time, style = MaterialTheme.typography.labelMedium)
                        Text(item.title, fontWeight = if (item.done) FontWeight.Normal else FontWeight.SemiBold)
                    }
                    TextButton(onClick = { save(plan.filterNot { it.id == item.id }) }) { Text("Удалить") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { save(plan.map { it.copy(done = false) }) }, modifier = Modifier.fillMaxWidth()) { Text("Сбросить отметки на новый день") }
        }
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile, save: (UserProfile) -> Unit, modifier: Modifier = Modifier) {
    var age by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var height by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var weight by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var goal by remember(profile.goal) { mutableStateOf(profile.goal) }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { NumberField("Возраст", age) { age = it } }
        item { NumberField("Рост, см", height) { height = it } }
        item { NumberField("Вес, кг", weight) { weight = it } }
        item { OutlinedTextField(goal, { goal = it }, label = { Text("Основная цель") }, modifier = Modifier.fillMaxWidth()) }
        item { InfoCard("Ограничения", "Без молочных продуктов", "Низкая начальная активность, гиперлордоз и РС учитываются при будущих рекомендациях.") }
        item {
            Button(
                onClick = { save(UserProfile(age.toIntOrNull() ?: 29, height.toIntOrNull() ?: 170, weight.toIntOrNull() ?: 98, goal)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить профиль") }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit)) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun SettingsScreen(value: Int, increment: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { InfoCard("Версия", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", "${BuildConfig.APPLICATION_ID} · ${BuildConfig.BUILD_ID}") }
        item { InfoCard("Health Connect", "Ещё не подключён", "Будет получать данные Samsung Health только после вашего разрешения.") }
        item { InfoCard("Google", "Ещё не подключён", "Авторизация будет только через официальный OAuth.") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Проверка сохранения данных: $value")
                    Button(onClick = increment, modifier = Modifier.fillMaxWidth()) { Text("Увеличить значение") }
                }
            }
        }
    }
}
