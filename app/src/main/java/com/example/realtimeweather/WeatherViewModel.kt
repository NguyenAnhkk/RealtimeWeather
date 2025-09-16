package com.example.realtimeweather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimeweather.api.Constant
import com.example.realtimeweather.api.NetworkResponse
import com.example.realtimeweather.api.RetrofitInstance
import com.example.realtimeweather.api.WeatherModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class WeatherViewModel : ViewModel() {
    private val weatherApi = RetrofitInstance.weatherApi
    private val _weatherResult = MutableStateFlow<NetworkResponse<WeatherModel>?>(null)
    val weatherResult: StateFlow<NetworkResponse<WeatherModel>?> = _weatherResult.asStateFlow()

    fun getData(city: String) {
        _weatherResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApi.getWeather(Constant.apiKey, city)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _weatherResult.value = NetworkResponse.Success(it)
                        Log.d("WeatherViewModel", "Data loaded for: $city")
                    }
                } else {
                    _weatherResult.value = NetworkResponse.Error("Failed to load data: ${response.code()}")
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error("Error: ${e.message}")
            }
        }
    }

}