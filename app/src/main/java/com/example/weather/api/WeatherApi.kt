package com.example.weather.api

import com.example.weather.model.OpenWeather
import com.example.weather.util.Constants.Companion.API_KEY
import com.example.weather.util.Constants.Companion.UNITS
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("data/2.5/weather")
    suspend fun getWeatherUSCity(
        @Query("q")
        cityName: String,
        @Query("appid")
        apiKey: String = API_KEY,
        @Query("units")
        units: String = UNITS
    ): OpenWeather
}