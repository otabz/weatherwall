package com.otabz.skyflowliveweather;

import java.util.Locale;

public final class WeatherSnapshot {
    public enum Kind {
        CLEAR,
        CLOUDS,
        DRIZZLE,
        RAIN,
        SNOW,
        FOG,
        STORM
    }

    public final long fetchedAtMillis;
    public final double temperatureC;
    public final int weatherCode;
    public final boolean isDay;
    public final double precipitationMm;
    public final double cloudCoverPercent;
    public final double windSpeedKmh;
    public final String locationLabel;

    public WeatherSnapshot(
            long fetchedAtMillis,
            double temperatureC,
            int weatherCode,
            boolean isDay,
            double precipitationMm,
            double cloudCoverPercent,
            double windSpeedKmh,
            String locationLabel
    ) {
        this.fetchedAtMillis = fetchedAtMillis;
        this.temperatureC = temperatureC;
        this.weatherCode = weatherCode;
        this.isDay = isDay;
        this.precipitationMm = precipitationMm;
        this.cloudCoverPercent = cloudCoverPercent;
        this.windSpeedKmh = windSpeedKmh;
        this.locationLabel = locationLabel == null ? "Selected location" : locationLabel;
    }

    public Kind kind() {
        return kindForCode(weatherCode);
    }

    public String description() {
        return descriptionForCode(weatherCode);
    }

    public String temperatureText() {
        return String.format(Locale.getDefault(), "%.0f°", temperatureC);
    }

    public static Kind kindForCode(int code) {
        if (code == 0 || code == 1) {
            return Kind.CLEAR;
        }
        if (code == 2 || code == 3) {
            return Kind.CLOUDS;
        }
        if (code == 45 || code == 48) {
            return Kind.FOG;
        }
        if ((code >= 51 && code <= 57)) {
            return Kind.DRIZZLE;
        }
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return Kind.RAIN;
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return Kind.SNOW;
        }
        if (code == 95 || code == 96 || code == 99) {
            return Kind.STORM;
        }
        return Kind.CLOUDS;
    }

    public static String descriptionForCode(int code) {
        switch (code) {
            case 0:
                return "Clear";
            case 1:
                return "Mostly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
            case 48:
                return "Fog";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
                return "Drizzle";
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
                return "Rain";
            case 71:
            case 73:
            case 75:
            case 77:
                return "Snow";
            case 80:
            case 81:
            case 82:
                return "Rain showers";
            case 85:
            case 86:
                return "Snow showers";
            case 95:
            case 96:
            case 99:
                return "Thunderstorm";
            default:
                return "Cloudy";
        }
    }

    public static WeatherSnapshot fallback(String locationLabel) {
        return new WeatherSnapshot(
                System.currentTimeMillis(),
                18.0,
                2,
                true,
                0.0,
                45.0,
                10.0,
                locationLabel
        );
    }
}
