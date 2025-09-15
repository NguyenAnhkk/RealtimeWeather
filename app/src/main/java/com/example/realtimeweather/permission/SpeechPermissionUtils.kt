package com.example.realtimeweather.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

private const val TAG = "SpeechPermissionState"

@Composable
fun rememberSpeechPermissionState(): SpeechPermissionState {
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionDialog = remember { mutableStateOf(false) }
    val shouldRequestPermission = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
        if (!isGranted) {
            showPermissionDialog.value = true
        }
        shouldRequestPermission.value = false
        Log.d(TAG, "Permission request result: $isGranted")
    }

    return remember {
        SpeechPermissionState(
            permissionGranted = permissionGranted,
            showPermissionDialog = showPermissionDialog,
            shouldRequestPermission = shouldRequestPermission,
            permissionLauncher = permissionLauncher
        )
    }
}

class SpeechPermissionState(
    val permissionGranted: MutableState<Boolean>,
    val showPermissionDialog: MutableState<Boolean>,
    val shouldRequestPermission: MutableState<Boolean>,
    val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun checkPermission(context: Context): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        permissionGranted.value = hasPermission
        Log.d(TAG, "Permission check result: $hasPermission")
        return hasPermission
    }

    fun requestPermission() {
        if (!shouldRequestPermission.value) {
            shouldRequestPermission.value = true
            Log.d(TAG, "Requesting microphone permission")
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun dismissDialog() {
        showPermissionDialog.value = false
    }
}