package com.example.taskman

import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RegisterScreen()
        }
    }
}

@Composable
fun RegisterScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF424242)),
        contentAlignment = Alignment.TopCenter
    ) {
        TopRect()

        MidRect(
            modifier = Modifier
                .offset(y=(230).dp)
        )
    }
}

@Composable
fun TopRect() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
            .offset(y=(-35).dp)
            .background(
                color = Color(0xFF282828),
                shape = RoundedCornerShape(35.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y=(-20).dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrow),
                contentDescription = "arrow icon",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Register",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MidRect(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.5f)
            .background(
                color = Color(0xFF6a6a6a),
                shape = RoundedCornerShape(35.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .fillMaxWidth(1f)
                .offset(y=(70).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainTextField("Email/Login")
            Spacer(modifier = Modifier.height(35.dp))

            MainTextField("Password")
            Spacer(modifier = Modifier.height(35.dp))

            MainTextField("Re-type password")
        }
    }
}

@Composable
fun MainTextField(labText:String) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(labText) },
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(55.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFD9D9D9),
            unfocusedContainerColor = Color(0xFFD9D9D9),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(35.dp),
        textStyle = TextStyle(fontSize = 14.sp),
        singleLine = true
    )
}
