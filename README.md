# StepUp Android App

A comprehensive step tracking Android application built with Kotlin that helps users monitor their daily steps, explore nearby locations, and create walking memories.

## Features

### Core Features
- **Step Counter**: Real-time step tracking using device sensors with foreground service
- **Daily Step Targets**: Customizable daily step goals (5000, 6500, 8000 steps, or custom)
- **Health Metrics**: Track steps, distance (km), and calories burned
- **Weekly Statistics**: Comprehensive 7-day history with charts and progress tracking
- **Goal Progress**: Visual progress bars and achievement notifications

### Location & Exploration
- **Nearby Places Discovery**: Find interesting locations near you using OpenTripMap API
- **Location Categories**: Filter by interests (Amusements, Architecture, Cultural, Shops, Foods, Sport, Historical, Natural)
- **Interactive Maps**: OpenStreetMap integration with location details
- **Location Details**: Detailed information about nearby attractions and places
- **Route Visualization**: Display walking routes to destinations

### Memory Management
- **Photo Memories**: Create and store memories of visited locations
- **Location-Tagged Photos**: Associate photos with specific locations and dates
- **Memory Gallery**: Browse through your walking adventures and achievements

### User Management
- **User Authentication**: Secure login/register system with Supabase backend
- **User Profiles**: Manage personal information and preferences
- **Interest Preferences**: Customize location recommendations based on interests
- **Setup Wizard**: Initial app configuration for new users

### Smart Notifications
- **Goal Achievement Alerts**: Notifications at 75%, 90%, 95%, and 100% goal completion
- **Daily Reminders**: Customizable walking reminders
- **Background Tracking**: Persistent step counting even when app is closed

### Analytics & Insights
- **Progress Charts**: Visual representation of daily and weekly progress using Jetpack Compose
- **Streak Tracking**: Monitor consecutive days of goal achievement
- **Average Statistics**: Weekly and monthly step averages
- **Performance Insights**: Track improvements over time

## Tech Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Android Views + Jetpack Compose (for charts)
- **Architecture**: Service-oriented architecture with foreground services
- **Data Binding**: View Binding and Data Binding enabled

### Backend & Database
- **Backend**: Supabase (PostgreSQL + Authentication)
- **Local Storage**: SharedPreferences for user settings and daily data
- **Real-time Sync**: Profile synchronization with cloud backend

### APIs & Services
- **Maps**: OpenStreetMap (OSMDroid)
- **Location Data**: OpenTripMap API for points of interest
- **Image Loading**: Glide for efficient image loading and caching

### Key Libraries
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0
- **JSON Parsing**: Gson converter
- **Compose UI**: Version 1.5.1
- **Material Design**: Material 3 (1.1.2)
- **Maps**: OSMDroid for OpenStreetMap integration
- **Coroutines**: Kotlin Coroutines for asynchronous operations

## Dependencies

### Core Android Dependencies
```gradle
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.recyclerview:recyclerview:1.3.1")
implementation("androidx.cardview:cardview:1.0.0")
```

### Location & Maps
```gradle
implementation("com.google.android.gms:play-services-location:21.1.0")
// OSMDroid for OpenStreetMap (check build.gradle for exact version)
```

### Networking & API
```gradle
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

### UI & Compose
```gradle
implementation("androidx.compose.ui:ui:1.5.1")
implementation("androidx.compose.foundation:foundation:1.5.1")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.compose.runtime:runtime-livedata:1.5.1")
```

### Image Loading
```gradle
implementation("com.github.bumptech.glide:glide:4.16.0")
annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
```

## Getting Started

### Prerequisites
- **Android Studio**: Latest stable version (2023.1 or newer)
- **Android SDK**: API Level 28 (Android 9.0) minimum, API Level 34 target
- **Java Development Kit**: JDK 1.8 or higher
- **Kotlin**: 1.9.0 or higher

### Step-by-Step Setup

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd StepUpAppDev/stepup
```

