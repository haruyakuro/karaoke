package com.example.studyreward.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pointsBalance: Int,
    pendingCount: Int,
    dueSoonCount: Int,
    doneCount: Int,
    onOpenTaskList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("ホーム") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ポイント残高
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("ポイント残高", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("$pointsBalance pt", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "全部交換すると ${pointsBalance / 10 * 5} 分(10pt = 5分)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // 課題の件数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CountCard("未完了", pendingCount, Modifier.weight(1f))
                CountCard("締切が近い", dueSoonCount, Modifier.weight(1f))
                CountCard("完了", doneCount, Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onOpenTaskList,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("課題一覧を見る")
            }
        }
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text("$count 件", style = MaterialTheme.typography.titleLarge)
        }
    }
}