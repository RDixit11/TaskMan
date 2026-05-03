package com.example.taskman

import android.media.Image
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskman.RetrofitInstance.api
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController, startDestination = "login") {
                composable("register") { RegisterScreen(navController) }
                composable("login") { LoginScreen(navController) }
            }
        }
    }
}


@Composable // Komponent ekranu logowania
fun LoginScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF424242)),
        contentAlignment = Alignment.TopCenter
    ) {
        TopRect("Login")

        MidRectLogin(navController)
    }
}
@Composable // Komponent ekranu rejestracji
fun RegisterScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF424242)),
        contentAlignment = Alignment.TopCenter
    ) {
        TopRect("Register")

        MidRect(navController)
    }
}

@Composable // komponent który jest częścią komponentu logowania i rejestracji - czarny prostokąt u góry
fun TopRect(value: String) {
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
                value,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable // Komponent, który jest częścią ekranu rejestracji - jasno szany prostokąt na środku
fun MidRect(navController: NavController, modifier: Modifier = Modifier.offset(y = 230.dp)) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.55f)
            .background(
                color = Color(0xFF6a6a6a),
                shape = RoundedCornerShape(35.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .fillMaxWidth(1f)
                .offset(y=(70).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainTextField("Login", email) {email = it}
            Spacer(modifier = Modifier.height(35.dp))

            MainTextField("Password", password,true) {password = it}
            Spacer(modifier = Modifier.height(35.dp))

            MainTextField("Re-type password", repeatPassword, true) {repeatPassword = it}
            Spacer(modifier = Modifier.height(35.dp))

            val scope = rememberCoroutineScope()

            Btn("Sign up"){
                Log.d("API_DEBUG", "login=$email password=$password")
                scope.launch {
                    try {
                        val response = api.createUser(
                            User(login=email, haslo=password)
                        )

                        if (response.isSuccessful) {
                            Log.d("API", "Sukces")
                            navController.navigate("login")
                        } else {
                            Log.e("API", "Błąd: ${response.code()}")
                            Log.e("API", "BODY: ${response.errorBody()?.string()}")
                        }

                    } catch (e: Exception) {
                        Log.e("API", "Wyjątek: ${e.message}")
                    }
                }
            }
        }
    }
}

@Composable // Komponent, który jest częścią ekranu logowania - jasno szany prostokąt na środku
fun MidRectLogin(navController: NavController, modifier: Modifier = Modifier.offset(y = 230.dp)) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.55f)
            .background(
                color = Color(0xFF6a6a6a),
                shape = RoundedCornerShape(35.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .fillMaxWidth(1f)
                .offset(y=(70).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainTextField("Login", email) {email = it}
            Spacer(modifier = Modifier.height(35.dp))

            MainTextField("Password", password,true) {password = it}
            Spacer(modifier = Modifier.height(35.dp))

            Text(
                text = "Forgot password?",
                modifier = Modifier
                    .offset(x=(-70).dp, y=-20.dp)
                    .clickable {
                    navController.navigate("") //TO DO
                },
                color = Color.White,
            )

            val scope = rememberCoroutineScope()

            Btn("Log in"){
                Log.d("API_DEBUG", "login=$email password=$password")
                scope.launch {
                    try {
                        val response = api.createUser(
                            User(login=email, haslo=password)
                        )

                        if (response.isSuccessful) {
                            Log.d("API", "")
                            navController.navigate("") // TO DO
                        } else {
                            Log.e("API", "Błąd: ${response.code()}")
                            Log.e("API", "BODY: ${response.errorBody()?.string()}")
                        }

                    } catch (e: Exception) {
                        Log.e("API", "Wyjątek: ${e.message}")
                    }
                }
            }

            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) {
                    append("First time? ")
                }

                Spacer(modifier = Modifier.height(85.dp))
                withStyle(SpanStyle(color = Color(0xFF3982FF))) {
                    append("Register")
                }

            },
                modifier = Modifier.clickable {
                    navController.navigate("register")
                }
            )
        }
    }
}
@Composable // Komponent, który jest częścią MidRect oraz MidRectLogin - pola do wpisywania
fun MainTextField(
    labText: String,
    value: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
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
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword)
            KeyboardOptions(keyboardType = KeyboardType.Password)
        else
            KeyboardOptions.Default
    )
}

@Composable // Komponent z przyciskiem logowana / rejestrowania
fun Btn(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3982FF),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(35.dp)
    ){
        Text(text, fontSize = 16.sp)
    }
}