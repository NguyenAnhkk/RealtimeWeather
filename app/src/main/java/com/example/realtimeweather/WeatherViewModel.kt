package com.example.realtimeweather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimeweather.api.Constant
import com.example.realtimeweather.api.RetrofitInstance
import kotlinx.coroutines.launch

class WeatherViewModel(activity: MainActivity) : ViewModel() {

    private val weatherApi = RetrofitInstance.weatherApi
    fun getData(city: String) {
        viewModelScope.launch {
           val response =  weatherApi.getWeather(Constant.apiKey, city)
            if (response.isSuccessful){
                Log.i("Response", response.body().toString())
            } else{
                Log.e("Response", "Error: ${response.code()}")
            }
        }
    }
}