package com.activitypoints.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.activitypoints.ui.screens.*
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.TutorViewModel
// ── Route constants ────────────────────────────────────────────────────────────

object Routes {
    // Auth
    const val UNIFIED_LOGIN      = "unified_login"
    const val VERIFY_OTP         = "verify_otp/{registerNumber}"
    const val FORGOT_PASSWORD    = "forgot_password"
    const val RESET_PASSWORD     = "reset_password/{registerNumber}"
    const val TUTOR_FORGOT_PW    = "tutor_forgot_password"

    // Student
    const val STUDENT_HOME       = "student_home"
    const val STUDENT_PROFILE    = "student_profile"

    // Tutor
    const val TUTOR_HOME             = "tutor_home"
    const val TUTOR_PROFILE          = "tutor_profile"
    const val TUTOR_STUDENT_DETAILS  = "tutor_student_details/{studentId}"

    // Helper builders
    fun verifyOtp(registerNumber: String) = "verify_otp/$registerNumber"
    fun resetPassword(registerNumber: String) = "reset_password/$registerNumber"
    fun studentDetail(studentId: String) = "tutor_student_details/$studentId"
}

// ── Root NavGraph ──────────────────────────────────────────────────────────────

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.authState.collectAsState()

    // While token is being validated, show a centered spinner — no flash to login
    if (authState is AuthState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = when (authState) {
        is AuthState.Student   -> Routes.STUDENT_HOME
        is AuthState.Tutor     -> Routes.TUTOR_HOME
        else                   -> Routes.UNIFIED_LOGIN
    }

    // Once auth resolves, navigate away from the start destination if needed
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Student  -> navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Tutor    -> navController.navigate(Routes.TUTOR_HOME) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.LoggedOut -> navController.navigate(Routes.UNIFIED_LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            else -> Unit
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {
        // ── Auth screens ───────────────────────────────────────────────────────

        composable(Routes.UNIFIED_LOGIN) {
            UnifiedLoginScreen(
                authViewModel = authViewModel,
                navController = navController,
            )
        }

        composable(Routes.VERIFY_OTP) { backStack ->
            val registerNumber = backStack.arguments?.getString("registerNumber") ?: ""
            VerifyOtpScreen(
                registerNumber = registerNumber,
                navController  = navController,
                authViewModel  = authViewModel,
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(Routes.RESET_PASSWORD) { backStack ->
            val registerNumber = backStack.arguments?.getString("registerNumber") ?: ""
            ResetPasswordScreen(
                registerNumber = registerNumber,
                navController  = navController,
            )
        }

        composable(Routes.TUTOR_FORGOT_PW) {
            TutorForgotPasswordScreen(navController = navController)
        }

        // ── Student app ────────────────────────────────────────────────────────

        composable(Routes.STUDENT_HOME) {
            StudentHomeScreen(
                authViewModel = authViewModel,
                navController = navController,
            )
        }

        composable(Routes.STUDENT_PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                navController = navController,
            )
        }

        // ── Tutor app ──────────────────────────────────────────────────────────

        composable(Routes.TUTOR_HOME) {
            TutorHomeScreen(
                authViewModel = authViewModel,
                navController = navController,
            )
        }

        composable(Routes.TUTOR_PROFILE) {
            val tutorViewModel: TutorViewModel = hiltViewModel()

            TutorProfileScreen(
                authViewModel = authViewModel,
                tutorViewModel = tutorViewModel,
                navController = navController,
            )
        }

        composable(Routes.TUTOR_STUDENT_DETAILS) { backStack ->
            val studentId = backStack.arguments?.getString("studentId") ?: ""
            TutorStudentDetailsScreen(
                studentId     = studentId,
                navController = navController,
            )
        }
    }
}
