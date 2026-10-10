package com.meteo.app.backlog.scheduler

import com.meteo.app.backlog.repository.WeatherRepository
import java.util.Timer
import java.util.TimerTask

/**
 * Gestionnaire de rafraîchissement automatique des données météo.
 *
 * Utilise un Timer Java pour planifier des rafraîchissements périodiques
 * toutes les 5 minutes. Dans la version Android finale, ce mécanisme sera
 * remplacé par WorkManager ou un AlarmManager pour respecter les contraintes
 * de batterie (Doze mode).
 *
 * Usage :
 *   val scheduler = WeatherRefreshScheduler(repository)
 *   scheduler.startAutoRefresh("Paris")   // Démarre le rafraîchissement auto
 *   scheduler.stopAutoRefresh()            // Arrête le rafraîchissement auto
 */
class WeatherRefreshScheduler(private val repository: WeatherRepository) {

    companion object {
        /** Intervalle de rafraîchissement : 5 minutes en millisecondes. */
        const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes

        /** Délai avant le premier rafraîchissement automatique. */
        const val INITIAL_DELAY_MS = 0L  // Immédiat
    }

    /** Timer utilisé pour la planification. */
    private var timer: Timer? = null

    /** Indique si le rafraîchissement automatique est actif. */
    @Volatile
    var isRunning: Boolean = false
        private set

    /** Nom de la ville actuellement surveillée. */
    @Volatile
    var currentCity: String? = null
        private set

    /** Coordonnées GPS actuellement surveillées (alternative à la ville). */
    @Volatile
    var currentLatitude: Double? = null
        private set

    @Volatile
    var currentLongitude: Double? = null
        private set

    /** Callback optionnel appelé après chaque rafraîchissement. */
    var onRefreshComplete: (() -> Unit)? = null

    /** Callback optionnel appelé en cas d'erreur. */
    var onRefreshError: ((String) -> Unit)? = null

    // ──────────────────────────────────────────────
    // Rafraîchissement par nom de ville
    // ──────────────────────────────────────────────

    /**
     * Démarre le rafraîchissement automatique pour une ville donnée.
     * Si un rafraîchissement est déjà en cours, il est d'abord arrêté.
     *
     * @param cityName Nom de la ville (ex: "Paris", "Lyon")
     */
    fun startAutoRefresh(cityName: String) {
        stopAutoRefresh()

        currentCity = cityName
        currentLatitude = null
        currentLongitude = null
        isRunning = true

        timer = Timer("WeatherRefreshTimer", true) // Timer daemon
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                performRefreshByCity(cityName)
            }
        }, INITIAL_DELAY_MS, REFRESH_INTERVAL_MS)
    }

    // ──────────────────────────────────────────────
    // Rafraîchissement par coordonnées GPS
    // ──────────────────────────────────────────────

    /**
     * Démarre le rafraîchissement automatique par coordonnées GPS.
     * Si un rafraîchissement est déjà en cours, il est d'abord arrêté.
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     */
    fun startAutoRefreshByCoords(latitude: Double, longitude: Double) {
        stopAutoRefresh()

        currentCity = null
        currentLatitude = latitude
        currentLongitude = longitude
        isRunning = true

        timer = Timer("WeatherRefreshTimer", true)
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                performRefreshByCoords(latitude, longitude)
            }
        }, INITIAL_DELAY_MS, REFRESH_INTERVAL_MS)
    }

    // ──────────────────────────────────────────────
    // Contrôle du rafraîchissement
    // ──────────────────────────────────────────────

    /**
     * Arrête le rafraîchissement automatique.
     */
    fun stopAutoRefresh() {
        timer?.cancel()
        timer?.purge()
        timer = null
        isRunning = false
    }

    /**
     * Force un rafraîchissement immédiat (en dehors du cycle automatique).
     * Utilise la ville ou les coordonnées actuellement configurées.
     */
    fun forceRefresh() {
        val city = currentCity
        val lat = currentLatitude
        val lon = currentLongitude

        when {
            city != null -> performRefreshByCity(city)
            lat != null && lon != null -> performRefreshByCoords(lat, lon)
            else -> onRefreshError?.invoke("Aucune ville ou coordonnées configurées pour le rafraîchissement")
        }
    }

    /**
     * Change la ville surveillée sans interrompre le cycle de rafraîchissement.
     * Relance le timer avec la nouvelle ville.
     *
     * @param cityName Nouveau nom de ville
     */
    fun changeCity(cityName: String) {
        if (isRunning) {
            startAutoRefresh(cityName)
        } else {
            currentCity = cityName
            currentLatitude = null
            currentLongitude = null
        }
    }

    // ──────────────────────────────────────────────
    // Exécution du rafraîchissement
    // ──────────────────────────────────────────────

    /**
     * Effectue le rafraîchissement par nom de ville.
     */
    private fun performRefreshByCity(cityName: String) {
        try {
            repository.refreshWeatherByCity(cityName)

            val error = repository.errorMessage
            if (error != null) {
                onRefreshError?.invoke(error)
            } else {
                onRefreshComplete?.invoke()
            }
        } catch (e: Exception) {
            onRefreshError?.invoke("Erreur lors du rafraîchissement: ${e.message}")
        }
    }

    /**
     * Effectue le rafraîchissement par coordonnées GPS.
     */
    private fun performRefreshByCoords(latitude: Double, longitude: Double) {
        try {
            repository.refreshWeatherByCoords(latitude, longitude)

            val error = repository.errorMessage
            if (error != null) {
                onRefreshError?.invoke(error)
            } else {
                onRefreshComplete?.invoke()
            }
        } catch (e: Exception) {
            onRefreshError?.invoke("Erreur lors du rafraîchissement: ${e.message}")
        }
    }
}
