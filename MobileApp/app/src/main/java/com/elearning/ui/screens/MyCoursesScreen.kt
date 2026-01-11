package com.elearning.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elearning.ui.components.CourseCard
import com.elearning.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCoursesScreen(
    viewModel: CourseViewModel,
    onCourseClick: (String) -> Unit
) {
    val courses by viewModel.courses.collectAsState()
    val isTeacher by viewModel.isTeacher.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCourses()
    }

    val myCourses = remember(courses, isTeacher) {
        viewModel.getMyEnrolledCourses()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var courseToDeleteId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Courses",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCourses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Course")
                }
            }
        }
    ) { padding ->
        if (showCreateDialog) {
            CreateCourseDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { title, desc ->
                    viewModel.createCourse(title, desc)
                    showCreateDialog = false
                }
            )
        }

        if (courseToDeleteId != null) {
            AlertDialog(
                onDismissRequest = { courseToDeleteId = null },
                title = { Text("Delete Course") },
                text = { Text("Are you sure you want to delete this course? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            courseToDeleteId?.let { id ->
                                viewModel.deleteCourse(id)
                            }
                            courseToDeleteId = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { courseToDeleteId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (myCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        if (isTeacher) "No courses created yet" else "No enrolled courses yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        if (isTeacher) "Start by creating a new course!" else "Start learning by enrolling in courses!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                if (isTeacher)
                                    "You have created ${myCourses.size} course${if (myCourses.size != 1) "s" else ""}"
                                else
                                    "You're enrolled in ${myCourses.size} course${if (myCourses.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                items(myCourses) { course ->
                    // Fix for "Unknown Teacher": Fetch teacher name if missing
                    val teacherId = course.teacherId
                    val teacherName = course.teacherName
                    if (teacherName == null) {
                        viewModel.fetchTeacherName(teacherId)
                    }

                    val enrollmentCount by viewModel.getCourseEnrollmentCount(course.id).collectAsState(0)
                    CourseCard(
                        course = course,
                        enrollmentCount = enrollmentCount,
                        onClick = { onCourseClick(course.id.toString()) },
                        onChatClick = if (!isTeacher) { { /* Navigate to chat */ } } else null,
                        onDeleteClick = if (isTeacher) { { courseToDeleteId = course.id } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun CreateCourseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    initialTitle: String = "", // Added defaults
    initialDescription: String = "" // Added defaults
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Create New Course" else "Edit Course") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Course Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text(if (initialTitle.isEmpty()) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
