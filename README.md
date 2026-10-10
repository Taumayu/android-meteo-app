# Android Météo App ☀️🌧️

Une application Android moderne permettant de consulter la météo actuelle et les prévisions sur 5 jours pour la ville de Paris (par défaut), développée en Kotlin natif.

## Fonctionnalités

- **Météo Actuelle** : Température, température ressentie, humidité, vitesse du vent, pression, etc.
- **Reste de la journée** : Prévisions horaires par tranches de 3 heures (températures et icônes).
- **Prévisions 5 jours** : Min/Max, condition dominante et probabilité de précipitation.
- **Rafraîchissement Auto** : L'application récupère de nouvelles données toutes les 5 minutes automatiquement en tâche de fond (lorsque l'application est ouverte).
- **Stockage Sécurisé de la Clé API** : La clé API d'OpenWeather est demandée au premier lancement de l'application et est stockée de manière chiffrée via `EncryptedSharedPreferences`.
- **Interface Utilisateur Moderne** : Thème sombre ("Dark Navy"), cartes utilisant l'effet glassmorphism, disposition en deux colonnes (optimisé tablettes ou grands écrans).

## Architecture

L'application est structurée selon une architecture claire (MVC/MVVM-like sans composants d'architecture lourds pour l'instant) :

- **`models`** : Modèles de données purs (Data classes Kotlin).
- **`api`** : Gestion des appels réseau directs avec `HttpURLConnection` vers OpenWeatherMap, avec un parsing JSON manuel.
- **`repository`** : `WeatherRepository` qui orchestre l'API, agrège les données, et fait office de source de vérité en mémoire.
- **`scheduler`** : `WeatherRefreshScheduler` basé sur `Timer` pour gérer la fréquence des mises à jour (5 minutes).
- **`ui`** : 
  - `MainActivity` : Gère l'affichage en mode portrait/paysage, affiche les RecyclerViews.
  - `DailyForecastAdapter` & `HourlyForecastAdapter` : Adaptateurs pour la gestion des listes RecyclerView.
  - Dialog d'initialisation pour la clé API chiffrée.

## Dépendances

L'application limite le nombre de bibliothèques tierces, mais utilise les composants suivants :
- `androidx.appcompat:appcompat` et `androidx.core:core-ktx` pour les composants UI standards.
- `com.google.android.material:material` pour le design système.
- `androidx.recyclerview:recyclerview` pour l'affichage de listes performantes.
- `androidx.security:security-crypto` pour le stockage chiffré de la clé API avec l'Android Keystore.

## Installation et Compilation

### Prérequis
- Java JDK 17
- Android SDK (avec Command Line Tools et Build Tools 34.0.0)

### Pour construire l'application
Le projet utilise Gradle. Vous pouvez générer l'APK via le terminal :
```bash
./gradlew assembleDebug
```
L'APK se trouvera dans `app/build/outputs/apk/debug/app-debug.apk`.

### Premier Lancement
1. Ouvrez l'application.
2. Une boîte de dialogue vous demandera votre clé API **OpenWeatherMap**.
3. Saisissez votre clé. Si elle est valide, l'application chargera automatiquement la météo et sauvegardera votre clé.
