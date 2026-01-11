package com.elearning.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CourseList : Screen("course_list")
    object CourseDetail : Screen("course_detail/{courseId}") {
        fun createRoute(courseId: String) = "course_detail/$courseId"
    }
    object Chat : Screen("chat/{courseId}") {
        fun createRoute(courseId: String) = "chat/$courseId"
    }
    object MyCourses : Screen("my_courses")
    object Profile : Screen("profile")
}
