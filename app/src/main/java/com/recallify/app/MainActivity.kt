package com.recallify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.recallify.app.data.local.database.DatabaseProvider
import com.recallify.app.data.local.entity.NoteEntity
import com.recallify.app.data.repository.NoteRepository
import com.recallify.app.ui.components.AddNoteDialog
import com.recallify.app.ui.theme.RecallifyTheme
import com.recallify.app.viewmodel.NoteViewModel
import com.recallify.app.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = DatabaseProvider.getDatabase(this)
        val dao = database.noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]
        viewModel.loadTheme(this)

        setContent {
            val notes by viewModel.filteredNotes.collectAsState(initial = emptyList())
            val searchQuery by viewModel.searchQuery.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val aiResult by viewModel.aiResult.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val aiError by viewModel.error.collectAsState()

            RecallifyTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteScreen(
                        notes = notes,
                        searchQuery = searchQuery,
                        aiResult = aiResult,
                        isLoading = isLoading,
                        aiError = aiError,
                        onSearchChange = { viewModel.updateSearch(it) },
                        onThemeToggle = { viewModel.toggleTheme(this) },
                        onAddNote = { title, content -> viewModel.addNote(title, content) },
                        onUpdateNote = { viewModel.updateNote(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onUndoDelete = { viewModel.undoDelete() },
                        onTogglePin = { viewModel.togglePin(it) },
                        onSummarize = { viewModel.summarizeNote(it) },
                        onGenerateQuiz = { viewModel.generateQuiz(it) },
                        onGenerateEli5 = { viewModel.summarizeNote("Explain this simply like I'm 5: $it") },
                        onClearAi = { viewModel.clearAiResult() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    notes: List<NoteEntity>,
    searchQuery: String,
    aiResult: String?,
    isLoading: Boolean,
    aiError: String?,
    onSearchChange: (String) -> Unit,
    onThemeToggle: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onUndoDelete: () -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onSummarize: (String) -> Unit,
    onGenerateQuiz: (String) -> Unit,
    onGenerateEli5: (String) -> Unit,
    onClearAi: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Recall",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "ify",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🧠", fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, "Add Note") },
                text = { Text("New Note") },
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search your mind...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            val pinnedNotes = notes.filter { it.isPinned }
            val otherNotes = notes.filter { !it.isPinned }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item { SectionHeader("PINNED") }
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteItemComponent(
                            note = note,
                            onEdit = { noteToEdit = note },
                            onDelete = {
                                onDeleteNote(note)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        "Note moved to trash",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onUndoDelete()
                                }
                            },
                            onTogglePin = onTogglePin,
                            onSummarize = onSummarize,
                            onGenerateQuiz = onGenerateQuiz,
                            onGenerateEli5 = onGenerateEli5
                        )
                    }
                }

                if (otherNotes.isNotEmpty()) {
                    item { SectionHeader("RECENT") }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteItemComponent(
                            note = note,
                            onEdit = { noteToEdit = note },
                            onDelete = {
                                onDeleteNote(note)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        "Note deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onUndoDelete()
                                }
                            },
                            onTogglePin = onTogglePin,
                            onSummarize = onSummarize,
                            onGenerateQuiz = onGenerateQuiz,
                            onGenerateEli5 = onGenerateEli5
                        )
                    }
                }
                
                if (notes.isEmpty() && !isLoading) {
                    item {
                        EmptyState()
                    }
                }
            }
        }

        if (showAddDialog) {
            AddNoteDialog(
                onAdd = { title, content ->
                    onAddNote(title, content)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        noteToEdit?.let { note ->
            EditNoteDialog(
                note = note,
                onUpdate = { title, content ->
                    onUpdateNote(note.copy(title = title, content = content))
                    noteToEdit = null
                },
                onDismiss = { noteToEdit = null }
            )
        }

        if (aiResult != null || aiError != null) {
            AiResultDialog(result = aiResult, error = aiError, onDismiss = onClearAi)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.AutoMirrored.Filled.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No notes found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItemComponent(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onSummarize: (String) -> Unit,
    onGenerateQuiz: (String) -> Unit,
    onGenerateEli5: (String) -> Unit
) {
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp))

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = { onTogglePin(note) },
                    modifier = Modifier.offset(x = 12.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onSummarize(note.content) },
                    label = { Text("Summary", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(12.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                AssistChip(
                    onClick = { onGenerateQuiz(note.content) },
                    label = { Text("Quiz", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Quiz, null, modifier = Modifier.size(12.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                AssistChip(
                    onClick = { onGenerateEli5(note.content) },
                    label = { Text("ELI5", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.ChildCare, null, modifier = Modifier.size(12.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditNoteDialog(note: NoteEntity, onUpdate: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(title, content) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun AiResultDialog(result: String?, error: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                if (error != null) "Analysis Failed" else "AI Insights",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = error ?: result ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Got it") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun NoteScreenPreview() {
    RecallifyTheme {
        NoteScreen(
            notes = listOf(
                NoteEntity(id = 1, title = "Modern Architecture", content = "Learn about clean architecture and MVVM in Android. Focus on separation of concerns and testability.", isPinned = true, timestamp = System.currentTimeMillis()),
                NoteEntity(id = 2, title = "Gemini AI", content = "Integrating LLMs into mobile apps for smarter features like summarization and interactive quizzes.", timestamp = System.currentTimeMillis() - 86400000)
            ),
            searchQuery = "",
            aiResult = null,
            isLoading = false,
            aiError = null,
            onSearchChange = {},
            onThemeToggle = {},
            onAddNote = { _, _ -> },
            onUpdateNote = {},
            onDeleteNote = {},
            onUndoDelete = {},
            onTogglePin = {},
            onSummarize = {},
            onGenerateQuiz = {},
            onGenerateEli5 = {},
            onClearAi = {}
        )
    }
}
