package com.example.taskman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.lifecycleScope
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
class AppHomePage : ComponentActivity() {
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = intent.getStringExtra("TOKEN_CSRF") ?: ""

        setContent {
            var searchQuery by remember { mutableStateOf("") }
            var privateBoards by remember { mutableStateOf<List<Board>>(emptyList()) }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                if (token.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            val response = api.getBoards(token)
                            if (response.isSuccessful) {
                                val wrapper = response.body()
                                if (wrapper != null) {
                                    privateBoards = wrapper.listy
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("API", "Błąd pobierania danych w ON_RESUME: ${e.message}")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .offset(y = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome Back!",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .offset(x = 20.dp)
                            .weight(1f)
                    )

                    ClickableAddIcon(token)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SearchBar(
                        labText = "Search",
                        value = searchQuery,
                        icon = R.drawable.search,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BoardList(
                        textval = "Your Boards",
                        boards = privateBoards,
                        token = token,
                        onRefresh = {
                            lifecycleScope.launch {
                                try {
                                    val response = api.getBoards(token)
                                    if (response.isSuccessful) {
                                        val wrapper = response.body()
                                        if (wrapper != null) {
                                            privateBoards = wrapper.listy
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("API", "Błąd podczas odświeżania listy: ${e.message}")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    //BoardList("Shared Boards")
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    labText: String,
    value: String,
    icon: Int? = null,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labText) },
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF404041),
            unfocusedContainerColor = Color(0xFF404041),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = Color(0xFF828282),
            unfocusedLabelColor = Color(0xFF828282)
        ),
        textStyle = TextStyle(fontSize = 14.sp),
        singleLine = true,
        leadingIcon = icon?.let {
            {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}

@Composable
fun ClickableAddIcon(token: String) {
    val context = LocalContext.current

    Image(
        painter = painterResource(id = R.drawable.add),
        contentDescription = "Dodaj nową tablicę",
        modifier = Modifier
            .size(120.dp)
            .clickable {
                val intent = Intent(context, AppAddPage::class.java).apply {
                    putExtra("TOKEN_CSRF", token)
                }
                context.startActivity(intent)
            }
    )
}

@Composable
fun BoardList(textval: String, boards: List<Board>, token: String, onRefresh: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            textval,
            color = Color.White,
            fontSize = 25.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(200.dp)
            .background(
                color = Color(0xFF282828),
                shape = RoundedCornerShape(35.dp)
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (boards.isEmpty()) {
                    item {
                        Text(
                            text = "No boards found",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    items(boards) { board ->
                        var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
                        var showRenameDialog by remember { mutableStateOf(false) }
                        var newBoardName by remember { mutableStateOf(board.name) }
                        var showDeleteDialog by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(
                                        color = Color(0xFF404041),
                                        shape = RoundedCornerShape(15.dp)
                                    )
                                    .offset(x=10.dp)
                                    .clickable {
                                        val intent =
                                            Intent(context, AppTaskPage::class.java).apply {
                                                putExtra("TOKEN_CSRF", token)
                                                putExtra("BOARD_ID", board.id)
                                                putExtra("BOARD_NAME", board.name)
                                            }
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = board.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(vertical = 2.dp).weight(1f)

                                    )

                                    Box {
                                        Image(
                                            painter = painterResource(id = R.drawable.more),
                                            contentDescription = "Menu",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .offset(x = -20.dp)
                                                .clickable {
                                                    isMenuExpanded = true
                                                }
                                        )

                                        DropdownMenu(
                                            expanded = isMenuExpanded,
                                            onDismissRequest = { isMenuExpanded = false },
                                            offset = DpOffset(x = (-20).dp, y=0.dp),
                                            modifier = Modifier.background(Color(0xFF282828))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Change name", color = Color.White) },
                                                onClick = {
                                                    isMenuExpanded = false
                                                    newBoardName = board.name
                                                    showRenameDialog = true
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = { Text("Delete board", color = Color.Red) },
                                                onClick = {
                                                    isMenuExpanded = false
                                                    showDeleteDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        if (showRenameDialog) {
                            AlertDialog(
                                onDismissRequest = { showRenameDialog = false },
                                title = { Text(text = "Change board name", color = Color.White) },
                                text = {
                                    Column {
                                        Text("Enter new name for the board:", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                        TextField(
                                            value = newBoardName,
                                            onValueChange = { newBoardName = it },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF404041),
                                                unfocusedContainerColor = Color(0xFF404041),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }
                                },
                                containerColor = Color(0xFF282828),
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showRenameDialog = false
                                            if (newBoardName.isNotBlank()) {
                                                Log.d("API", "Zmieniam nazwę tablicy ${board.id} na: $newBoardName")
                                                coroutineScope.launch {
                                                    val boardInfo = CreateBoardRequest(token, newBoardName)
                                                    try {
                                                        val response = api.renameBoard(boardInfo, board.id)
                                                        if (response.isSuccessful) {
                                                            Log.d("API", "Nazwa zmieniona pomyślnie!")
                                                            onRefresh()
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("API", "Błąd zmiany nazwy: ${e.message}")
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Save", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRenameDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                }
                            )
                        }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text(text = "Delete board", color = Color.White) },
                                text = {
                                    Text("Are you sure you want to delete board \"${board.name}\"? This action cannot be undone.", color = Color.LightGray)
                                },
                                containerColor = Color(0xFF282828),
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false
                                            coroutineScope.launch {
                                                try {
                                                    val response = api.deleteBoard(token, board.id)
                                                    if (response.isSuccessful) {
                                                        Log.d("API", "Usunięto tablicę!")
                                                        onRefresh()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("API", "Błąd usuwania: ${e.message}")
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Delete", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                }
                            )
                        }

                    }
                }
            }
        }
    }
}