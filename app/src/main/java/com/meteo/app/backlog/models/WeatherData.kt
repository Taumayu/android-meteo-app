package com.meteo.app.backlog.models

/**
 * Représente les données météo actuelles pour une ville donnée.
 */
data class CurrentWeather(
    val cityName: String,
    val country: String,
    val timestamp: Long,              // Unix timestamp de la mesure
    val temperature: Double,           // Température actuelle en °C
    val feelsLike: Double,             // Température ressentie en °C
    val temperatureMin: Double,        // Température minimale en °C
    val temperatureMax: Double,        // Température maximale en °C
    val pressure: Int,                 // Pression atmosphérique en hPa
    val humidity: Int,                 // Humidité en %
    val windSpeed: Double,             // Vitesse du vent en m/s
    val windDirection: Int,            // Direction du vent en degrés
    val cloudiness: Int,               // Couverture nuageuse en %
    val weatherCondition: String,      // Condition principale (ex: "Rain", "Clear")
    val weatherDescription: String,    // Description détaillée (ex: "légère pluie")
    val weatherIconCode: String,       // Code de l'icône météo OpenWeather
    val visibility: Int,               // Visibilité en mètres
    val sunrise: Long,                 // Heure du lever du soleil (Unix timestamp)
    val sunset: Long                   // Heure du coucher du soleil (Unix timestamp)
)

/**
 * Représente les prévisions météo pour un créneau horaire donné
 * (utilisé dans les prévisions sur 5 jours, découpées en tranches de 3h).
 */
data class ForecastItem(
    val timestamp: Long,               // Unix timestamp du créneau
    val temperature: Double,           // Température en °C
    val feelsLike: Double,             // Température ressentie en °C
    val temperatureMin: Double,        // Température minimale en °C
    val temperatureMax: Double,        // Température maximale en °C
    val pressure: Int,                 // Pression atmosphérique en hPa
    val humidity: Int,                 // Humidité en %
    val windSpeed: Double,             // Vitesse du vent en m/s
    val windDirection: Int,            // Direction du vent en degrés
    val cloudiness: Int,               // Couverture nuageuse en %
    val weatherCondition: String,      // Condition principale
    val weatherDescription: String,    // Description détaillée
    val weatherIconCode: String,       // Code de l'icône météo
    val precipitationProbability: Double, // Probabilité de précipitation (0 à 1)
    val rainVolume3h: Double?,         // Volume de pluie sur 3h en mm (nullable)
    val snowVolume3h: Double?          // Volume de neige sur 3h en mm (nullable)
)

/**
 * Représente les prévisions regroupées par jour.
 */
data class DailyForecast(
    val date: String,                  // Date au format "yyyy-MM-dd"
    val temperatureMin: Double,        // Température minimale du jour en °C
    val temperatureMax: Double,        // Température maximale du jour en °C
    val dominantCondition: String,     // Condition météo dominante du jour
    val dominantDescription: String,   // Description dominante du jour
    val dominantIconCode: String,      // Icône dominante du jour
    val averageHumidity: Int,          // Humidité moyenne du jour en %
    val averageWindSpeed: Double,      // Vitesse moyenne du vent en m/s
    val maxPrecipitationProbability: Double, // Probabilité max de précipitation
    val hourlyForecasts: List<ForecastItem>  // Détail des prévisions horaires (3h)
)

/**
 * Conteneur principal regroupant toutes les données météo.
 * C'est l'objet principal stocké en mémoire et rafraîchi toutes les 5 minutes.
 */
data class WeatherInfo(
    val currentWeather: CurrentWeather?,        // Météo actuelle (null si pas encore chargée)
    val dailyForecasts: List<DailyForecast>,    // Prévisions sur 5 jours
    val lastUpdateTimestamp: Long,               // Dernier rafraîchissement (Unix ms)
    val isLoading: Boolean,                      // Indicateur de chargement en cours
    val errorMessage: String?                    // Message d'erreur éventuel (null si OK)
)
