package com.meteo.app.backlog.api

import com.meteo.app.backlog.models.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service responsable des appels réseau vers l'API OpenWeatherMap.
 *
 * Endpoints utilisés :
 * - /weather   : météo actuelle
 * - /forecast  : prévisions 5 jours / 3 heures
 *
 * Les unités sont en métrique (°C, m/s).
 * La langue des descriptions est le français.
 */
class OpenWeatherService(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5"
        private const val UNITS = "metric"
        private const val LANG = "fr"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    // ──────────────────────────────────────────────
    // Météo actuelle
    // ──────────────────────────────────────────────

    /**
     * Récupère la météo actuelle pour une ville donnée.
     *
     * @param cityName Nom de la ville (ex: "Paris")
     * @return CurrentWeather ou null en cas d'erreur
     * @throws WeatherApiException en cas d'erreur réseau ou API
     */
    fun fetchCurrentWeather(cityName: String): CurrentWeather {
        val url = "$BASE_URL/weather?q=$cityName&appid=$apiKey&units=$UNITS&lang=$LANG"
        val json = executeHttpGet(url)
        return parseCurrentWeather(json)
    }

    /**
     * Récupère la météo actuelle par coordonnées GPS.
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     * @return CurrentWeather
     * @throws WeatherApiException en cas d'erreur réseau ou API
     */
    fun fetchCurrentWeatherByCoords(latitude: Double, longitude: Double): CurrentWeather {
        val url = "$BASE_URL/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=$UNITS&lang=$LANG"
        val json = executeHttpGet(url)
        return parseCurrentWeather(json)
    }

    // ──────────────────────────────────────────────
    // Prévisions 5 jours
    // ──────────────────────────────────────────────

    /**
     * Récupère les prévisions sur 5 jours (par tranches de 3h) pour une ville.
     *
     * @param cityName Nom de la ville
     * @return Liste de ForecastItem (jusqu'à 40 créneaux)
     * @throws WeatherApiException en cas d'erreur réseau ou API
     */
    fun fetchForecast(cityName: String): List<ForecastItem> {
        val url = "$BASE_URL/forecast?q=$cityName&appid=$apiKey&units=$UNITS&lang=$LANG"
        val json = executeHttpGet(url)
        return parseForecastList(json)
    }

    /**
     * Récupère les prévisions sur 5 jours par coordonnées GPS.
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     * @return Liste de ForecastItem
     * @throws WeatherApiException en cas d'erreur réseau ou API
     */
    fun fetchForecastByCoords(latitude: Double, longitude: Double): List<ForecastItem> {
        val url = "$BASE_URL/forecast?lat=$latitude&lon=$longitude&appid=$apiKey&units=$UNITS&lang=$LANG"
        val json = executeHttpGet(url)
        return parseForecastList(json)
    }

    // ──────────────────────────────────────────────
    // Requête HTTP GET
    // ──────────────────────────────────────────────

    /**
     * Exécute une requête HTTP GET et retourne le JSON de la réponse.
     *
     * @param urlString URL complète de la requête
     * @return JSONObject de la réponse
     * @throws WeatherApiException si le code HTTP n'est pas 200 ou si erreur réseau
     */
    private fun executeHttpGet(urlString: String): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Pas de corps d'erreur"
                } catch (e: Exception) {
                    "Impossible de lire l'erreur"
                }
                throw WeatherApiException(
                    "Erreur API OpenWeather (code $responseCode): $errorBody",
                    responseCode
                )
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            return JSONObject(response)
        } catch (e: WeatherApiException) {
            throw e
        } catch (e: Exception) {
            throw WeatherApiException("Erreur réseau: ${e.message}", -1, e)
        } finally {
            connection?.disconnect()
        }
    }

    // ──────────────────────────────────────────────
    // Parsing JSON → Modèles
    // ──────────────────────────────────────────────

    /**
     * Parse la réponse JSON de l'endpoint /weather en CurrentWeather.
     */
    private fun parseCurrentWeather(json: JSONObject): CurrentWeather {
        val main = json.getJSONObject("main")
        val wind = json.getJSONObject("wind")
        val clouds = json.getJSONObject("clouds")
        val sys = json.getJSONObject("sys")
        val weatherArray = json.getJSONArray("weather")
        val weather = weatherArray.getJSONObject(0)

        return CurrentWeather(
            cityName = json.getString("name"),
            country = sys.getString("country"),
            timestamp = json.getLong("dt"),
            temperature = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            temperatureMin = main.getDouble("temp_min"),
            temperatureMax = main.getDouble("temp_max"),
            pressure = main.getInt("pressure"),
            humidity = main.getInt("humidity"),
            windSpeed = wind.getDouble("speed"),
            windDirection = wind.optInt("deg", 0),
            cloudiness = clouds.getInt("all"),
            weatherCondition = weather.getString("main"),
            weatherDescription = weather.getString("description"),
            weatherIconCode = weather.getString("icon"),
            visibility = json.optInt("visibility", 0),
            sunrise = sys.getLong("sunrise"),
            sunset = sys.getLong("sunset")
        )
    }

    /**
     * Parse la réponse JSON de l'endpoint /forecast en liste de ForecastItem.
     */
    private fun parseForecastList(json: JSONObject): List<ForecastItem> {
        val list = json.getJSONArray("list")
        val items = mutableListOf<ForecastItem>()

        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            items.add(parseForecastItem(item))
        }

        return items
    }

    /**
     * Parse un objet JSON individuel du tableau "list" en ForecastItem.
     */
    private fun parseForecastItem(json: JSONObject): ForecastItem {
        val main = json.getJSONObject("main")
        val wind = json.getJSONObject("wind")
        val clouds = json.getJSONObject("clouds")
        val weatherArray = json.getJSONArray("weather")
        val weather = weatherArray.getJSONObject(0)

        return ForecastItem(
            timestamp = json.getLong("dt"),
            temperature = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            temperatureMin = main.getDouble("temp_min"),
            temperatureMax = main.getDouble("temp_max"),
            pressure = main.getInt("pressure"),
            humidity = main.getInt("humidity"),
            windSpeed = wind.getDouble("speed"),
            windDirection = wind.optInt("deg", 0),
            cloudiness = clouds.getInt("all"),
            weatherCondition = weather.getString("main"),
            weatherDescription = weather.getString("description"),
            weatherIconCode = weather.getString("icon"),
            precipitationProbability = json.optDouble("pop", 0.0),
            rainVolume3h = json.optJSONObject("rain")?.optDouble("3h"),
            snowVolume3h = json.optJSONObject("snow")?.optDouble("3h")
        )
    }
}

/**
 * Exception spécifique aux erreurs de l'API OpenWeather.
 */
class WeatherApiException(
    message: String,
    val httpCode: Int,
    cause: Throwable? = null
) : Exception(message, cause)
