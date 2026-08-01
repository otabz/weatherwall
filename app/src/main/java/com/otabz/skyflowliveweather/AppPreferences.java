package com.otabz.skyflowliveweather;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPreferences {
    public static final String PREFS_NAME = "skyflow_preferences";

    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LOCATION_LABEL = "location_label";
    public static final String KEY_LOCATION_MODE = "location_mode";
    public static final String KEY_LOCATION_TIMEZONE = "location_timezone";
    public static final String KEY_LOCATION_REVISION = "location_revision";
    public static final String KEY_SHOW_INFO = "show_info";
    public static final String KEY_ANIMATE = "animate";

    public static final String LOCATION_MODE_DEVICE = "device";
    public static final String LOCATION_MODE_CITY = "city";
    public static final String LOCATION_MODE_MANUAL = "manual";

    private static final double DEFAULT_LATITUDE = -36.8485;
    private static final double DEFAULT_LONGITUDE = 174.7633;
    private static final String DEFAULT_LABEL = "Auckland, New Zealand";
    private static final String DEFAULT_TIMEZONE = "Pacific/Auckland";

    private AppPreferences() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static double latitude(Context context) {
        long bits = prefs(context).getLong(
                KEY_LATITUDE,
                Double.doubleToRawLongBits(DEFAULT_LATITUDE)
        );
        return Double.longBitsToDouble(bits);
    }

    public static double longitude(Context context) {
        long bits = prefs(context).getLong(
                KEY_LONGITUDE,
                Double.doubleToRawLongBits(DEFAULT_LONGITUDE)
        );
        return Double.longBitsToDouble(bits);
    }

    public static String locationLabel(Context context) {
        return prefs(context).getString(KEY_LOCATION_LABEL, DEFAULT_LABEL);
    }

    public static String locationMode(Context context) {
        return prefs(context).getString(KEY_LOCATION_MODE, LOCATION_MODE_CITY);
    }

    public static String locationTimezone(Context context) {
        return prefs(context).getString(KEY_LOCATION_TIMEZONE, DEFAULT_TIMEZONE);
    }

    public static long locationRevision(Context context) {
        return prefs(context).getLong(KEY_LOCATION_REVISION, 0L);
    }

    public static void setLocation(
            Context context,
            double latitude,
            double longitude,
            String label,
            String mode,
            String timezone
    ) {
        SharedPreferences preferences = prefs(context);
        long nextRevision = preferences.getLong(KEY_LOCATION_REVISION, 0L) + 1L;
        preferences.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putString(KEY_LOCATION_LABEL, clean(label, DEFAULT_LABEL))
                .putString(KEY_LOCATION_MODE, clean(mode, LOCATION_MODE_CITY))
                .putString(KEY_LOCATION_TIMEZONE, clean(timezone, "auto"))
                .putLong(KEY_LOCATION_REVISION, nextRevision)
                .apply();
    }

    public static void setLocation(Context context, double latitude, double longitude, String label) {
        setLocation(
                context,
                latitude,
                longitude,
                label,
                LOCATION_MODE_MANUAL,
                "auto"
        );
    }

    public static boolean showInfo(Context context) {
        return prefs(context).getBoolean(KEY_SHOW_INFO, true);
    }

    public static void setShowInfo(Context context, boolean showInfo) {
        prefs(context).edit().putBoolean(KEY_SHOW_INFO, showInfo).apply();
    }

    public static boolean animate(Context context) {
        return prefs(context).getBoolean(KEY_ANIMATE, true);
    }

    public static void setAnimate(Context context, boolean animate) {
        prefs(context).edit().putBoolean(KEY_ANIMATE, animate).apply();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
