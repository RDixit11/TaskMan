package com.example.taskman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.launch

class AppAddPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val LoginToken = intent.getStringExtra("TOKEN_CSRF") ?: ""

        setContent {
            var boardName by remember { mutableStateOf("") }
            var vis by remember { mutableStateOf<Boolean?>(null) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .offset(y = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClickableExitIcon()

                    Spacer(modifier = Modifier.weight(1f))

                    CreateButton(
                        boardName = boardName,
                        vis = vis,
                        onCreateClick = {
                            val boardInfo = CreateBoardRequest(LoginToken, boardName)

                            lifecycleScope.launch {
                                try {
                                    val response = if (vis == true) {
                                        api.createBoard(boardInfo)
                                    } else {
                                        api.createSharedBoard(boardInfo)
                                    }
                                    if (response.isSuccessful) {
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    Log.d("API", "Wyjątek: ${e.message}")
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AddTextField(
                        labText = "Name",
                        value = boardName,
                        onValueChange = { boardName = it },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

                    Spacer(modifier = Modifier.height(55.dp))

                    Text("Visibility", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(20.dp))

                    VisibilityOptions(currentVis = vis, onVisChanged = { vis = it })
                }
            }
        }
    }
}


@Composable
fun AddTextField(
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
        shape = RoundedCornerShape(25.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF404041),
            unfocusedContainerColor = Color(0xFF404041),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = Color(0xFF727272),
            unfocusedLabelColor = Color(0xFF727272)
        ),
        textStyle = TextStyle(fontSize = 14.sp),
        singleLine = true,
    )
}

@Composable
fun ClickableExitIcon() {
    val context = LocalContext.current

    Image(
        painter = painterResource(id = R.drawable.exit),
        contentDescription = "Wróć",
        modifier = Modifier
            .size(40.dp)
            .clickable {
                (context as? ComponentActivity)?.finish()
            }
    )
}

@Composable
fun CreateButton(boardName: String,
                 vis: Boolean?,
                 onCreateClick: () -> Unit) {
    Button(onClick = {
        if (boardName.isNotBlank() && vis != null) {
            onCreateClick()
    } else {
        println("Błąd: Wpisz nazwę i wybierz widoczność!")
    }},
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF404041),
            contentColor = Color.White
    ),
    shape = RoundedCornerShape(25.dp)) {
        Text(text = "Create")
    }
}

@Composable
fun VisibilityOptions(currentVis: Boolean?, onVisChanged: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f)
            .background(
                color = Color(0xFF404041),
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable {
                        onVisChanged(true)
                    }
                    .background(
                        if (currentVis == true) Color(0xFF505052) else Color.Transparent
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Private",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This board can be seen and edited only by you",
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF636365),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable {
                        onVisChanged(false)
                    }
                    .background(
                        if (currentVis == false) Color(0xFF505052) else Color.Transparent
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Public",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This board can be seen and edited by people you share this board with",
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp
                )
            }
        }
    }
}