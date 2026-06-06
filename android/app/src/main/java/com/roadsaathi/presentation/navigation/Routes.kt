package com.roadsaathi.presentation.navigation

sealed class Routes(val route: String) {
    data object Map : Routes("map")
    data object Camera : Routes("camera")
    data object Reports : Routes("reports")
    data object Profile : Routes("profile")
    data class ReportDetail(val localId: String) : Routes("report_detail/{localId}") {
        companion object {
            const val ROUTE = "report_detail/{localId}"
            fun createRoute(localId: String) = "report_detail/$localId"
        }
    }
}
