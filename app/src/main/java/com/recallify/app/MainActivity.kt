package com.recallify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recallify.app.data.local.database.DatabaseProvider
import com.recallify.app.data.local.entity.NoteEntity
import com.recallify.app.data.repository.NoteRepository
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
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val navController = rememberNavController()

            RecallifyTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val notes by viewModel.filteredNotes.collectAsState(initial = emptyList())
                            val searchQuery by viewModel.searchQuery.collectAsState()
                            
                            NoteListScreen(
                                notes = notes,
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.updateSearch(it) },
                                onThemeToggle = { viewModel.toggleTheme(this@MainActivity) },
                                onAddClick = { navController.navigate("edit/-1") },
                                onNoteClick = { navController.navigate("edit/${it.id}") },
                                onDeleteNote = { viewModel.deleteNote(it) },
                                onUndoDelete = { viewModel.undoDelete() },
                                onTogglePin = { viewModel.togglePin(it) }
                            )
                        }
                        composable(
                            route = "edit/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                            EditNoteScreen(
                                noteId = noteId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<NoteEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onThemeToggle: () -> Unit,
    onAddClick: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onUndoDelete: () -> Unit,
    onTogglePin: (NoteEntity) -> Unit
) {
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
                onClick = onAddClick,
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
                            onClick = { onNoteClick(note) },
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
                            onTogglePin = onTogglePin
                        )
                    }
                }

                if (otherNotes.isNotEmpty()) {
                    item { SectionHeader("RECENT") }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteItemComponent(
                            note = note,
                            onClick = { onNoteClick(note) },
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
                            onTogglePin = onTogglePin
                        )
                    }
                }
                
                if (notes.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(18f) }

    LaunchedEffect(noteId) {
        if (noteId != -1) {
            val existingNote = viewModel.getNoteById(noteId)
            existingNote?.let {
                note = it
                title = it.title
                content = it.content
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (noteId == -1) {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                viewModel.addNote(title, content)
                            }
                        } else {
                            note?.let {
                                viewModel.updateNote(it.copy(title = title, content = content))
                            }
                        }
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isBold = !isBold },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.FormatBold, "Bold")
                }
                
                IconButton(
                    onClick = { isItalic = !isItalic },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.FormatItalic, "Italic")
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { if (fontSize < 40f) fontSize += 2f }) {
                    Icon(Icons.Default.Add, "Increase Size")
                }
                
                Text("${fontSize.toInt()}", style = MaterialTheme.typography.bodyMedium)

                IconButton(onClick = { if (fontSize > 12f) fontSize -= 2f }) {
                    Icon(Icons.Default.Remove, "Decrease Size")
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing...") },
                modifier = Modifier.fillMaxSize().weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    fontSize = fontSize.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
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
    onClick: () -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit
) {
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp))

    Card(
        onClick = onClick,
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
                        text = note.title.ifBlank { "Untitled Note" },
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
                Row {
                    IconButton(
                        onClick = { onTogglePin(note) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { onDelete(note) }) {
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
}
