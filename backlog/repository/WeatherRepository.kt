package com.meteo.app.backlog.repository

import com.meteo.app.backlog.api.OpenWeatherService
import com.meteo.app.backlog.api.WeatherApiException
import com.meteo.app.backlog.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository central qui orchestre la récupération des données météo,
 * les transforme (agrégation par jour), et les stocke en mémoire.
 *
 * C'est le point d'accès unique aux données météo pour le reste de l'app.
 */
class WeatherRepository(private val weatherService: OpenWeatherService) {

    // ──────────────────────────────────────────────
    // Stockage en mémoire des données météo
    // ──────────────────────────────────────────────

    /** Données météo complètes (actuelle + prévisions). Null tant que non chargées. */
    @Volatile
    var weatherInfo: WeatherInfo = WeatherInfo(
        currentWeather = null,
        dailyForecasts = emptyList(),
        lastUpdateTimestamp = 0L,
        isLoading = false,
        errorMessage = null
    )
        private set

    /** Météo actuelle (raccourci d'accès). */
    val currentWeather: CurrentWeather?
        get() = weatherInfo.currentWeather

    /** Prévisions 5 jours regroupées par jour. */
    val dailyForecasts: List<DailyForecast>
        get() = weatherInfo.dailyForecasts

    /** Dernière mise à jour (timestamp Unix ms). */
    val lastUpdateTimestamp: Long
        get() = weatherInfo.lastUpdateTimestamp

    /** Indique si un chargement est en cours. */
    val isLoading: Boolean
        get() = weatherInfo.isLoading

    /** Message d'erreur éventuel. */
    val errorMessage: String?
        get() = weatherInfo.errorMessage

    // ──────────────────────────────────────────────
    // Rafraîchissement des données
    // ──────────────────────────────────────────────

    /**
     * Rafraîchit toutes les données météo pour une ville donnée.
     * Met à jour les variables internes avec les nouvelles données.
     *
     * @param cityName Nom de la ville (ex: "Paris")
     * @return WeatherInfo mis à jour
     */
    fun refreshWeatherByCity(cityName: String): WeatherInfo {
        return try {
            setLoadingState()

            // 1. Récupérer la météo actuelle
            val current = weatherService.fetchCurrentWeather(cityName)

            // 2. Récupérer les prévisions 5 jours (tranches de 3h)
            val forecastItems = weatherService.fetchForecast(cityName)

            // 3. Agréger les prévisions par jour
            val dailyForecasts = aggregateForecastsByDay(forecastItems)

            // 4. Mettre à jour le stockage
            weatherInfo = WeatherInfo(
                currentWeather = current,
                dailyForecasts = dailyForecasts,
                lastUpdateTimestamp = System.currentTimeMillis(),
                isLoading = false,
                errorMessage = null
            )

            weatherInfo
        } catch (e: WeatherApiException) {
            handleError("Erreur API: ${e.message}")
        } catch (e: Exception) {
            handleError("Erreur inattendue: ${e.message}")
        }
    }

    /**
     * Rafraîchit toutes les données météo par coordonnées GPS.
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     * @return WeatherInfo mis à jour
     */
    fun refreshWeatherByCoords(latitude: Double, longitude: Double): WeatherInfo {
        return try {
            setLoadingState()

            val current = weatherService.fetchCurrentWeatherByCoords(latitude, longitude)
            val forecastItems = weatherService.fetchForecastByCoords(latitude, longitude)
            val dailyForecasts = aggregateForecastsByDay(forecastItems)

            weatherInfo = WeatherInfo(
                currentWeather = current,
                dailyForecasts = dailyForecasts,
                lastUpdateTimestamp = System.currentTimeMillis(),
                isLoading = false,
                errorMessage = null
            )

            weatherInfo
        } catch (e: WeatherApiException) {
            handleError("Erreur API: ${e.message}")
        } catch (e: Exception) {
            handleError("Erreur inattendue: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────
    // Agrégation des prévisions par jour
    // ──────────────────────────────────────────────

    /**
     * Regroupe les prévisions 3h en prévisions quotidiennes.
     * Calcule les min/max, moyennes, et la condition dominante pour chaque jour.
     *
     * @param items Liste des ForecastItem (créneaux de 3h)
     * @return Liste de DailyForecast (un par jour, max 5)
     */
    private fun aggregateForecastsByDay(items: List<ForecastItem>): List<DailyForecast> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Regrouper par date
        val groupedByDay = items.groupBy { item ->
            dateFormat.format(Date(item.timestamp * 1000))
        }

        return groupedByDay.map { (date, dayItems) ->
            DailyForecast(
                date = date,
                temperatureMin = dayItems.minOf { it.temperatureMin },
                temperatureMax = dayItems.maxOf { it.temperatureMax },
                dominantCondition = findDominantCondition(dayItems),
                dominantDescription = findDominantDescription(dayItems),
                dominantIconCode = findDominantIconCode(dayItems),
                averageHumidity = dayItems.map { it.humidity }.average().toInt(),
                averageWindSpeed = dayItems.map { it.windSpeed }.average(),
                maxPrecipitationProbability = dayItems.maxOf { it.precipitationProbability },
                hourlyForecasts = dayItems
            )
        }.take(5) // Limiter à 5 jours
    }

    /**
     * Trouve la condition météo la plus fréquente dans une liste de prévisions.
     */
    private fun findDominantCondition(items: List<ForecastItem>): String {
        return items.groupingBy { it.weatherCondition }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "Unknown"
    }

    /**
     * Trouve la description météo la plus fréquente.
     */
    private fun findDominantDescription(items: List<ForecastItem>): String {
        return items.groupingBy { it.weatherDescription }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "Inconnu"
    }

    /**
     * Trouve le code d'icône le plus fréquent (en privilégiant les icônes de jour "d").
     */
    private fun findDominantIconCode(items: List<ForecastItem>): String {
        // Privilégier les icônes de jour pour la représentation quotidienne
        val dayIcons = items.filter { it.weatherIconCode.endsWith("d") }
        val source = if (dayIcons.isNotEmpty()) dayIcons else items

        return source.groupingBy { it.weatherIconCode }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "01d"
    }

    // ──────────────────────────────────────────────
    // Helpers internes
    // ──────────────────────────────────────────────

    /** Met l'état en chargement. */
    private fun setLoadingState() {
        weatherInfo = weatherInfo.copy(isLoading = true, errorMessage = null)
    }

    /** Gère une erreur en mettant à jour l'état. */
    private fun handleError(message: String): WeatherInfo {
        weatherInfo = weatherInfo.copy(
            isLoading = false,
            errorMessage = message
        )
        return weatherInfo
    }
}
