# Android Weather App ☀️🌧️

A modern Android application for viewing the current weather and 5-day forecasts for Paris (default city), built natively with Kotlin.

## Features

- **Current Weather**: Temperature, "feels like" temperature, humidity, wind speed, pressure, etc.
- **Rest of the Day**: Hourly forecasts in 3-hour intervals (temperatures and icons).
- **5-Day Forecast**: Min/Max temperatures, dominant weather condition, and probability of precipitation.
- **Auto Refresh**: The application automatically fetches new data every 5 minutes in the background (when the app is running).
- **Secure API Key Storage**: The OpenWeather API key is requested upon the first launch and securely stored using `EncryptedSharedPreferences`.
- **Modern User Interface**: Dark Navy theme, glassmorphism card effects, and a two-column layout (optimized for tablets or large screens).

## Architecture

The application is built on a clean architecture (MVC/MVVM-like, without heavy architecture components for now):

- **`models`**: Pure data models (Kotlin Data classes).
- **`api`**: Direct network requests using `HttpURLConnection` to OpenWeatherMap with manual JSON parsing.
- **`repository`**: `WeatherRepository` orchestrates the API, aggregates data, and acts as the in-memory single source of truth.
- **`scheduler`**: `WeatherRefreshScheduler` uses a `Timer` to handle the update frequency (5 minutes).
- **`ui`**: 
  - `MainActivity`: Handles portrait/landscape displays and sets up RecyclerViews.
  - `DailyForecastAdapter` & `HourlyForecastAdapter`: Adapters for handling the RecyclerView lists.
  - Initialization dialog for the encrypted API key.

## Dependencies

The app minimizes the use of third-party libraries, relying on the following standard components:
- `androidx.appcompat:appcompat` and `androidx.core:core-ktx` for standard UI components.
- `com.google.android.material:material` for the design system.
- `androidx.recyclerview:recyclerview` for efficient list rendering.
- `androidx.security:security-crypto` for encrypted storage of the API key using the Android Keystore.

## Setup and Build

### Prerequisites
- Java JDK 17
- Android SDK (including Command Line Tools and Build Tools 34.0.0)

### Building the app
The project uses Gradle. You can build the APK via the terminal:
```bash
./gradlew assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

### First Launch
1. Open the application.
2. A dialog will prompt you for your **OpenWeatherMap** API key.
3. Enter your key. If valid, the app will automatically load the weather data and securely save your key.
