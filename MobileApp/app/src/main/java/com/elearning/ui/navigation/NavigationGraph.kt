package com.elearning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.elearning.ui.data.local.TokenManager
import com.elearning.ui.screens.*
import com.elearning.ui.viewmodel.ChatViewModel
import com.elearning.ui.viewmodel.CourseDetailViewModel
import com.elearning.ui.viewmodel.CourseViewModel

@Composable
fun NavigationGraph(navController: NavHostController) {
    val startDestination = if (TokenManager.getToken() != null) {
        Screen.CourseList.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.CourseList.route) {
            val viewModel: CourseViewModel = viewModel()
            CourseListScreen(
                viewModel = viewModel,
                onCourseClick = { courseId ->
                    navController.navigate(Screen.CourseDetail.createRoute(courseId))
                },
                onChatClick = { courseId ->
                    navController.navigate(Screen.Chat.createRoute(courseId))
                }
            )
        }

        composable(
            route = Screen.CourseDetail.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
            val viewModel: CourseDetailViewModel = viewModel()
            CourseDetailScreen(
                courseId = courseId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onChatClick = { navController.navigate(Screen.Chat.createRoute(courseId)) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
            val viewModel: ChatViewModel = viewModel()
            ChatScreen(
                courseId = courseId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.MyCourses.route) {
            val viewModel: CourseViewModel = viewModel()
            MyCoursesScreen(
                viewModel = viewModel,
                onCourseClick = { courseId ->
                    navController.navigate(Screen.CourseDetail.createRoute(courseId))
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.CourseList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.CourseList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
