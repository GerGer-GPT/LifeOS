package com.lifeos.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.personal.ui.theme.LifeOsTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeOsTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Зачем LifeOS нужны данные здоровья", style = MaterialTheme.typography.headlineSmall)
                        Text("Шаги, сон, пульс, тренировки, вес и активность используются только для вашего обзора дня и локальных рекомендаций. Доступ можно отозвать в Health Connect в любой момент.")
                        Button(onClick = ::finish) { Text("Понятно") }
                    }
                }
            }
        }
    }
}
