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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.lifecycleScope
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.launch

class AppHomePage : ComponentActivity() {

    private var privateBoards by mutableStateOf<List<Board>>(emptyList())
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = intent.getStringExtra("TOKEN_CSRF") ?: ""

        setContent {
            var searchQuery by remember { mutableStateOf("") }

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

                    BoardList("Your Boards", privateBoards)

                    Spacer(modifier = Modifier.height(20.dp))

                    //BoardList("Shared Boards")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (token.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    val response = api.getBoards(token)
                    if (response.isSuccessful) {
                        val wrapper = response.body()
                        if (wrapper != null) {
                            privateBoards = wrapper.listy // Aktualizacja stanu -> Compose sam przerysuje listę!
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API", "Błąd onResume: ${e.message}")
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
fun BoardList(textval: String, boards: List<Board>) {
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
            .height(160.dp)
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
                    boards.forEach { board ->
                        item {
                            Text(
                                text = board.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}