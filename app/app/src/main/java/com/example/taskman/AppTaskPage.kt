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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog // <--- DODANY KLUCZOWY IMPORT
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
class AppTaskPage : ComponentActivity() {
    private var token = ""
    private var boardId = -1
    private var boardName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        token = intent.getStringExtra("TOKEN_CSRF") ?: ""
        boardId = intent.getIntExtra("BOARD_ID", -1)
        boardName = intent.getStringExtra("BOARD_NAME") ?: "Task Page"

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp)
                    .offset(y = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Przeniosłem Spacer do wnętrza Column, żeby zachować poprawny układ
                Spacer(modifier = Modifier.height(40.dp))

                ClickableExitIcon()

                Spacer(modifier = Modifier.height(40.dp))

                TaskBox(boardName = boardName, token = token, boardId = boardId)
            }
        }
    }
}
@Composable
fun TaskBox(boardName: String, token: String, boardId: Int) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f)
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
                                Log.d("API", "Dodaję zadanie -> Nazwa: $taskName, Opis: $taskDescription")
                                val task = AddTaskRequest(token, taskName, taskDescription)
                                coroutineScope.launch {
                                    try {
                                        val resonse = api.addTask(task, boardId)
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