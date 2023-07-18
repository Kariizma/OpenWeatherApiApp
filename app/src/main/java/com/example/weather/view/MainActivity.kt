package com.example.weather.view

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.weather.model.OpenWeather
import com.example.weather.ui.theme.WeatherTheme
import com.example.weather.viewmodel.WeatherUIState
import com.example.weather.viewmodel.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val viewModel: WeatherViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        initLocationServices()
        super.onCreate(savedInstanceState)
        setContent {
            WeatherTheme {
                setupPermissions()
                handleUIStates()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
            }
        }

        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, locationCallback, null)
    }


    @Composable
    fun handleUIStates() {
        val uiState by viewModel.openWeather.collectAsStateWithLifecycle()
        when (uiState) {
            is WeatherUIState.Success -> {
                Log.v("OnCreate: ", "WeatherUIState: Success")
                val weather = (uiState as WeatherUIState.Success).weather
                MainScreen(viewModel = viewModel, weather)
            }

            WeatherUIState.Loading -> {
                Log.e("OnCreate: ", "WeatherUIState: Loading")
                LoadingAnimation()
            }

            is WeatherUIState.Error -> {
                val error = (uiState as WeatherUIState.Error).exception
                Log.e("OnCreate: ", "WeatherUIState Error")
            }
        }
    }

    @Composable
    @SuppressLint("MissingPermission")
    fun setupPermissions() {
        val locationPermissionResultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        val geocoder = Geocoder(this)
                        val currentLocation =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        Log.e("Location Coords:", currentLocation!!.first().locality)
                        viewModel.getLocationUpdate(currentLocation!!.first().locality)
                        viewModel.getWeatherData(currentLocation!!.first().locality)
                    }
                } else {
                    val lastSearchedCity = viewModel.getLastSearchedCity()
                    viewModel.getWeatherData(lastSearchedCity)
                }
            }
        )

        LaunchedEffect(key1 = viewModel.requestPermissionOnce) {
            if (!viewModel.requestPermissionOnce) {
                locationPermissionResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                viewModel.requestPermissionOnce = true
            }
        }
    }



    @OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
    @Composable
    fun MainScreen(viewModel: WeatherViewModel, weather: OpenWeather) {
        val text by viewModel.searchText.collectAsState()
        var active by remember { mutableStateOf(false) }
        var mutableList: MutableList<String> = mutableListOf()
        var prevItems = remember {
            mutableList
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                query = text,
                onQueryChange = viewModel::onSearchTextChanged,
                onSearch = {
                    if (prevItems.size > 14)
                        prevItems.clear()
                    prevItems.add(text)
                    viewModel.searchWeather(text)
                    active = false
                },
                active = active,
                onActiveChange = {
                    active = it
                },
                placeholder = {
                    Text(text = "Search")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (active) {
                        Icon(
                            modifier = Modifier.clickable {
                                if (text.isNotEmpty()) {
                                    viewModel.emptyText()
                                } else {
                                    active = false
                                }
                            },
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Icon"
                        )
                    }
                }
            ) {
                prevItems.forEach {
                    Row(modifier = Modifier
                        .padding(all = 14.dp)
                        .clickable { viewModel.onSearchTextChanged(it) }
                        .fillMaxWidth()) {
                        Icon(
                            modifier = Modifier.padding(end = 10.dp),
                            imageVector = Icons.Default.History,
                            contentDescription = "Search History Icon"
                        )
                        Text(it)
                    }
                }
            }

            val iconCode = weather.weather.first().icon
            val iconUrl = "http://openweathermap.org/img/wn/$iconCode@2x.png"
            val imagePainter = rememberImagePainter(
                data = iconUrl,
                builder = {
                    crossfade(true)
                    listener(
                        onError = { _, throwable ->
                            Log.e("ImageLoading", "Image loading failed", throwable)
                        }
                    )
                }
            )

            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Text(text = weather.name)

            Text(text = weather.weather.first().main)

            Text(text = weather.main.temp.toString())
        }
    }

    @Composable
    fun LoadingScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
        {
            LoadingAnimation(Modifier.fillMaxSize())
        }
    }

    @Composable
    fun LoadingAnimation(
        modifier: Modifier = Modifier,
        circleSize: Dp = 25.dp,
        circleColor: Color = MaterialTheme.colors.primary,
        spaceBetween: Dp = 10.dp,
        travelDistance: Dp = 20.dp
    ) {
        val circles = listOf(
            remember { Animatable(initialValue = 0f) },
            remember { Animatable(initialValue = 0f) },
            remember { Animatable(initialValue = 0f) }
        )

        circles.forEachIndexed { index, animatable ->
            LaunchedEffect(key1 = animatable) {
                delay(index * 100L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0.0f at 0 with LinearOutSlowInEasing
                            1.0f at 300 with LinearOutSlowInEasing
                            0.0f at 600 with LinearOutSlowInEasing
                            0.0f at 1200 with LinearOutSlowInEasing
                        },
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }

        val circleValues = circles.map { it.value }
        val distance = with(LocalDensity.current) { travelDistance.toPx() }

        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spaceBetween)
            ) {
                circleValues.forEach { value ->
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .graphicsLayer {
                                translationY = -value * distance
                            }
                            .background(
                                color = circleColor,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

