package com.activitypoints.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.activitypoints.navigation.Routes
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel

@Composable
fun UnifiedLoginScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
) {
    val authState  by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()

    // Navigate when logged in
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Student -> navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(Routes.UNIFIED_LOGIN) { inclusive = true }
            }
            is AuthState.Tutor   -> navController.navigate(Routes.TUTOR_HOME) {
                popUpTo(Routes.UNIFIED_LOGIN) { inclusive = true }
            }
            else -> Unit
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(loginState.error) {
        loginState.error?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearLoginError()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Student", "Tutor")

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo & title ───────────────────────────────────────────────────
            Icon(
                imageVector  = Icons.Outlined.School,
                contentDescription = null,
                modifier     = Modifier.size(64.dp),
                tint         = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text       = "Activity Points",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                text  = "Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            // ── Tab row ────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color    = MaterialTheme.colorScheme.primary,
                            )
                        },
                    ) {
                        tabs.forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTab == index,
                                onClick  = { selectedTab = index },
                                text     = { Text(label, fontWeight = FontWeight.SemiBold) },
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "tab_content",
                    ) { tab ->
                        when (tab) {
                            0 -> StudentLoginForm(
                                isLoading   = loginState.isLoading,
                                onLogin     = authViewModel::loginStudent,
                                onForgotPw  = { navController.navigate(Routes.FORGOT_PASSWORD) },
                                onOtp       = { reg ->
                                    navController.navigate(Routes.verifyOtp(reg))
                                },
                            )
                            else -> TutorLoginForm(
                                isLoading  = loginState.isLoading,
                                onLogin    = authViewModel::loginTutor,
                                onForgotPw = { navController.navigate(Routes.TUTOR_FORGOT_PW) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Student login form ─────────────────────────────────────────────────────────

@Composable
private fun StudentLoginForm(
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onForgotPw: () -> Unit,
    onOtp: (String) -> Unit,
) {
    val fm  = LocalFocusManager.current
    var reg by remember { mutableStateOf("") }
    var pw  by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value         = reg,
            onValueChange = { reg = it.uppercase() },
            label         = { Text("Register Number") },
            leadingIcon   = { Icon(Icons.Outlined.Badge, null) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { fm.moveFocus(FocusDirection.Down) }),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value         = pw,
            onValueChange = { pw = it },
            label         = { Text("Password") },
            leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon  = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(
                        if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPw) "Hide" else "Show",
                    )
                }
            },
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { fm.clearFocus(); onLogin(reg, pw) }),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick  = onForgotPw,
            modifier = Modifier.align(Alignment.End),
        ) { Text("Forgot Password?") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = { onLogin(reg, pw) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled  = !isLoading && reg.isNotBlank() && pw.isNotBlank(),
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Login", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = { if (reg.isNotBlank()) onOtp(reg) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled  = !isLoading,
        ) { Text("Login with OTP") }
    }
}

// ── Tutor login form ───────────────────────────────────────────────────────────

@Composable
private fun TutorLoginForm(
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onForgotPw: () -> Unit,
) {
    val fm    = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var pw    by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            label         = { Text("Email") },
            leadingIcon   = { Icon(Icons.Outlined.Email, null) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
            keyboardActions = KeyboardActions(onNext = { fm.moveFocus(FocusDirection.Down) }),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value         = pw,
            onValueChange = { pw = it },
            label         = { Text("Password") },
            leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon  = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(
                        if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                    )
                }
            },
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { fm.clearFocus(); onLogin(email, pw) }),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick  = onForgotPw,
            modifier = Modifier.align(Alignment.End),
        ) { Text("Forgot Password?") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = { onLogin(email, pw) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled  = !isLoading && email.isNotBlank() && pw.isNotBlank(),
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Tutor Login", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
