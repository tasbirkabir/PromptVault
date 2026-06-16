package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PromptVaultViewModel

enum class AuthMode {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    viewModel: PromptVaultViewModel,
    initialIsSignUp: Boolean = false,
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var authMode by remember { mutableStateOf(if (initialIsSignUp) AuthMode.SIGNUP else AuthMode.LOGIN) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var successMessage by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Home",
                    color = CoolGrayMuted,
                    fontSize = 14.sp
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = when (authMode) {
                        AuthMode.LOGIN -> "Welcome back to PromptVault"
                        AuthMode.SIGNUP -> "Get your secure vault today"
                        AuthMode.FORGOT_PASSWORD -> "Reset vault access key"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (authMode) {
                        AuthMode.LOGIN -> "Log in with your private Supabase credentials"
                        AuthMode.SIGNUP -> "Create a password-protected tenant space"
                        AuthMode.FORGOT_PASSWORD -> "We will simulate a secure password reset protocol"
                    },
                    color = CoolGrayMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Error Banner
                if (authError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorColor.copy(alpha = 0.1f))
                            .border(1.dp, ErrorColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = authError ?: "",
                            color = ErrorColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Success Message
                if (successMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldAccent.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = EmeraldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Email Input
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    placeholder = { Text("tasbir777x@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CoolGrayMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = CardBackground,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        cursorColor = EmeraldAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                // Password Input
                if (authMode != AuthMode.FORGOT_PASSWORD) {
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoolGrayMuted) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = CoolGrayMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            cursorColor = EmeraldAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Password Confirm for Signup
                if (authMode == AuthMode.SIGNUP) {
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoolGrayMuted) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            cursorColor = EmeraldAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Forgot Password link
                if (authMode == AuthMode.LOGIN) {
                    TextButton(
                        onClick = { authMode = AuthMode.FORGOT_PASSWORD },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?", color = EmeraldAccent, fontSize = 12.sp)
                    }
                }

                // Main CTA Button
                Button(
                    onClick = {
                        successMessage = null
                        when (authMode) {
                            AuthMode.LOGIN -> {
                                viewModel.login(email, password, onAuthSuccess)
                            }
                            AuthMode.SIGNUP -> {
                                viewModel.signup(email, password, confirmPassword, onAuthSuccess)
                            }
                            AuthMode.FORGOT_PASSWORD -> {
                                viewModel.resetPassword(email) {
                                    successMessage = "Secure Supabase recovery code sent to $email."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    enabled = !isAuthLoading
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(color = ObsidianBg, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = when (authMode) {
                                AuthMode.LOGIN -> "Log In with Supabase"
                                AuthMode.SIGNUP -> "Complete Signup (Free)"
                                AuthMode.FORGOT_PASSWORD -> "Send Recovery Link"
                            },
                            color = ObsidianBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Secondary modes toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (authMode) {
                            AuthMode.LOGIN -> "Don't have a vault account?"
                            AuthMode.SIGNUP -> "Already setup a workspace?"
                            AuthMode.FORGOT_PASSWORD -> "Remembered your key?"
                        },
                        color = CoolGrayMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = {
                            successMessage = null
                            authMode = when (authMode) {
                                AuthMode.LOGIN -> AuthMode.SIGNUP
                                AuthMode.SIGNUP -> AuthMode.LOGIN
                                AuthMode.FORGOT_PASSWORD -> AuthMode.LOGIN
                            }
                        }
                    ) {
                        Text(
                            text = when (authMode) {
                                AuthMode.LOGIN -> "Sign Up Free"
                                AuthMode.SIGNUP -> "Sign In Here"
                                AuthMode.FORGOT_PASSWORD -> "Sign In Here"
                            },
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
