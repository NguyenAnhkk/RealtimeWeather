package com.example.realtimeweather

import WeatherPage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.realtimeweather.screen.HomeScreen
import com.example.realtimeweather.ui.theme.RealtimeWeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherApp()
        }
    }
}

@Composable
fun WeatherApp() {
    val navController = rememberNavController()
    val viewModel: WeatherViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val weatherResult by viewModel.weatherResult.collectAsState()

            HomeScreen(
                weatherResult = weatherResult,
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                navController = navController
            )
        }
        composable("search") {
            WeatherPage(
                viewModel = viewModel,
                navController = navController,
                onSearchComplete = {
                    // Chỉ quay lại, không xóa data
                    navController.popBackStack()
                }
            )
        }
    }
}