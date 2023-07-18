@file:OptIn(ExperimentalCoroutinesApi::class)

import android.app.Application
import android.content.SharedPreferences
import com.example.weather.api.WeatherApi
import com.example.weather.model.Coord
import com.example.weather.model.Main
import com.example.weather.model.OpenWeather
import com.example.weather.model.Sys
import com.example.weather.model.Weather
import com.example.weather.viewmodel.WeatherUIState
import com.example.weather.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

//Given more time i would test the unhappy paths and the error paths aswell
//make sure i get full test coverage

class WeatherViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockWeatherApi: WeatherApi

    @Mock
    private lateinit var mockSharedPref: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var viewModel: WeatherViewModel
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        closeable = MockitoAnnotations.openMocks(this)
        viewModel = WeatherViewModel(mockWeatherApi,mockApplication)
        Mockito.`when`(mockApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPref)
    }

    @After
    fun closeMocks() {
        closeable.close()
        testDispatcher.cleanupTestCoroutines()
    }


    @Test
    fun onSearchTextChanged() = runBlocking {
        val newText = "New Text"

        viewModel.onSearchTextChanged(newText)

        assertEquals(newText, viewModel.searchText.value)
    }

    @Test
    fun getWeatherData() = runBlocking {
        val testCity = "Test City"
        val testWeather = OpenWeather(
            coord = Coord(73.85, 18.52),
            id = 12345,
            main = Main(0, 37, 1.1, 302.15, 304.15),
            name = "Test City",
            sys = Sys("US", 1, 0.0, 1625312072, 1625357645, 1625401179),
            weather = listOf(
                Weather(
                    id = 800,
                    main = "Clear",
                    description = "clear sky",
                    icon = "01d"
                )
            )
        )

        Mockito.`when`(mockWeatherApi.getWeatherUSCity(testCity)).thenReturn(testWeather)

        viewModel.getWeatherData(testCity)

        val uiState = viewModel.openWeather.value
        if (uiState is WeatherUIState.Success) {
            assertEquals(testCity, uiState.weather.name)
        } else {
            assert(false) { "Expected WeatherUIState.Success" }
        }
    }

    @Test
    fun saveLastSearchedCity() = runBlocking {
        val testCity = "Test City"
        Mockito.`when`(mockSharedPref.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPref)
        viewModel.saveLastSearchedCity(testCity)
        verify(mockEditor).putString("lastSearchedCity", testCity)
        verify(mockEditor).apply()
    }

    @Test
    fun getLastSearchedCity() = runBlocking {
        val testCity = "Test City"
        Mockito.`when`(mockSharedPref.getString(anyString(), anyString())).thenReturn(testCity)
        val city = viewModel.getLastSearchedCity()
        assertEquals(testCity, city)
    }
}
