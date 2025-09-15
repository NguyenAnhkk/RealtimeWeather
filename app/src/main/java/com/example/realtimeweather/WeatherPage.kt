package com.example.realtimeweather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.realtimeweather.api.NetworkResponse
import com.example.realtimeweather.api.WeatherModel
import com.example.realtimeweather.permission.hasLocationPermission
import com.example.realtimeweather.permission.rememberSpeechPermissionState
import com.example.realtimeweather.utils.SpeechRecognizerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WeatherPage(viewModel: WeatherViewModel) {
    var city by remember { mutableStateOf("") }
    val weatherResult = viewModel.weatherResult.observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val speechPermissionState = rememberSpeechPermissionState()
    var isSpeechRecognitionActive by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {

            getLocationAfterPermission(
                context, isLoadingLocation, locationError,
                onLoadingChange = { isLoadingLocation = it },
                onErrorChange = { locationError = it },
                onCityChange = { city = it },
                viewModel = viewModel
            )
        } else {
            isLoadingLocation = false
            locationError = "Location permission denied"
        }
    }
    val speechRecognizerHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onResult = { result ->

                city = result
                viewModel.getData(result)
                isSpeechRecognitionActive = false
            },
            onPartialResult = { partialResult ->
                city = partialResult
            },
            onError = { error ->
                speechPermissionState.permissionGranted.value = false
                isSpeechRecognitionActive = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = error,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
    var micScale by remember { mutableStateOf(1f) }
    LaunchedEffect(isSpeechRecognitionActive) {
        if (isSpeechRecognitionActive) {
            while (isSpeechRecognitionActive) {
                micScale = 1.2f
                delay(500)
                micScale = 1f
                delay(500)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerHelper.destroy()
        }
    }
    LaunchedEffect(Unit) {
        speechPermissionState.checkPermission(context)
    }
    if (speechPermissionState.showPermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { speechPermissionState.showPermissionDialog.value = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("This app needs microphone access to use voice search") },
            confirmButton = {
                Button(
                    onClick = {
                        speechPermissionState.showPermissionDialog.value = false
                        speechPermissionState.requestPermission()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(
                    onClick = { speechPermissionState.showPermissionDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        WeatherSearchBar(
            city = city,
            onCityChange = { city = it },
            onSearch = {
                if (city.isNotBlank()) {
                    viewModel.getData(city)
                    keyboardController?.hide()
                }
            },
            onGetLocation = {
                isLoadingLocation = true
                locationError = null
                if (hasLocationPermission(context)) {
                    getLocationAfterPermission(
                        context, isLoadingLocation, locationError,
                        onLoadingChange = { isLoadingLocation = it },
                        onErrorChange = { locationError = it },
                        onCityChange = { city = it },
                        viewModel = viewModel
                    )
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onMicClick = {
                if (speechPermissionState.permissionGranted.value) {
                    if (isSpeechRecognitionActive) {
                        speechRecognizerHelper.stopListening()
                        isSpeechRecognitionActive = false
                    } else {
                        isSpeechRecognitionActive = true
                        city = ""
                        speechRecognizerHelper.startListening()
                    }
                } else {
                    speechPermissionState.requestPermission()
                }
            },
            isLoadingLocation = isLoadingLocation,
            isSpeechRecognitionActive = isSpeechRecognitionActive,
            micEnabled = speechPermissionState.permissionGranted.value
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (val result = weatherResult.value) {
            is NetworkResponse.Error -> Text(
                text = result.errorMessage,
                color = Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            NetworkResponse.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            is NetworkResponse.Success<*> -> WeatherDetails(result.data as WeatherModel)

            null -> {}
        }
    }

    when (val result = weatherResult.value) {
        is NetworkResponse.Error -> {
            Text(text = result.errorMessage, color = Color.Red)
        }

        NetworkResponse.Loading -> CircularProgressIndicator()
        is NetworkResponse.Success<*> -> {
            WeatherDetails(result.data as WeatherModel)
        }

        null -> {}
    }
}

@Composable
fun WeatherDetails(data: WeatherModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF74ABE2), Color(0xFF5563DE))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "${data.location.name}, ${data.location.country}",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Temperature
        Text(
            text = data.current.temp_c + "°C",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Icon
        AsyncImage(
            model = "https:${data.current.condition.icon}".replace("64x64", "128x128"),
            contentDescription = "Weather Icon",
            modifier = Modifier.size(160.dp)
        )

        // Condition text
        Text(
            text = data.current.condition.text,
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Weather info card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeatherKeyVal("Humidity", "${data.current.humidity}%")
                    WeatherKeyVal("Wind", data.current.wind_kph + " km/h")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeatherKeyVal("UV Index", data.current.uv.toString())
                    WeatherKeyVal("Rain", data.current.precip_mm + " mm")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Last updated: ${data.location.localtime}",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun WeatherKeyVal(key: String, value: String) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(text = key, fontWeight = FontWeight.SemiBold, color = Color.Gray)
    }
}

fun getLocationAfterPermission(
    context: android.content.Context,
    isLoading: Boolean,
    error: String?,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onCityChange: (String) -> Unit,
    viewModel: WeatherViewModel
) {
    getCurrentLocation(
        context = context,
        onLocationReady = { latLon ->
            onLoadingChange(false)
            onCityChange(latLon)
            viewModel.getData(latLon)
        },
        onError = { errorMsg ->
            onLoadingChange(false)
            onErrorChange(errorMsg)
        }
    )
}
@Composable
fun WeatherSearchBar(
    city: String,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    onGetLocation: () -> Unit,
    onMicClick: () -> Unit,
    isLoadingLocation: Boolean,
    isSpeechRecognitionActive: Boolean,
    micEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = { onCityChange(it) },
                placeholder = { Text("Search location...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Location button
            IconButton(onClick = onGetLocation) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Get Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isSpeechRecognitionActive) Color.Red.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                if (isSpeechRecognitionActive) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_mic_24),
                            contentDescription = "Listening...",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.outline_mic_24),
                        contentDescription = "Voice search",
                        tint = if (micEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
