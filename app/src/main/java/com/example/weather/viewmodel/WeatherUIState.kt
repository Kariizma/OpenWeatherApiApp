package com.example.weather.viewmodel

import com.example.weather.model.OpenWeather

sealed class WeatherUIState {
    class Success(val weather: OpenWeather): WeatherUIState()
    class Error(val exception: Exception): WeatherUIState()
    object Loading: WeatherUIState()
}