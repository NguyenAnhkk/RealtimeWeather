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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.realtimeweather.R
import com.example.realtimeweather.WeatherViewModel
import com.example.realtimeweather.getCurrentLocation
import com.example.realtimeweather.permission.hasLocationPermission
import com.example.realtimeweather.permission.rememberSpeechPermissionState
import com.example.realtimeweather.utils.SpeechRecognizerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherPage(
    viewModel: WeatherViewModel,
    navController: NavController,
    onSearchComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var city by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                viewModel = viewModel,
                onComplete = onSearchComplete
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
                onSearchComplete()
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

    LaunchedEffect(locationError) {
        locationError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            locationError = null
        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search Weather") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            WeatherSearchBar(
                city = city,
                onCityChange = { city = it },
                onSearch = {
                    if (city.isNotBlank()) {
                        viewModel.getData(city)
                        keyboardController?.hide()
                        onSearchComplete()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Please enter a location",
                                duration = SnackbarDuration.Short
                            )
                        }
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
                            viewModel = viewModel,
                            onComplete = onSearchComplete
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
                micEnabled = speechPermissionState.permissionGranted.value,
                micScale = micScale
            )
        }
    }
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
    micEnabled: Boolean,
    micScale: Float = 1f
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = { onCityChange(it) },
                placeholder = { Text("Enter city name...") },
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

            Spacer(modifier = Modifier.width(12.dp))

            // Location button
            IconButton(
                onClick = onGetLocation,
                modifier = Modifier.size(48.dp)
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
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

            Spacer(modifier = Modifier.width(8.dp))

            // Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(48.dp)
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
                            .size(32.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_mic_24),
                            contentDescription = "Listening...",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.outline_mic_24),
                        contentDescription = "Voice search",
                        tint = if (micEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

fun getLocationAfterPermission(
    context: android.content.Context,
    isLoading: Boolean,
    error: String?,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onCityChange: (String) -> Unit,
    viewModel: WeatherViewModel,
    onComplete: () -> Unit
) {
    getCurrentLocation(
        context = context,
        onLocationReady = { latLon ->
            onLoadingChange(false)
            onCityChange(latLon)
            viewModel.getData(latLon)
            onComplete()
        },
        onError = { errorMsg ->
            onLoadingChange(false)
            onErrorChange(errorMsg)
        }
    )
}