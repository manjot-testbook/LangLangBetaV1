package com.example.langlangbetav1.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.langlangbetav1.scene.SceneScreen
import com.example.langlangbetav1.score.ScoreScreen
import com.example.langlangbetav1.splash.SplashScreen
import com.example.langlangbetav1.upi.PaymentSuccessScreen
import com.example.langlangbetav1.upi.SignupScreen
import com.example.langlangbetav1.upi.UpiPaymentScreen
import com.example.langlangbetav1.upi.UpiPinScreen

/** Two-destination nav graph: splash → scene/{sceneId} */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = "splash",
    ) {

        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("scene/0") {
                        // Remove splash from the back stack so Back exits the app
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = "scene/{stepId}",
            arguments = listOf(navArgument("stepId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val stepId = backStackEntry.arguments?.getInt("stepId") ?: 0
            SceneScreen(stepId = stepId, navController = navController)
        }

        composable("score") {
            ScoreScreen(navController = navController)
        }

        composable("upi") {
            UpiPaymentScreen(navController = navController)
        }

        composable("upi_pin") {
            UpiPinScreen(navController = navController)
        }

        composable("payment_success") {
            PaymentSuccessScreen(navController = navController)
        }

        composable("signup") {
            SignupScreen(navController = navController)
        }
    }
}
