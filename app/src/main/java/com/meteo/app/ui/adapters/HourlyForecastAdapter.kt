package com.meteo.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meteo.app.R
import com.meteo.app.backlog.models.ForecastItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter RecyclerView pour les créneaux horaires du reste de la journée
 * (scroll horizontal sous la météo actuelle).
 *
 * Chaque item affiche :
 * - L'heure (ex: "15h")
 * - L'icône météo
 * - La température
 */
class HourlyForecastAdapter : RecyclerView.Adapter<HourlyForecastAdapter.HourViewHolder>() {

    /** Liste des créneaux horaires affichés. */
    private var hourlyItems: List<ForecastItem> = emptyList()

    /**
     * Met à jour les données affichées avec les créneaux restants de la journée.
     * Filtre automatiquement pour ne garder que les créneaux futurs du jour en cours.
     *
     * @param allItems   Tous les ForecastItem disponibles
     */
    fun updateData(allItems: List<ForecastItem>) {
        val now = System.currentTimeMillis() / 1000  // En secondes Unix
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(tomorrowCal.time)

        // Garder les créneaux futurs d'aujourd'hui + ceux de demain matin si fin de journée
        hourlyItems = allItems.filter { item ->
            val itemDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(item.timestamp * 1000))
            item.timestamp > now && (itemDate == todayDateStr || itemDate == tomorrowDateStr)
        }.take(8)  // Max 8 créneaux (24h de prévisions restantes)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        holder.bind(hourlyItems[position])
    }

    override fun getItemCount(): Int = hourlyItems.size

    /**
     * ViewHolder pour un créneau horaire.
     */
    inner class HourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHourTime: TextView = itemView.findViewById(R.id.tvHourTime)
        private val ivHourIcon: ImageView = itemView.findViewById(R.id.ivHourIcon)
        private val tvHourTemp: TextView = itemView.findViewById(R.id.tvHourTemp)

        /**
         * Lie les données d'un créneau horaire aux vues.
         */
        fun bind(item: ForecastItem) {
            // Heure formatée (ex: "15h")
            val hourFormat = SimpleDateFormat("HH'h'", Locale.getDefault())
            tvHourTime.text = hourFormat.format(Date(item.timestamp * 1000))

            // Température arrondie
            tvHourTemp.text = "${item.temperature.toInt()}°"

            // Icône météo
            DailyForecastAdapter.loadWeatherIcon(ivHourIcon, item.weatherIconCode)
        }
    }
}
