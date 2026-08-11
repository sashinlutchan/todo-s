package com.todo.to_do.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.todo.to_do.ui.screens.AuthScreen
import com.todo.to_do.ui.screens.ForgotPasswordScreen
import com.todo.to_do.ui.screens.ProfileScreen
import com.todo.to_do.ui.screens.ResetPasswordScreen
import com.todo.to_do.ui.screens.SplashScreen
import com.todo.to_do.ui.screens.TodoEditorScreen
import com.todo.to_do.ui.screens.TodoListScreen
import com.todo.to_do.ui.screens.VerifyEmailScreen
import com.todo.to_do.ui.screens.VerifyResetCodeScreen

private const val TRANSITION_DURATION_MS = 320

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        enterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) +
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { it / 6 }
        },
        exitTransition = { fadeOut(tween(TRANSITION_DURATION_MS)) },
        popEnterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) +
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { -it / 6 }
        },
        popExitTransition = {
            fadeOut(tween(TRANSITION_DURATION_MS)) +
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { it / 6 }
        }
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onFinished = { isLoggedIn ->
                    val destination = if (isLoggedIn) TodoListRoute else AuthRoute
                    navController.navigate(destination) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<AuthRoute> {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(TodoListRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                },
                onNeedsVerification = { email -> navController.navigate(VerifyEmailRoute(email)) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) }
            )
        }
        composable<VerifyEmailRoute> { entry ->
            val route: VerifyEmailRoute = entry.toRoute()
            VerifyEmailScreen(
                email = route.email,
                onVerified = {
                    navController.navigate(TodoListRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onCodeSent = { email -> navController.navigate(VerifyResetCodeRoute(email)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<VerifyResetCodeRoute> { entry ->
            val route: VerifyResetCodeRoute = entry.toRoute()
            VerifyResetCodeScreen(
                email = route.email,
                onVerified = { resetToken -> navController.navigate(ResetPasswordRoute(resetToken)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ResetPasswordRoute> { entry ->
            val route: ResetPasswordRoute = entry.toRoute()
            ResetPasswordScreen(
                resetToken = route.resetToken,
                onSuccess = {
                    navController.navigate(AuthRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<TodoListRoute> {
            TodoListScreen(
                onOpenTodo = { navController.navigate(TodoEditorRoute(it)) },
                onAddTodo = { navController.navigate(TodoEditorRoute()) },
                onOpenProfile = { navController.navigate(ProfileRoute) }
            )
        }
        composable<TodoEditorRoute> { entry ->
            val route: TodoEditorRoute = entry.toRoute()
            TodoEditorScreen(todoId = route.todoId, onBack = { navController.popBackStack() })
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(AuthRoute) {
                        popUpTo(TodoListRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}