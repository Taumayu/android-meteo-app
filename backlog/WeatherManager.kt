package com.meteo.app.backlog

import com.meteo.app.backlog.api.OpenWeatherService
import com.meteo.app.backlog.repository.WeatherRepository
import com.meteo.app.backlog.scheduler.WeatherRefreshScheduler

/**
 * Point d'entrée principal du backlog météo.
 *
 * Initialise et connecte tous les composants :
 *   API Service → Repository → Scheduler
 *
 * Usage typique :
 *   val manager = WeatherManager("VOTRE_CLE_API")
 *   manager.start("Paris")
 *
 *   // Accéder aux données stockées :
 *   val current = manager.repository.currentWeather
 *   val forecasts = manager.repository.dailyForecasts
 *
 *   // Changer de ville :
 *   manager.changeCity("Lyon")
 *
 *   // Arrêter :
 *   manager.stop()
 */
class WeatherManager(apiKey: String) {

    /** Service d'accès à l'API OpenWeatherMap. */
    val weatherService = OpenWeatherService(apiKey)

    /** Repository de données météo (stockage en mémoire). */
    val repository = WeatherRepository(weatherService)

    /** Scheduler de rafraîchissement automatique (toutes les 5 min). */
    val scheduler = WeatherRefreshScheduler(repository)

    /**
     * Démarre le système météo pour une ville donnée.
     * Lance un premier chargement immédiat puis un rafraîchissement
     * automatique toutes les 5 minutes.
     *
     * @param cityName Nom de la ville (ex: "Paris")
     */
    fun start(cityName: String) {
        scheduler.startAutoRefresh(cityName)
    }

    /**
     * Démarre le système météo par coordonnées GPS.
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     */
    fun startByCoords(latitude: Double, longitude: Double) {
        scheduler.startAutoRefreshByCoords(latitude, longitude)
    }

    /**
     * Change la ville surveillée (relance le cycle de rafraîchissement).
     *
     * @param cityName Nouveau nom de ville
     */
    fun changeCity(cityName: String) {
        scheduler.changeCity(cityName)
    }

    /**
     * Force un rafraîchissement immédiat des données.
     */
    fun forceRefresh() {
        scheduler.forceRefresh()
    }

    /**
     * Arrête complètement le système (scheduler + nettoyage).
     */
    fun stop() {
        scheduler.stopAutoRefresh()
    }

    /**
     * Définit un callback appelé après chaque rafraîchissement réussi.
     */
    fun onRefreshComplete(callback: () -> Unit) {
        scheduler.onRefreshComplete = callback
    }

    /**
     * Définit un callback appelé en cas d'erreur de rafraîchissement.
     */
    fun onRefreshError(callback: (String) -> Unit) {
        scheduler.onRefreshError = callback
    }
}
