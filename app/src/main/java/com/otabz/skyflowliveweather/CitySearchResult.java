package com.otabz.skyflowliveweather;

import java.util.ArrayList;
import java.util.List;

public final class CitySearchResult {
    public final long id;
    public final String name;
    public final String admin1;
    public final String country;
    public final String countryCode;
    public final String timezone;
    public final double latitude;
    public final double longitude;

    public CitySearchResult(
            long id,
            String name,
            String admin1,
            String country,
            String countryCode,
            String timezone,
            double latitude,
            double longitude
    ) {
        this.id = id;
        this.name = clean(name, "Unknown location");
        this.admin1 = clean(admin1, "");
        this.country = clean(country, "");
        this.countryCode = clean(countryCode, "");
        this.timezone = clean(timezone, "auto");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String displayLabel() {
        List<String> parts = new ArrayList<>();
        addDistinct(parts, name);
        addDistinct(parts, admin1);
        addDistinct(parts, country);
        return String.join(", ", parts);
    }

    public String coordinateText() {
        return String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude);
    }

    private static void addDistinct(List<String> parts, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String cleanValue = value.trim();
        for (String existing : parts) {
            if (existing.equalsIgnoreCase(cleanValue)) {
                return;
            }
        }
        parts.add(cleanValue);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
