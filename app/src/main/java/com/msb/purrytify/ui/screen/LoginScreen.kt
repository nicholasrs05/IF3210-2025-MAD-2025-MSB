package com.msb.purrytify.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.R
import com.msb.purrytify.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val state = authViewModel.uiState.collectAsState()
    val context = LocalContext.current

//    LaunchedEffect(authViewModel.navigateToHome) {
//        authViewModel.navigateToHome.collectLatest { shouldNavigate ->
//            if (shouldNavigate) {
//                navController.navigate("home") {
//                    popUpTo("login") {
//                        inclusive = true
//                    }
//                    launchSingleTop = true
//                }
//            }
//        }
//    }

    LaunchedEffect(state.value.loginError) {
        if (!state.value.loginError.isNullOrEmpty()) {
            Toast.makeText(context, state.value.loginError, Toast.LENGTH_LONG).show()
            authViewModel.clearLoginError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF121212)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(2f),
            contentScale = ContentScale.FillWidth,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.3f)
                .align(Alignment.Center)
                .zIndex(4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .zIndex(2f)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Millions of Songs.\n" +
                        "Only on Purritify.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(top = 50.dp)
                .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Email Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Email",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.value.email,
                        onValueChange = { authViewModel.setEmail(it) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF535353),
                            unfocusedContainerColor = Color(0xFF535353)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Enter your email",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        },
                        isError = state.value.emailError != null,
                        supportingText = {
                            if (state.value.emailError != null) {
                                Text(
                                    text = state.value.emailError!!,
                                    color = Color.Red
                                )
                            }
                        }
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = state.value.password,
                        onValueChange = { authViewModel.setPassword(it) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF535353),
                            unfocusedContainerColor = Color(0xFF535353)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = {
                            Text(
                                "Enter your password",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff
                            val description =
                                if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, description, tint = Color(0xFF9a9a9a))
                            }
                        },
                        isError = state.value.passwordError != null,
                        supportingText = {
                            if (state.value.passwordError != null) {
                                Text(
                                    text = state.value.passwordError!!,
                                    color = Color.Red
                                )
                            }
                        }
                    )
                }

            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp)
                    .background(Color(0xFF1DB955), RoundedCornerShape(48.dp)),
                onClick = { authViewModel.loginWithValidation() },
                enabled = !state.value.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB955),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF535353),
                    disabledContentColor = Color.White
                )
            ) {
                if (state.value.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.background(Color(0xFF1DB955), RoundedCornerShape(48.dp))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController, hiltViewModel())
}