package com.example.weather.model

data class OpenWeather(
    val coord: Coord = Coord(0.0,0.0),
    val id: Int = 0,
    val main: Main = Main(0,0,0.0,0.0,0.0),
    val name: String = "name",
    val sys: Sys = Sys("US",0, 0.0,0,0,0),
    val weather: List<Weather> = listOf<Weather>(),
)