#### 2. Setup Supabase Backend
1. Create a Supabase project at [supabase.com](https://supabase.com)
2. Create a file named `supabase.properties` in the project root (next to `README.md`):
   ```properties
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_KEY=your-public-anon-key
   ```

#### 3. Configure API Keys
1. Get an API key from [OpenTripMap](https://opentripmap.io/docs)
2. Open `app/src/main/java/com/example/stepupapp/api/OpenTripMapService.kt` and set your API key in the appropriate variable or constant (see comments in that file).

#### 4. Android Studio Setup
1. Open the project in Android Studio
2. Let Gradle sync automatically
3. Resolve any SDK version issues if prompted
4. Ensure all required permissions are granted in the manifest

#### 5. Build Configuration
1. Update `local.properties` with your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```
2. Sync project with Gradle files

#### 6. Database Setup (Supabase)
Create the following table in your Supabase database:
```sql
CREATE TABLE Profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    pfp_url TEXT,
    setup_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### 7. Permissions Setup
The app requires the following permissions (already configured in AndroidManifest.xml):
- `ACTIVITY_RECOGNITION` - For step counting
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - For location services
- `INTERNET` - For API calls
- `FOREGROUND_SERVICE` - For background step tracking
- `POST_NOTIFICATIONS` - For goal achievement notifications
- `CAMERA` - For taking photos in memories
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` - For selecting and saving images

> **Note:** The app will request Camera and Storage permissions for photo memories. Accept these when prompted. If denied, you can enable them in your device's app settings.

#### 8. Testing
1. **Emulator Testing**: The app includes emulator mode for step counting simulation (note: camera and step counting may not work as expected on emulator)
2. **Device Testing**: Install on a physical device for accurate step counting and camera features
3. **Location Testing**: Ensure location services are enabled for the explore feature

### Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Install and run on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## App Architecture

### Activities Overview
- **LoadingScreenActivity**: App entry point and splash screen
- **AuthOptionsActivity**: Authentication selection
- **LoginActivity** / **RegisterActivity**: User authentication
- **MainActivity**: Initial setup and onboarding
- **HomeActivity**: Main dashboard with step tracking
- **ExploreActivity**: Location discovery and mapping
- **MemoryActivity**: Photo memories management
- **SettingsActivity**: App configuration
- **StepsOverviewActivity**: Statistics and progress charts

### Services
- **StepCounterService**: Foreground service for continuous step counting
- **ProfileService**: User profile management with Supabase integration

### Key Components
- **LocationManager**: GPS and location services handling
- **PlaceRepository**: API integration for location data
- **UserPreferences**: Local data storage and settings
- **FilterManager**: Location filtering and categorization

## Configuration

### Customization Options
- **Step Targets**: Modify default targets in `UserPreferences.kt`
- **API Endpoints**: Update base URLs in API service files
- **UI Themes**: Customize colors and styles in `ui/theme/` directory
- **Notification Settings**: Adjust notification triggers in `StepCounterService.kt`

### Environment Variables
Create `local.properties` with:
```properties
sdk.dir=/path/to/android/sdk
MAPS_API_KEY=your_maps_api_key_if_needed
```

## Troubleshooting

### Common Issues
1. **Step Counter Not Working**: Ensure ACTIVITY_RECOGNITION permission is granted
2. **Location Not Found**: Check location permissions and GPS settings
3. **API Calls Failing**: Verify internet connection and API keys
4. **Build Errors**: Clean project and invalidate caches in Android Studio

### Debug Mode
- Enable logging in `build.gradle` for detailed logs
- Use Android Studio's Logcat to monitor app behavior
- Test location features on physical device for best results

## License

This project is for educational purposes. Please ensure you comply with all API terms of service when using third-party services.

---

**Note**: This app requires physical device testing for accurate step counting. The emulator mode is provided for development and testing purposes only.