package com.example.taskman

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

class AppAddPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var boardName by remember { mutableStateOf("") }

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

                    CreateButton()
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
                val intent = Intent(context, AppHomePage::class.java)
                context.startActivity(intent)
            }
    )
}

@Composable
fun CreateButton() {
    Button(onClick = {
        println("Przycisk został kliknięty!")
    },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF404041),
            contentColor = Color.White
    ),
    shape = RoundedCornerShape(25.dp)) {
        Text(text = "Create")
    }
}