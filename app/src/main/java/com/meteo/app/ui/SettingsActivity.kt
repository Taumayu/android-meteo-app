package com.meteo.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meteo.app.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerTheme: Spinner
    private lateinit var rgLocationMode: RadioGroup
    private lateinit var rbGps: RadioButton
    private lateinit var rbManual: RadioButton
    private lateinit var etCityName: EditText
    private lateinit var btnChangeApiKey: Button
    private lateinit var tvGithubLink: TextView

    private val PREFS_NAME = "MeteoPrefs"
    private val KEY_API = "openweather_api_key"
    private val KEY_THEME = "app_theme"
    private val KEY_IS_GPS = "is_using_gps"
    private val KEY_CITY = "manual_city"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadPreferences()
        setupListeners()
    }

    private fun initViews() {
        spinnerTheme = findViewById(R.id.spinnerTheme)
        rgLocationMode = findViewById(R.id.rgLocationMode)
        rbGps = findViewById(R.id.rbGps)
        rbManual = findViewById(R.id.rbManual)
        etCityName = findViewById(R.id.etCityName)
        btnChangeApiKey = findViewById(R.id.btnChangeApiKey)
        tvGithubLink = findViewById(R.id.tvGithubLink)

        // Setup Spinner
        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        spinnerTheme.adapter = adapter
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Theme
        val themePref = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        spinnerTheme.setSelection(when(themePref) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        })

        // Location
        val isGps = prefs.getBoolean(KEY_IS_GPS, false)
        val city = prefs.getString(KEY_CITY, "Paris")
        
        if (isGps) {
            rbGps.isChecked = true
            etCityName.visibility = View.GONE
        } else {
            rbManual.isChecked = true
            etCityName.visibility = View.VISIBLE
        }
        etCityName.setText(city)
    }

    private fun setupListeners() {
        rgLocationMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbGps) {
                etCityName.visibility = View.GONE
            } else {
                etCityName.visibility = View.VISIBLE
            }
        }

        btnChangeApiKey.setOnClickListener {
            showApiKeyPrompt()
        }

        tvGithubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Taumayu/android-meteo-app"))
            startActivity(intent)
        }

        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isFirstLaunch = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    return
                }
                val mode = when (position) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onPause() {
        super.onPause()
        savePreferences()
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val mode = when (spinnerTheme.selectedItemPosition) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        prefs.edit().apply {
            putInt(KEY_THEME, mode)
            putBoolean(KEY_IS_GPS, rbGps.isChecked)
            putString(KEY_CITY, etCityName.text.toString().trim().ifEmpty { "Paris" })
        }.apply()
    }

    private fun showApiKeyPrompt() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = getString(R.string.api_key)

        AlertDialog.Builder(this)
            .setTitle(R.string.api_key)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    saveApiKey(key)
                    Toast.makeText(this, "API Key updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.key_cannot_be_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
            Toast.makeText(this, "Key save error", Toast.LENGTH_SHORT).show()
        }
    }
}
