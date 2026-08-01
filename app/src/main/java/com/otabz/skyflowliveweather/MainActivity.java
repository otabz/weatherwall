package com.otabz.skyflowliveweather;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private EditText citySearchInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private TextView statusText;
    private TextView selectedLocationText;
    private CheckBox showWeatherCheck;
    private CheckBox animateWeatherCheck;
    private RadioGroup locationModeGroup;
    private LinearLayout citySearchContainer;
    private LinearLayout manualLocationContainer;
    private LinearLayout cityResultsContainer;
    private Button deviceLocationButton;
    private Button searchCityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        citySearchInput = findViewById(R.id.citySearchInput);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        statusText = findViewById(R.id.statusText);
        selectedLocationText = findViewById(R.id.selectedLocationText);
        showWeatherCheck = findViewById(R.id.showWeatherCheck);
        animateWeatherCheck = findViewById(R.id.animateWeatherCheck);
        locationModeGroup = findViewById(R.id.locationModeGroup);
        citySearchContainer = findViewById(R.id.citySearchContainer);
        manualLocationContainer = findViewById(R.id.manualLocationContainer);
        cityResultsContainer = findViewById(R.id.cityResultsContainer);
        deviceLocationButton = findViewById(R.id.deviceLocationButton);
        searchCityButton = findViewById(R.id.searchCityButton);

        Button saveLocationButton = findViewById(R.id.saveLocationButton);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button setWallpaperButton = findViewById(R.id.setWallpaperButton);

        saveLocationButton.setOnClickListener(view -> saveManualLocation());
        deviceLocationButton.setOnClickListener(view -> useDeviceLocation());
        searchCityButton.setOnClickListener(view -> searchCities());
        refreshButton.setOnClickListener(view -> refreshWeather(true));
        setWallpaperButton.setOnClickListener(view -> openWallpaperPreview());

        citySearchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCities();
                return true;
            }
            return false;
        });

        locationModeGroup.setOnCheckedChangeListener((group, checkedId) ->
                updateLocationModePanels(checkedId));

        showWeatherCheck.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setShowInfo(this, checked));
        animateWeatherCheck.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setAnimate(this, checked));

        loadPreferencesIntoForm();
        refreshWeather(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferencesIntoForm();
    }

    private void loadPreferencesIntoForm() {
        latitudeInput.setText(String.format(Locale.US, "%.6f", AppPreferences.latitude(this)));
        longitudeInput.setText(String.format(Locale.US, "%.6f", AppPreferences.longitude(this)));
        showWeatherCheck.setChecked(AppPreferences.showInfo(this));
        animateWeatherCheck.setChecked(AppPreferences.animate(this));

        selectedLocationText.setText(String.format(
                Locale.getDefault(),
                "%s\n%.4f, %.4f",
                AppPreferences.locationLabel(this),
                AppPreferences.latitude(this),
                AppPreferences.longitude(this)
        ));

        String mode = AppPreferences.locationMode(this);
        int radioId;
        if (AppPreferences.LOCATION_MODE_DEVICE.equals(mode)) {
            radioId = R.id.deviceLocationRadio;
        } else if (AppPreferences.LOCATION_MODE_MANUAL.equals(mode)) {
            radioId = R.id.manualLocationRadio;
        } else {
            radioId = R.id.cityLocationRadio;
        }
        if (locationModeGroup.getCheckedRadioButtonId() != radioId) {
            locationModeGroup.check(radioId);
        } else {
            updateLocationModePanels(radioId);
        }
    }

    private void updateLocationModePanels(int checkedId) {
        boolean deviceMode = checkedId == R.id.deviceLocationRadio;
        boolean cityMode = checkedId == R.id.cityLocationRadio;
        boolean manualMode = checkedId == R.id.manualLocationRadio;

        deviceLocationButton.setVisibility(deviceMode ? View.VISIBLE : View.GONE);
        citySearchContainer.setVisibility(cityMode ? View.VISIBLE : View.GONE);
        manualLocationContainer.setVisibility(manualMode ? View.VISIBLE : View.GONE);
    }

    private void searchCities() {
        String query = citySearchInput.getText().toString().trim();
        if (query.length() < 2) {
            showMessage("Enter at least two letters of a city or a postcode.");
            return;
        }

        hideKeyboard();
        cityResultsContainer.removeAllViews();
        searchCityButton.setEnabled(false);
        statusText.setText("Searching for “" + query + "”…");

        CitySearchRepository.search(query, new CitySearchRepository.Callback() {
            @Override
            public void onSuccess(List<CitySearchResult> results) {
                searchCityButton.setEnabled(true);
                if (results.isEmpty()) {
                    statusText.setText("No matching cities were found. Try a broader spelling.");
                    return;
                }
                statusText.setText("Select the correct city from the results below.");
                showCityResults(results);
            }

            @Override
            public void onError(String message) {
                searchCityButton.setEnabled(true);
                statusText.setText("City search failed: " + message);
            }
        });
    }

    private void showCityResults(List<CitySearchResult> results) {
        cityResultsContainer.removeAllViews();
        int verticalMargin = Math.round(5f * getResources().getDisplayMetrics().density);
        int horizontalPadding = Math.round(14f * getResources().getDisplayMetrics().density);
        int verticalPadding = Math.round(10f * getResources().getDisplayMetrics().density);

        for (CitySearchResult result : results) {
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setText(result.displayLabel() + "\n" + result.coordinateText());
            button.setPadding(
                    horizontalPadding,
                    verticalPadding,
                    horizontalPadding,
                    verticalPadding
            );
            button.setContentDescription("Use weather for " + result.displayLabel());
            button.setOnClickListener(view -> selectCity(result));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, verticalMargin, 0, verticalMargin);
            cityResultsContainer.addView(button, params);
        }
    }

    private void selectCity(CitySearchResult result) {
        WeatherRepository.invalidate(this);
        AppPreferences.setLocation(
                this,
                result.latitude,
                result.longitude,
                result.displayLabel(),
                AppPreferences.LOCATION_MODE_CITY,
                result.timezone
        );
        citySearchInput.setText(result.name);
        citySearchInput.setSelection(citySearchInput.length());
        cityResultsContainer.removeAllViews();
        loadPreferencesIntoForm();
        statusText.setText("Loading current weather for " + result.displayLabel() + "…");
        refreshWeather(true);
    }

    private void saveManualLocation() {
        try {
            double latitude = Double.parseDouble(latitudeInput.getText().toString().trim());
            double longitude = Double.parseDouble(longitudeInput.getText().toString().trim());

            if (latitude < -90.0 || latitude > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90.");
            }
            if (longitude < -180.0 || longitude > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180.");
            }

            WeatherRepository.invalidate(this);
            AppPreferences.setLocation(
                    this,
                    latitude,
                    longitude,
                    "Manual location",
                    AppPreferences.LOCATION_MODE_MANUAL,
                    "auto"
            );
            loadPreferencesIntoForm();
            statusText.setText(String.format(
                    Locale.getDefault(),
                    "Saved manual location: %.4f, %.4f",
                    latitude,
                    longitude
            ));
            refreshWeather(true);
        } catch (NumberFormatException exception) {
            showMessage("Enter valid latitude and longitude numbers.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void useDeviceLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }
        acquireDeviceLocation();
    }

    private void acquireDeviceLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            showMessage("Location service is unavailable.");
            return;
        }

        String provider = chooseProvider(manager);
        if (provider == null) {
            statusText.setText("Turn on device location, then try again.");
            try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                // The settings screen is not present on this device.
            }
            return;
        }

        statusText.setText("Finding your current location…");

        try {
            Location last = manager.getLastKnownLocation(provider);
            if (last != null) {
                applyDeviceLocation(last, true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(
                        provider,
                        null,
                        getMainExecutor(),
                        location -> {
                            if (location != null) {
                                applyDeviceLocation(location, true);
                            } else if (last == null) {
                                showMessage("A current location was not available.");
                            }
                        }
                );
            } else {
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        manager.removeUpdates(this);
                        applyDeviceLocation(location, true);
                    }

                    @Override
                    public void onProviderDisabled(String disabledProvider) {
                    }

                    @Override
                    public void onProviderEnabled(String enabledProvider) {
                    }

                    @Override
                    public void onStatusChanged(String changedProvider, int status, Bundle extras) {
                    }
                };
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
            }
        } catch (SecurityException exception) {
            showMessage("Location permission was not granted.");
        } catch (RuntimeException exception) {
            showMessage("Could not obtain device location: " + exception.getMessage());
        }
    }

    private String chooseProvider(LocationManager manager) {
        try {
            boolean fineGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;

            if (fineGranted && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return LocationManager.GPS_PROVIDER;
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                return LocationManager.NETWORK_PROVIDER;
            }
            List<String> providers = manager.getProviders(true);
            return providers.isEmpty() ? null : providers.get(0);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void applyDeviceLocation(Location location, boolean refresh) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        WeatherRepository.invalidate(this);
        AppPreferences.setLocation(
                this,
                latitude,
                longitude,
                "Current device location",
                AppPreferences.LOCATION_MODE_DEVICE,
                "auto"
        );
        loadPreferencesIntoForm();
        statusText.setText(String.format(
                Locale.getDefault(),
                "Using device location: %.4f, %.4f",
                latitude,
                longitude
        ));
        if (refresh) {
            refreshWeather(true);
        }
    }

    private void refreshWeather(boolean force) {
        statusText.setText("Refreshing current weather…");
        WeatherRepository.fetch(this, force, new WeatherRepository.Callback() {
            @Override
            public void onSuccess(WeatherSnapshot snapshot, boolean fromCache) {
                String source = fromCache ? "cached" : "updated";
                statusText.setText(String.format(
                        Locale.getDefault(),
                        "%s · %s · %s · wind %.0f km/h (%s)",
                        snapshot.locationLabel,
                        snapshot.temperatureText(),
                        snapshot.description(),
                        snapshot.windSpeedKmh,
                        source
                ));
            }

            @Override
            public void onError(String message) {
                statusText.setText("Weather refresh failed: " + message);
            }
        });
    }

    private void openWallpaperPreview() {
        ComponentName component = new ComponentName(this, WeatherWallpaperService.class);
        Intent direct = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        direct.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);

        try {
            startActivity(direct);
        } catch (ActivityNotFoundException exception) {
            Intent chooser = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            startActivity(chooser);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                acquireDeviceLocation();
            } else {
                showMessage("Location permission was declined. Search for a city instead.");
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View current = getCurrentFocus();
        if (manager != null && current != null) {
            manager.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
    }

    private void showMessage(String message) {
        statusText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
