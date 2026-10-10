package com.meteo.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meteo.app.R
import com.meteo.app.backlog.models.DailyForecast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter RecyclerView pour la liste des prévisions sur 5 jours (colonne droite).
 *
 * Chaque item affiche :
 * - Le nom du jour (Lundi, Mardi...)
 * - L'icône météo dominante du jour
 * - Les températures min / max
 * - La probabilité de précipitation avec une barre de progression
 */
class DailyForecastAdapter : RecyclerView.Adapter<DailyForecastAdapter.DayViewHolder>() {

    /** Liste des prévisions quotidiennes affichées. */
    private var forecasts: List<DailyForecast> = emptyList()

    /**
     * Met à jour les données affichées.
     * Appelé uniquement quand le rafraîchissement réussit.
     *
     * @param newForecasts Nouvelles prévisions quotidiennes
     */
    fun updateData(newForecasts: List<DailyForecast>) {
        forecasts = newForecasts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(forecasts[position])
    }

    override fun getItemCount(): Int = forecasts.size

    /**
     * ViewHolder pour un jour de prévision.
     */
    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val ivDayIcon: ImageView = itemView.findViewById(R.id.ivDayIcon)
        private val tvDayTempMax: TextView = itemView.findViewById(R.id.tvDayTempMax)
        private val tvDayTempMin: TextView = itemView.findViewById(R.id.tvDayTempMin)
        private val tvPrecipProb: TextView = itemView.findViewById(R.id.tvPrecipProb)
        private val pbPrecipBar: ProgressBar = itemView.findViewById(R.id.pbPrecipBar)

        /**
         * Lie les données d'une prévision quotidienne aux vues.
         */
        fun bind(forecast: DailyForecast) {
            // Nom du jour (ex: "Lundi", "Mardi"...)
            tvDayName.text = formatDayName(forecast.date)

            // Températures min / max arrondies
            tvDayTempMax.text = "max ${forecast.temperatureMax.toInt()}°"
            tvDayTempMin.text = "min ${forecast.temperatureMin.toInt()}°"

            // Probabilité de précipitation
            val precipPercent = (forecast.maxPrecipitationProbability * 100).toInt()
            tvPrecipProb.text = "💧 $precipPercent%"
            pbPrecipBar.progress = precipPercent

            // Icône météo (chargée depuis OpenWeather)
            loadWeatherIcon(ivDayIcon, forecast.dominantIconCode)
        }
    }

    companion object {
        /**
         * Convertit une date "yyyy-MM-dd" en nom de jour localisé (ex: "Lundi").
         */
        fun formatDayName(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(dateString) ?: return dateString
                val outputFormat = SimpleDateFormat("EEEE", Locale.FRENCH)
                outputFormat.format(date).replaceFirstChar { it.uppercase() }
            } catch (e: Exception) {
                dateString
            }
        }

        /**
         * Charge l'icône météo depuis l'URL OpenWeather dans l'ImageView.
         * Utilise une approche simple avec un thread réseau.
         * En production, utiliser Glide ou Coil.
         *
         * @param imageView ImageView cible
         * @param iconCode  Code icône OpenWeather (ex: "01d", "10n")
         */
        fun loadWeatherIcon(imageView: ImageView, iconCode: String) {
            val url = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
            // Note: En production, remplacer par Glide/Coil :
            // Glide.with(imageView.context).load(url).into(imageView)
            //
            // Pour l'instant, placeholder en attendant l'intégration d'une lib d'images.
            Thread {
                try {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                    imageView.post { imageView.setImageBitmap(bitmap) }
                    input.close()
                    connection.disconnect()
                } catch (e: Exception) {
                    // Silently fail — l'icône reste vide
                }
            }.start()
        }
    }
}
