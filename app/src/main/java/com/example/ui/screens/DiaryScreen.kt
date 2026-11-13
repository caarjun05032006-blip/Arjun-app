package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiaryEntry
import com.example.security.DiarySecurityManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnlocked by viewModel.isDiaryUnlocked.collectAsState()
    val isConfigured = remember { viewModel.isDiaryPinConfigured() }

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isSettingUp by remember { mutableStateOf(!isConfigured) }

    val entries by viewModel.allDiaryEntries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }
    var viewingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confidential Personal Diary", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(
                            onClick = { viewModel.lockDiary() },
                            modifier = Modifier.testTag("btn_lock_diary")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Diary", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isUnlocked) {
                FloatingActionButton(
                    onClick = {
                        viewingEntry = null
                        showAddDialog = true
                    },
                    containerColor = Color(0xFFE11D48),
                    modifier = Modifier.testTag("fab_add_diary")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "New Entry", tint = Color.White)
                }
            }
        }
    ) { padding ->
        if (!isUnlocked) {
            // Lock Screen UI
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(28.dp))
                        }

                        Text(
                            text = if (isSettingUp) "Set Up Diary Passcode" else "Diary Locked",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = if (isSettingUp)
                                "Choose a secure 4-6 digit passcode to encrypt and protect your private diary entries."
                            else
                                "Enter your passcode to decrypt and access your confidential diary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it; authError = null },
                            label = { Text("Passcode (PIN)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_diary_pin")
                        )

                        if (isSettingUp) {
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { confirmPin = it; authError = null },
                                label = { Text("Confirm Passcode") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_diary_confirm_pin")
                            )
                        }

                        if (authError != null) {
                            Text(authError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                if (isSettingUp) {
                                    if (enteredPin.length < 4) {
                                        authError = "Passcode must be at least 4 digits"
                                    } else if (enteredPin != confirmPin) {
                                        authError = "Passcodes do not match"
                                    } else {
                                        viewModel.setupDiaryPin(enteredPin)
                                    }
                                } else {
                                    val ok = viewModel.unlockDiary(enteredPin)
                                    if (!ok) {
                                        authError = "Incorrect passcode. Please try again."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_auth_diary")
                        ) {
                            Text(if (isSettingUp) "Create Passcode & Open" else "Unlock Diary")
                        }
                    }
                }
            }
        } else {
            // Decrypted Unlocked Diary View
            val moods = listOf("All", "Happy", "Peaceful", "Productive", "Neutral", "Stressed", "Low", "Excited")

            val filteredEntries = entries.filter { entry ->
                val matchesMood = selectedMoodFilter == "All" || entry.mood == selectedMoodFilter
                val plainTitle = viewModel.decryptDiaryTitle(entry)
                val matchesSearch = searchQuery.isBlank() || plainTitle.contains(searchQuery, ignoreCase = true) || entry.tags.contains(searchQuery, ignoreCase = true)
                matchesMood && matchesSearch
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search title, keywords, tags...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth().testTag("input_diary_search"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Mood Filters
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moods.take(5).forEach { mood ->
                            FilterChip(
                                selected = selectedMoodFilter == mood,
                                onClick = { selectedMoodFilter = mood },
                                label = { Text(mood, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (filteredEntries.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No diary entries found", style = MaterialTheme.typography.titleMedium)
                                Text("Write your thoughts, ideas, reflections in absolute privacy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val decryptedTitle = viewModel.decryptDiaryTitle(entry)
                        val decryptedContent = viewModel.decryptDiaryContent(entry)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewingEntry = entry
                                    showAddDialog = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = decryptedTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Badge(containerColor = Color(0xFFE11D48).copy(alpha = 0.15f)) {
                                        Text(entry.mood, color = Color(0xFFE11D48), fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${entry.date} at ${entry.time}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = decryptedContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tags: #${entry.tags.replace(",", " #")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { viewModel.deleteDiaryEntry(entry) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val entry = viewingEntry
        DiaryEntryDialog(
            entry = entry,
            initialTitle = if (entry != null) viewModel.decryptDiaryTitle(entry) else "",
            initialContent = if (entry != null) viewModel.decryptDiaryContent(entry) else "",
            onDismiss = {
                showAddDialog = false
                viewingEntry = null
            },
            onSave = { date, time, title, content, mood, tags ->
                viewModel.saveDiaryEntry(
                    id = entry?.id ?: 0L,
                    date = date,
                    time = time,
                    plainTitle = title,
                    plainContent = content,
                    mood = mood,
                    tags = tags
                )
                showAddDialog = false
                viewingEntry = null
            }
        )
    }
}

@Composable
fun DiaryEntryDialog(
    entry: DiaryEntry?,
    initialTitle: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (date: String, time: String, title: String, content: String, mood: String, tags: String) -> Unit
) {
    val date = entry?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val time = entry?.time ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var mood by remember { mutableStateOf(entry?.mood ?: "Neutral") }
    var tags by remember { mutableStateOf(entry?.tags ?: "") }

    val moodOptions = listOf("Happy", "Peaceful", "Productive", "Neutral", "Stressed", "Low", "Excited")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "New Private Entry" else "Edit Diary Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content is encrypted before saving and never transmitted to AI or servers.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("input_diary_title")
                )

                Text("Mood:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    moodOptions.take(4).forEach { m ->
                        FilterChip(
                            selected = mood == m,
                            onClick = { mood = m },
                            label = { Text(m, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Confidential Thoughts / Reflections") },
                    modifier = Modifier.fillMaxWidth().height(140.dp).testTag("input_diary_content")
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated, e.g. health, work, ideas)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(date, time, title, content, mood, tags)
                    }
                },
                modifier = Modifier.testTag("btn_save_diary_entry")
            ) {
                Text("Encrypt & Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
