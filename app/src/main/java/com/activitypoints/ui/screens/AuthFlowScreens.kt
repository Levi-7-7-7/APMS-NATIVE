package com.activitypoints.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.activitypoints.navigation.Routes
import com.activitypoints.viewmodel.ForgotPasswordViewModel

// ══════════════════════════════════════════════════════════════════════════════
// STUDENT FORGOT PASSWORD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    vm: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState  by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var reg      by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbar.showSnackbar("OTP sent to your email!")
            vm.resetSuccess()
            navController.navigate(Routes.resetPassword(reg.uppercase()))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Forgot Password") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, "Back") }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Outlined.LockReset, null, Modifier.size(56.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
            Text("Reset Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Enter your register number to receive a password reset OTP.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value         = reg,
                onValueChange = { reg = it.uppercase() },
                label         = { Text("Register Number") },
                leadingIcon   = { Icon(Icons.Outlined.Badge, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (reg.isNotBlank()) vm.sendStudentOtp(reg) }),
            )
            Button(
                onClick  = { vm.sendStudentOtp(reg) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = reg.isNotBlank() && !uiState.isLoading,
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Send OTP", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STUDENT RESET PASSWORD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ResetPasswordScreen(
    registerNumber: String,
    navController: NavController,
    vm: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState  by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var otp      by remember { mutableStateOf("") }
    var newPw    by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            snackbar.showSnackbar("Password reset successfully!")
            vm.resetSuccess()
            navController.navigate(Routes.UNIFIED_LOGIN) {
                popUpTo(Routes.UNIFIED_LOGIN) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reset Password") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, "Back") }
        }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Enter OTP & New Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("OTP sent to your registered email for $registerNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = otp, onValueChange = { otp = it },
                label = { Text("OTP") }, leadingIcon = { Icon(Icons.Outlined.Pin, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = newPw, onValueChange = { newPw = it },
                label = { Text("New Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmPw, onValueChange = { confirmPw = it },
                label = { Text("Confirm Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPw.isNotEmpty() && confirmPw != newPw,
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick  = { vm.resetStudentPassword(registerNumber, otp, newPw) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = otp.isNotBlank() && newPw.isNotBlank() && newPw == confirmPw && !uiState.isLoading,
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Reset Password", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// VERIFY OTP (for OTP login)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun VerifyOtpScreen(
    registerNumber: String,
    navController: NavController,
    authViewModel: AuthViewModel,
    vm: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState  by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var otp      by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.sendStudentOtp(registerNumber) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.otpLoginSuccess) {
        uiState.otpLoginSuccess?.let { loginResp ->
            authViewModel.refreshStudentProfile(loginResp.student!!)
            navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(Routes.UNIFIED_LOGIN) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("OTP Login") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, "Back") }
        }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Verify OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Enter the OTP sent to your email for $registerNumber", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = otp, onValueChange = { otp = it },
                label = { Text("6-digit OTP") }, leadingIcon = { Icon(Icons.Outlined.Pin, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick  = { vm.verifyOtpLogin(registerNumber, otp) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = otp.isNotBlank() && !uiState.isLoading,
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Verify & Login", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { vm.sendStudentOtp(registerNumber) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Resend OTP")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR FORGOT PASSWORD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorForgotPasswordScreen(
    navController: NavController,
    vm: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState  by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var email    by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    var otp      by remember { mutableStateOf("") }
    var newPw    by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success && !showReset) {
            snackbar.showSnackbar("OTP sent to $email")
            vm.resetSuccess()
            showReset = true
        }
    }
    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            snackbar.showSnackbar("Password reset successfully!")
            vm.resetSuccess()
            navController.navigate(Routes.UNIFIED_LOGIN) { popUpTo(Routes.UNIFIED_LOGIN) { inclusive = true } }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tutor Reset Password") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, "Back") }
        }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            if (!showReset) {
                Text("Forgot Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Tutor Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Button(
                    onClick  = { vm.sendTutorOtp(email) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled  = email.isNotBlank() && !uiState.isLoading,
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Send OTP", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("Enter OTP & New Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("OTP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPw, onValueChange = { newPw = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confirmPw, onValueChange = { confirmPw = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), isError = confirmPw.isNotEmpty() && confirmPw != newPw, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick  = { vm.resetTutorPassword(email, otp, newPw) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled  = otp.isNotBlank() && newPw.isNotBlank() && newPw == confirmPw && !uiState.isLoading,
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Reset Password", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
