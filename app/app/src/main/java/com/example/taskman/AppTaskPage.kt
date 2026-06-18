package com.example.taskman

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import kotlinx.coroutines.launch
class AppTaskPage : ComponentActivity() {
    private var token = ""
    private var boardId = -1
    private var boardName = ""

    private var isShared = false
    private var taskList by mutableStateOf<List<Tasks>>(emptyList())
    private var memberList by mutableStateOf<List<Member>>(emptyList())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        token = intent.getStringExtra("TOKEN_CSRF") ?: ""
        boardId = intent.getIntExtra("BOARD_ID", -1)
        boardName = intent.getStringExtra("BOARD_NAME") ?: "Task Page"

        isShared = intent.getBooleanExtra("IS_SHARED", false)

        refreshTasks()
        if (isShared) {
            refreshMembers()
        }

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp)
                    .offset(y = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                ClickableExitIcon()

                Spacer(modifier = Modifier.height(40.dp))

                TaskBox(
                    boardName = boardName,
                    token = token,
                    boardId = boardId,
                    isShared = isShared,
                    tasks = taskList,
                    onRefresh = { refreshTasks() },
                    modifier = Modifier.fillMaxHeight(if (isShared) 0.45f else 0.85f)
                )
                if (isShared) {
                    Spacer(modifier = Modifier.height(20.dp))
                    MembersBox(
                        members = memberList,
                        boardId = boardId,
                        token = token,
                        onMembersChanged = { refreshMembers() }
                    )
                }
            }
        }
    }
    private fun refreshTasks() {
        lifecycleScope.launch {
            try {
                val response = if (isShared) {
                    api.getSharedTasks(token, boardId)
                } else {
                    api.getTasks(boardId, token)
                }
                if (response.isSuccessful) {
                    val tasksResponse = response.body()
                    if (tasksResponse != null) {
                        taskList = tasksResponse.zadania.sortedBy { it.id }
                        Log.d("API", "Pobrano zadania: ${taskList.size}")
                    }
                } else {
                    Log.e("API", "Błąd pobierania zadań: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API", "Błąd sieci przy zadaniach: ${e.message}")
            }
        }
    }

    private fun refreshMembers() {
        lifecycleScope.launch {
            try {
                val response = api.getMembers(token=token,boardId)
                if (response.isSuccessful) {
                    memberList = response.body()?.members ?: emptyList()
                    Log.d("API", "Pobrano członków: ${memberList.size}")
                }
            } catch (e: Exception) {
                Log.e("API", "Błąd sieci przy członkach: ${e.message}")
            }
        }
    }
}
@Composable
fun TaskBox(
    boardName: String,
    token: String,
    boardId: Int,
    isShared: Boolean,
    tasks: List<Tasks>, onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var editingTask by remember { mutableStateOf<Tasks?>(null) }
    var editTaskName by remember { mutableStateOf("") }
    var editTaskDescription by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF404041),
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Text(
            text = boardName,
            color = Color.White,
            modifier = Modifier.padding(20.dp),
            fontSize = 18.sp
        )

        editingTask?.let { task ->
            EditDetailsDialog(
                title = "Edit Task",
                initialName = editTaskName,
                initialDescription = editTaskDescription,
                hasDescription = true,
                onDismiss = { editingTask = null },
                onConfirm = { updatedName, updatedDescription ->
                    editingTask = null
                    coroutineScope.launch {
                        try {
                            val request = AddTaskRequest(token, updatedName, updatedDescription)
                            val response = if (isShared) {
                                api.updateSharedTask(request, task.id)
                            } else {
                                api.updateTask(request, task.id)
                            }
                            if (response.isSuccessful) {
                                onRefresh()
                            }
                        } catch (e: Exception) {
                            Log.e("API", "Błąd edycji zadania: ${e.message}")
                        }
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 65.dp, start = 20.dp, end = 20.dp)
        ) {
            if (tasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks found in this board",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                items(items = tasks, key = { task -> task.id }) { task ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch {
                                    try {
                                        val response = if (isShared) {
                                            api.deleteSharedTask(token, task.id)
                                        } else {
                                            api.deleteTask(token, task.id)
                                        }
                                        if (response.isSuccessful) {
                                            onRefresh()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("API", "Błąd usuwania zadania: ${e.message}")
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFFD32F2F), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "Delete",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        content = {

                            TaskItem(task = task, onStatusClick = {
                                val newState = when (task.stan) {
                                    "niezrobione" -> "w_trakcie"
                                    "w_trakcie" -> "zrobione"
                                    "zrobione" -> "niezrobione"
                                    else -> "niezrobione"
                                }


                                val updateTask = UpdateTaskStatusRequest(token, newState)
                                coroutineScope.launch {
                                    try {
                                        val response = if (isShared) {
                                            api.updateSharedTaskStatus(updateTask, task.id)
                                        } else {
                                            api.updateTaskStatus(updateTask, task.id)
                                        }
                                        if (response.isSuccessful) {
                                            Log.d("API", "Status zmieniony pomyślnie!")
                                            onRefresh()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("API", "Błąd zmiany statusu: ${e.message}")
                                    }
                                }
                            },
                                onTaskClick = {
                                    editTaskName = task.tytul
                                    editTaskDescription = task.opis ?: ""
                                    editingTask = task
                                }
                            )
                        })
                }
            }
        }

        Text(
            text = "+ Add task",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .clickable {
                    taskName = ""
                    taskDescription = ""
                    showAddTaskDialog = true
                }
        )

        if (showAddTaskDialog) {
            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                title = { Text(text = "Add new task", color = Color.White) },
                text = {
                    Column {
                        Text("Name:", color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        TextField(
                            value = taskName,
                            onValueChange = { taskName = it },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Description (optional):", color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        TextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                containerColor = Color(0xFF282828),
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (taskName.isNotBlank()) {
                                showAddTaskDialog = false
                                val task = AddTaskRequest(token, taskName, taskDescription)
                                coroutineScope.launch {
                                    try {
                                        val response = if (isShared) {
                                            api.addSharedTask(task, boardId)
                                        } else {
                                            api.addTask(task, boardId)
                                        }
                                        if (response.isSuccessful) {
                                            Log.d("API", "Zadanie dodane!")
                                            onRefresh()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("API", "Błąd sieci: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = taskName.isNotBlank()
                    ) {
                        Text("Add", color = if (taskName.isNotBlank()) Color.White else Color.Gray)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun TaskItem(task: Tasks,onStatusClick: () -> Unit, onTaskClick: () -> Unit) {
    val isDone = task.stan == "zrobione"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF282828), shape = RoundedCornerShape(10.dp))
            .padding(12.dp)
            .clickable { onTaskClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = getIconForStatus(task.stan)),
            contentDescription = "Task Status",
            modifier = Modifier
                .size(28.dp)
                .padding(end = 12.dp)
                .clickable { onStatusClick() }
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.tytul,
                color = if (isDone) Color.Gray else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )

            if (!task.opis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.opis,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

fun getIconForStatus(status: String): Int {
    return when (status) {
        "w_trakcie" -> R.drawable.taskinprogress
        "zrobione"  -> R.drawable.taskdone
        else -> R.drawable.tasknotdone
    }
}

@Composable
fun MembersBox(
    members: List<Member>,
    boardId: Int,
    token: String,
    onMembersChanged: () -> Unit
) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .background(
                color = Color(0xFF282828),
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Board Members",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 40.dp)
            ) {
                if (members.isEmpty()) {
                    item {
                        Text("No members found", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    items(members) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(Color(0xFF404041), shape = RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .clickable { memberToDelete = member },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.login.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = member.login,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "+ Add member",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .clickable {
                    newMemberName = ""
                    showAddMemberDialog = true
                }
        )
    }

    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Remove member", color = Color.White) },
            text = { Text("Are you sure you want to remove ${member.login} from this board?", color = Color.LightGray) },
            containerColor = Color(0xFF282828),
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetMember = member
                        memberToDelete = null
                        coroutineScope.launch {
                            try {
                                val response = api.deleteMember(token, boardId, targetMember.id)
                                if (response.isSuccessful) {
                                    Log.d("API", "Pomyślnie usunięto członka: ${targetMember.login}")
                                    onMembersChanged()
                                } else {
                                    Log.e("API", "Błąd usuwania członka: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Log.e("API", "Błąd sieci podczas usuwania członka: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add member to board", color = Color.White) },
            text = {
                TextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    placeholder = { Text("Enter username", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF282828),
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMemberName.isNotBlank()) {
                            showAddMemberDialog = false
                            coroutineScope.launch {
                                try {
                                    val request = AddMemberRequest(token, newMemberName)
                                    val response = api.addMember(boardId, request)
                                    if (response.isSuccessful) {
                                        onMembersChanged()
                                    }
                                } catch (e: Exception) {
                                    Log.e("API", "Błąd dodawania: ${e.message}")
                                }
                            }
                        }
                    }
                ) { Text("Add", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}