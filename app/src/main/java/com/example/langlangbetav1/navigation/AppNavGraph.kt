package com.example.langlangbetav1.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.langlangbetav1.scene.SceneScreen
import com.example.langlangbetav1.score.ScoreScreen
import com.example.langlangbetav1.splash.SplashScreen
import com.example.langlangbetav1.upi.PaymentSuccessScreen
import com.example.langlangbetav1.upi.SignupScreen
import com.example.langlangbetav1.upi.UpiPaymentScreen
import com.example.langlangbetav1.upi.UpiPinScreen

/**
 * App nav graph.
 *
 * Scene route: "scene/{moduleId}"
 *   moduleId is the base-name of the JSON file in assets, e.g.:
 *     "module_0"  →  assets/module_0.json
 *     "module_1"  →  assets/module_1.json   (add when ready)
 *
 * To launch a module from any button/screen:
 *     navController.navigate("scene/module_1")
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = "splash",
    ) {

        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("scene/module_0") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // moduleId is a plain String — no NavType annotation needed
        composable("scene/{moduleId}") { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: "module_0"
            SceneScreen(moduleId = moduleId, navController = navController)
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
