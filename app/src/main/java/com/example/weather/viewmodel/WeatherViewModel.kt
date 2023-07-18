package com.example.weather.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.api.WeatherApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel
@Inject constructor(private val weatherApi: WeatherApi, application: Application) :
    AndroidViewModel(application) {

    //note: you are unable to use stateflows in Java projects. i would use LiveData
    //if working on a mix of Java/Kotlin project (also wanted to learn stateflows)

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _openWeather = MutableStateFlow<WeatherUIState>(WeatherUIState.Loading)
    val openWeather: StateFlow<WeatherUIState> = _openWeather

    var requestPermissionOnce = false

    init {
        getWeatherData("plano")
    }


    fun getWeatherData(cityName: String, stateName: String? = null) {
        viewModelScope.launch {
            try {
                val location = buildString {
                    append(cityName)
                    if (!stateName.isNullOrEmpty()) {
                        append(",")
                        append(stateName)
                    }
                }
                val weather =
                    WeatherUIState.Success(weatherApi.getWeatherUSCity(location))
                _openWeather.value = WeatherUIState.Success(
                    weather = weather.weather
                )
            } catch (e: Exception) {
                _openWeather.value = WeatherUIState.Error(e)
            }
        }
    }

    fun saveLastSearchedCity(city: String) {
        val sharedPref =
            getApplication<Application>().getSharedPreferences("weather", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("lastSearchedCity", city)
            apply()
        }
    }

    fun getLastSearchedCity(): String {
        val sharedPref =
            getApplication<Application>().getSharedPreferences("weather", Context.MODE_PRIVATE)
        return sharedPref.getString("lastSearchedCity", "plano") ?: "plano"
    }


    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    fun emptyText() {
        _searchText.value = ""
    }

    fun getLocationUpdate(location: String) {
        _searchText.value = location
    }

    fun searchWeather(text: String) {
        val locationSplit = text.split(',')
        if (locationSplit.size == 2) {
            getWeatherData(locationSplit[0], locationSplit[1])
            saveLastSearchedCity(locationSplit[0])
        } else if (locationSplit.size == 1) {
            getWeatherData(locationSplit[0])
            saveLastSearchedCity(locationSplit[0])
        }
    }
}