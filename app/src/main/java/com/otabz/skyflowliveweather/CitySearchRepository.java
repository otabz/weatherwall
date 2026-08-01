package com.otabz.skyflowliveweather;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CitySearchRepository {
    public interface Callback {
        void onSuccess(List<CitySearchResult> results);

        void onError(String message);
    }

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private CitySearchRepository() {
    }

    public static void search(String rawQuery, Callback callback) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2) {
            MAIN_HANDLER.post(() -> callback.onError("Enter at least two characters."));
            return;
        }

        NETWORK_EXECUTOR.execute(() -> {
            try {
                List<CitySearchResult> results = request(query);
                MAIN_HANDLER.post(() -> callback.onSuccess(results));
            } catch (Exception exception) {
                String message = exception.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Unable to search for cities.";
                }
                String finalMessage = message;
                MAIN_HANDLER.post(() -> callback.onError(finalMessage));
            }
        });
    }

    private static List<CitySearchResult> request(String query) throws Exception {
        String language = Locale.getDefault().getLanguage();
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }

        String endpoint = "https://geocoding-api.open-meteo.com/v1/search"
                + "?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&count=8"
                + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8.name())
                + "&format=json";

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SkyFlowLiveWeather/1.1");

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException(readError(connection, status));
            }

            JSONObject root = new JSONObject(readAll(connection.getInputStream()));
            JSONArray jsonResults = root.optJSONArray("results");
            if (jsonResults == null || jsonResults.length() == 0) {
                return Collections.emptyList();
            }

            List<CitySearchResult> results = new ArrayList<>();
            for (int index = 0; index < jsonResults.length(); index++) {
                JSONObject item = jsonResults.optJSONObject(index);
                if (item == null || !item.has("latitude") || !item.has("longitude")) {
                    continue;
                }
                results.add(new CitySearchResult(
                        item.optLong("id", 0L),
                        item.optString("name", "Unknown location"),
                        item.optString("admin1", ""),
                        item.optString("country", ""),
                        item.optString("country_code", ""),
                        item.optString("timezone", "auto"),
                        item.optDouble("latitude"),
                        item.optDouble("longitude")
                ));
            }
            return results;
        } finally {
            connection.disconnect();
        }
    }

    private static String readError(HttpURLConnection connection, int status) {
        try {
            InputStream stream = connection.getErrorStream();
            if (stream != null) {
                JSONObject error = new JSONObject(readAll(stream));
                String reason = error.optString("reason", "").trim();
                if (!reason.isEmpty()) {
                    return reason;
                }
            }
        } catch (Exception ignored) {
            // Fall back to a concise HTTP message.
        }
        return "City search returned HTTP " + status + ".";
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
}
