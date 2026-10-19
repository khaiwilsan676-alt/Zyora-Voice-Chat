package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ReportNative(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val reportedId: String = "",
    val reportedName: String = "",
    val category: String = "",
    val description: String = "",
    val proofImage: String = "",
    val timestamp: Long = 0L
)

@Composable
fun Reports(onBack: () -> Unit = {}) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ReportNative?>(null) }
    val reports = remember { mutableStateListOf<ReportNative>() }
    val filtered = reports.filter {
        val q = query.lowercase()
        it.senderName.lowercase().contains(q) ||
            it.senderId.lowercase().contains(q) ||
            it.reportedName.lowercase().contains(q) ||
            it.reportedId.lowercase().contains(q)
    }

    if (selected != null) {
        ReportDetails(selected!!, { selected = null })
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Report", style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search by ID or Name...") },
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(filtered, key = { it.id }) { report ->
                Row(
                    Modifier.fillMaxWidth().clickable { selected = report }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(report.senderId.ifBlank { "User" }, Modifier.weight(1f))
                    Text(report.reportedId.ifBlank { "User" }, Modifier.weight(1f))
                }
                HorizontalDivider()
            }
            if (filtered.isEmpty()) {
                item { Text("No reports found.", Modifier.fillMaxWidth().padding(48.dp)) }
            }
        }
    }
}

@Composable
private fun ReportDetails(report: ReportNative, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 28.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("‹") }
            Text("Report Details", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text("Types", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Violence", "Abusing", "Illegal", "Other's").forEach { Text(it, Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(report.description.ifBlank { "No description provided" }, Modifier.fillMaxWidth().padding(vertical = 12.dp))
                Text("Uploaded Image", style = MaterialTheme.typography.titleMedium)
                Text(if (report.proofImage.isBlank()) "No Image" else report.proofImage, Modifier.padding(vertical = 12.dp))
            }
        }
    }
}