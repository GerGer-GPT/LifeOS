package com.lifeos.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.personal.ui.theme.LifeOsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeOsTheme { LifeOsApp() }
        }
    }
}

@Composable
private fun LifeOsApp(viewModel: MainViewModel = viewModel()) {
    val savedValue by viewModel.updateTestValue.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("LifeOS", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Новая стабильная ветка")
            Spacer(Modifier.height(28.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Milestone 0", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("Package: ${BuildConfig.APPLICATION_ID}")
                    Text("Версия: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Text("Сборка: ${BuildConfig.BUILD_ID}")
                    Spacer(Modifier.height(18.dp))
                    Text("Сохранённое значение: $savedValue", fontWeight = FontWeight.SemiBold)
                    Text("Увеличьте его, затем установите следующую версию поверх этой.")
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = viewModel::incrementUpdateTestValue,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Увеличить значение")
                    }
                }
            }
        }
    }
}
