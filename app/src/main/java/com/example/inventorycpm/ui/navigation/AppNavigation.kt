package com.example.inventorycpm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.inventorycpm.ui.screen.*
import java.time.LocalDate

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME
    ) {
        // Home
        composable(AppDestinations.HOME) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(AppDestinations.SCAN) },
                onNavigateToFactura = { id -> navController.navigate(AppDestinations.facturaReview(id)) },
                onNavigateToCierre = { navController.navigate(AppDestinations.CIERRE_DIA) },
                onNavigateToHistorial = { navController.navigate(AppDestinations.HISTORIAL) }
            )
        }

        // Scan
        composable(AppDestinations.SCAN) {
            ScanScreen(
                onNavigateToReview = { id ->
                    // Navegar a revisión y quitar Scan del backstack para que "Atrás" vaya a Home
                    navController.navigate(AppDestinations.facturaReview(id)) {
                        popUpTo(AppDestinations.SCAN) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Revisión de Factura (Edición)
        composable(
            route = AppDestinations.FACTURA_REVIEW,
            arguments = listOf(navArgument("facturaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val facturaId = backStackEntry.arguments?.getLong("facturaId") ?: return@composable
            FacturaReviewScreen(
                facturaId = facturaId,
                onNavigateBack = { navController.popBackStack() },
                onConfirmado = { navController.popBackStack(AppDestinations.HOME, false) }
            )
        }

        // Cierre del día
        composable(AppDestinations.CIERRE_DIA) {
            CierreDiaScreen(
                onNavigateBack = { navController.popBackStack() },
                onCierreGuardado = { navController.popBackStack() }
            )
        }

        // Historial General
        composable(AppDestinations.HISTORIAL) {
            HistorialScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDia = { fechaStr ->
                    navController.navigate(AppDestinations.historialDia(fechaStr))
                }
            )
        }

        // Historial de un Día Específico
        composable(
            route = AppDestinations.HISTORIAL_DIA,
            arguments = listOf(navArgument("fecha") { type = NavType.StringType })
        ) { backStackEntry ->
            val fechaStr = backStackEntry.arguments?.getString("fecha") ?: return@composable
            val fecha = LocalDate.parse(fechaStr)
            HistorialDiaScreen(
                fecha = fecha,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetalle = { id ->
                    navController.navigate(AppDestinations.facturaDetalle(id))
                }
            )
        }

        // Detalle de Factura (Read-only / Auditoría)
        composable(
            route = AppDestinations.FACTURA_DETALLE,
            arguments = listOf(navArgument("facturaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val facturaId = backStackEntry.arguments?.getLong("facturaId") ?: return@composable
            FacturaDetalleScreen(
                facturaId = facturaId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
