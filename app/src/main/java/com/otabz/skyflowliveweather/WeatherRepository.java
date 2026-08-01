package com.otabz.skyflowliveweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WeatherRepository {
    public interface Callback {
        void onSuccess(WeatherSnapshot snapshot, boolean fromCache);

        void onError(String message);
    }

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final long CACHE_MAX_AGE_MS = 30L * 60L * 1000L;

    private static final String KEY_FETCHED_AT = "weather_fetched_at";
    private static final String KEY_TEMP = "weather_temperature";
    private static final String KEY_CODE = "weather_code";
    private static final String KEY_IS_DAY = "weather_is_day";
    private static final String KEY_PRECIPITATION = "weather_precipitation";
    private static final String KEY_CLOUD = "weather_cloud_cover";
    private static final String KEY_WIND = "weather_wind_speed";
    private static final String KEY_WEATHER_LOCATION = "weather_location_label";
    private static final String KEY_WEATHER_LOCATION_REVISION = "weather_location_revision";

    private WeatherRepository() {
    }

    public static WeatherSnapshot cached(Context context) {
        SharedPreferences prefs = AppPreferences.prefs(context);
        long fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L);
        long cachedLocationRevision = prefs.getLong(KEY_WEATHER_LOCATION_REVISION, -1L);
        if (fetchedAt == 0L
                || cachedLocationRevision != AppPreferences.locationRevision(context)) {
            return null;
        }
        return new WeatherSnapshot(
                fetchedAt,
                Double.longBitsToDouble(
                        prefs.getLong(KEY_TEMP, Double.doubleToRawLongBits(18.0))
                ),
                prefs.getInt(KEY_CODE, 2),
                prefs.getBoolean(KEY_IS_DAY, true),
                Double.longBitsToDouble(
                        prefs.getLong(KEY_PRECIPITATION, Double.doubleToRawLongBits(0.0))
                ),
                Double.longBitsToDouble(
                        prefs.getLong(KEY_CLOUD, Double.doubleToRawLongBits(45.0))
                ),
                Double.longBitsToDouble(
                        prefs.getLong(KEY_WIND, Double.doubleToRawLongBits(10.0))
                ),
                prefs.getString(
                        KEY_WEATHER_LOCATION,
                        AppPreferences.locationLabel(context)
                )
        );
    }

    public static void invalidate(Context context) {
        AppPreferences.prefs(context).edit()
                .remove(KEY_FETCHED_AT)
                .remove(KEY_TEMP)
                .remove(KEY_CODE)
                .remove(KEY_IS_DAY)
                .remove(KEY_PRECIPITATION)
                .remove(KEY_CLOUD)
                .remove(KEY_WIND)
                .remove(KEY_WEATHER_LOCATION)
                .remove(KEY_WEATHER_LOCATION_REVISION)
                .apply();
    }

    public static boolean isWeatherCacheKey(String key) {
        return KEY_FETCHED_AT.equals(key)
                || KEY_TEMP.equals(key)
                || KEY_CODE.equals(key)
                || KEY_IS_DAY.equals(key)
                || KEY_PRECIPITATION.equals(key)
                || KEY_CLOUD.equals(key)
                || KEY_WIND.equals(key)
                || KEY_WEATHER_LOCATION.equals(key)
                || KEY_WEATHER_LOCATION_REVISION.equals(key);
    }

    public static void fetch(Context context, boolean force, Callback callback) {
        Context appContext = context.getApplicationContext();
        WeatherSnapshot cached = cached(appContext);
        long age = cached == null
                ? Long.MAX_VALUE
                : System.currentTimeMillis() - cached.fetchedAtMillis;

        if (!force && cached != null && age >= 0 && age < CACHE_MAX_AGE_MS) {
            MAIN_HANDLER.post(() -> callback.onSuccess(cached, true));
            return;
        }

        final long requestedLocationRevision = AppPreferences.locationRevision(appContext);
        NETWORK_EXECUTOR.execute(() -> {
            try {
                WeatherSnapshot fresh = requestCurrentWeather(appContext);
                if (requestedLocationRevision != AppPreferences.locationRevision(appContext)) {
                    throw new IOException("Location changed while weather was loading. Please refresh again.");
                }
                save(appContext, fresh, requestedLocationRevision);
                MAIN_HANDLER.post(() -> callback.onSuccess(fresh, false));
            } catch (Exception exception) {
                WeatherSnapshot stillValidCache = cached(appContext);
                if (stillValidCache != null) {
                    MAIN_HANDLER.post(() -> callback.onSuccess(stillValidCache, true));
                } else {
                    String message = exception.getMessage();
                    if (message == null || message.trim().isEmpty()) {
                        message = "Unable to download weather.";
                    }
                    String finalMessage = message;
                    MAIN_HANDLER.post(() -> callback.onError(finalMessage));
                }
            }
        });
    }

    private static WeatherSnapshot requestCurrentWeather(Context context) throws Exception {
        double latitude = AppPreferences.latitude(context);
        double longitude = AppPreferences.longitude(context);

        String endpoint = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f"
                        + "&current=temperature_2m,weather_code,is_day,precipitation,cloud_cover,wind_speed_10m"
                        + "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto",
                latitude,
                longitude
        );

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SkyFlowLiveWeather/1.1");

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Weather service returned HTTP " + status + ".");
            }

            String response = readAll(connection.getInputStream());
            JSONObject root = new JSONObject(response);
            JSONObject current = root.getJSONObject("current");

            int weatherCode = current.has("weather_code")
                    ? current.getInt("weather_code")
                    : current.optInt("weathercode", 2);

            return new WeatherSnapshot(
                    System.currentTimeMillis(),
                    current.optDouble("temperature_2m", 18.0),
                    weatherCode,
                    current.optInt("is_day", 1) == 1,
                    current.optDouble("precipitation", 0.0),
                    current.optDouble("cloud_cover", 45.0),
                    current.optDouble("wind_speed_10m", 10.0),
                    AppPreferences.locationLabel(context)
            );
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static void save(
            Context context,
            WeatherSnapshot snapshot,
            long locationRevision
    ) {
        AppPreferences.prefs(context).edit()
                .putLong(KEY_FETCHED_AT, snapshot.fetchedAtMillis)
                .putLong(KEY_TEMP, Double.doubleToRawLongBits(snapshot.temperatureC))
                .putInt(KEY_CODE, snapshot.weatherCode)
                .putBoolean(KEY_IS_DAY, snapshot.isDay)
                .putLong(
                        KEY_PRECIPITATION,
                        Double.doubleToRawLongBits(snapshot.precipitationMm)
                )
                .putLong(KEY_CLOUD, Double.doubleToRawLongBits(snapshot.cloudCoverPercent))
                .putLong(KEY_WIND, Double.doubleToRawLongBits(snapshot.windSpeedKmh))
                .putString(KEY_WEATHER_LOCATION, snapshot.locationLabel)
                .putLong(KEY_WEATHER_LOCATION_REVISION, locationRevision)
                .apply();
    }
}
