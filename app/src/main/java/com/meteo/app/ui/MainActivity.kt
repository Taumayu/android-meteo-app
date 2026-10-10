package com.meteo.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meteo.app.R
import com.meteo.app.backlog.WeatherManager
import com.meteo.app.backlog.models.WeatherInfo
import com.meteo.app.ui.adapters.DailyForecastAdapter
import com.meteo.app.ui.adapters.HourlyForecastAdapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity principale de l'app météo.
 *
 * Layout en deux colonnes :
 * - Gauche : dernière MAJ (top left) + météo actuelle + reste de la journée
 * - Droite : prévisions 5 jours
 *
 * Règle clé : si un appel API échoue, l'écran conserve les dernières données valides.
 * Seul un Toast notifie l'utilisateur de l'erreur.
 */
class MainActivity : AppCompatActivity() {

    // ──────────────────────────────────────────────
    // Backend
    // ──────────────────────────────────────────────

    private val PREFS_NAME = "MeteoPrefs"
    private val KEY_API = "openweather_api_key"

    /** Ville par défaut. */
    private val DEFAULT_CITY = "Paris"

    /** Gestionnaire météo (API + Repository + Scheduler). */
    private lateinit var weatherManager: WeatherManager

    /** Handler pour poster les mises à jour UI sur le thread principal. */
    private val mainHandler = Handler(Looper.getMainLooper())

    // ──────────────────────────────────────────────
    // Vues — Colonne gauche
    // ──────────────────────────────────────────────

    private lateinit var tvLastUpdate: TextView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var ivCurrentIcon: ImageView
    private lateinit var tvCityName: TextView
    private lateinit var tvWeatherDescription: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var rvHourlyForecast: RecyclerView

    // ──────────────────────────────────────────────
    // Vues — Colonne droite
    // ──────────────────────────────────────────────

    private lateinit var rvDailyForecast: RecyclerView

    // ──────────────────────────────────────────────
    // Adapters
    // ──────────────────────────────────────────────

    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var hourlyAdapter: HourlyForecastAdapter

    // ══════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initAdapters()
        
        val savedKey = getSavedApiKey()
        if (savedKey.isNullOrEmpty()) {
            showApiKeyPrompt()
        } else {
            initWeatherManager(savedKey)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::weatherManager.isInitialized) {
            weatherManager.stop()
        }
    }

    // ══════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════

    /**
     * Lie toutes les vues XML aux variables Kotlin.
     */
    private fun initViews() {
        // Colonne gauche
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp)
        ivCurrentIcon = findViewById(R.id.ivCurrentIcon)
        tvCityName = findViewById(R.id.tvCityName)
        tvWeatherDescription = findViewById(R.id.tvWeatherDescription)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPressure = findViewById(R.id.tvPressure)
        rvHourlyForecast = findViewById(R.id.rvHourlyForecast)

        // Colonne droite
        rvDailyForecast = findViewById(R.id.rvDailyForecast)
    }

    /**
     * Initialise les RecyclerView avec leurs adapters et layout managers.
     */
    private fun initAdapters() {
        // Prévisions 5 jours (vertical)
        dailyAdapter = DailyForecastAdapter()
        rvDailyForecast.layoutManager = LinearLayoutManager(this)
        rvDailyForecast.adapter = dailyAdapter

        // Reste de la journée (horizontal)
        hourlyAdapter = HourlyForecastAdapter()
        rvHourlyForecast.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        rvHourlyForecast.adapter = hourlyAdapter
    }

    /**
     * Initialise le WeatherManager, configure les callbacks,
     * et lance le rafraîchissement automatique.
     */
    private fun initWeatherManager(apiKey: String) {
        weatherManager = WeatherManager(apiKey)

        // Callback succès : mettre à jour l'UI sur le thread principal
        weatherManager.onRefreshComplete {
            val info = weatherManager.repository.weatherInfo
            mainHandler.post { updateUI(info) }
        }

        // Callback erreur : NE PAS mettre à jour l'UI, juste afficher un Toast
        weatherManager.onRefreshError { errorMessage ->
            mainHandler.post {
                Toast.makeText(
                    this,
                    "Erreur de mise à jour : $errorMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Démarrer le rafraîchissement auto (premier appel immédiat + toutes les 5 min)
        weatherManager.start(DEFAULT_CITY)
    }

    // ══════════════════════════════════════════════
    // Mise à jour de l'UI
    // ══════════════════════════════════════════════

    /**
     * Met à jour toutes les vues avec les nouvelles données météo.
     * Cette méthode n'est appelée QUE quand le rafraîchissement réussit.
     * En cas d'erreur API, l'écran conserve les données précédentes.
     *
     * @param info Données météo complètes validées
     */
    private fun updateUI(info: WeatherInfo) {
        // ── Dernière mise à jour (top left) ──
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val updateDate = Date(info.lastUpdateTimestamp)
        tvLastUpdate.text = "Dernière MAJ : ${dateFormat.format(updateDate)} à ${timeFormat.format(updateDate)}"

        // ── Météo actuelle (colonne gauche) ──
        info.currentWeather?.let { current ->
            tvCurrentTemp.text = "${current.temperature.toInt()}°"
            tvCityName.text = "${current.cityName}, ${current.country}"
            tvWeatherDescription.text = current.weatherDescription.replaceFirstChar { it.uppercase() }
            tvFeelsLike.text = "${current.feelsLike.toInt()}°C"
            tvHumidity.text = "${current.humidity}%"
            tvWind.text = "${current.windSpeed} m/s"
            tvPressure.text = "${current.pressure} hPa"

            // Icône météo actuelle
            DailyForecastAdapter.loadWeatherIcon(ivCurrentIcon, current.weatherIconCode)
        }

        // ── Reste de la journée (colonne gauche, sous météo actuelle) ──
        // Récupère les ForecastItem bruts du premier jour pour le défilement horaire
        if (info.dailyForecasts.isNotEmpty()) {
            val allHourlyItems = info.dailyForecasts.flatMap { it.hourlyForecasts }
            hourlyAdapter.updateData(allHourlyItems)
        }

        // ── Prévisions 5 jours (colonne droite) ──
        dailyAdapter.updateData(info.dailyForecasts)
    }

    // ══════════════════════════════════════════════
    // Gestion de la clé API
    // ══════════════════════════════════════════════

    private fun getSavedApiKey(): String? {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.getString(KEY_API, null)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveApiKey(apiKey: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().putString(KEY_API, apiKey).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de sauvegarde de la clé", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showApiKeyPrompt() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Clé API OpenWeather"

        AlertDialog.Builder(this)
            .setTitle("Bienvenue")
            .setMessage("Veuillez entrer votre clé API OpenWeatherMap pour continuer.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Valider") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    saveApiKey(key)
                    initWeatherManager(key)
                } else {
                    Toast.makeText(this, "La clé ne peut pas être vide", Toast.LENGTH_SHORT).show()
                    showApiKeyPrompt()
                }
            }
            .show()
    }
}
