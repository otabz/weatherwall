package com.otabz.skyflowliveweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class WeatherWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WeatherEngine();
    }

    private final class WeatherEngine extends Engine
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final long WEATHER_REFRESH_MS = 30L * 60L * 1000L;
        private static final long TRANSITION_DURATION_MS = 900L;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final WeatherRenderer renderer = new WeatherRenderer(
                getResources().getDisplayMetrics().density
        );

        private WeatherSnapshot snapshot;
        private WeatherSnapshot previousSnapshot;
        private long transitionStartedAtMillis;
        private boolean visible;
        private boolean surfaceReady;
        private boolean destroyed;
        private boolean showInfo;
        private boolean animate;
        private float xOffset = 0.5f;
        private int width;
        private int height;

        private final Runnable frameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!visible || !surfaceReady || destroyed) {
                    return;
                }
                drawFrame();
                handler.postDelayed(this, frameDelayMillis());
            }
        };

        private final Runnable weatherRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                requestWeather(false);
                if (visible) {
                    handler.postDelayed(this, WEATHER_REFRESH_MS);
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            snapshot = WeatherRepository.cached(WeatherWallpaperService.this);
            if (snapshot == null) {
                snapshot = WeatherSnapshot.fallback(
                        AppPreferences.locationLabel(WeatherWallpaperService.this)
                );
            }
            reloadSettings();
            AppPreferences.prefs(WeatherWallpaperService.this)
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            handler.removeCallbacks(frameRunnable);
            handler.removeCallbacks(weatherRefreshRunnable);

            if (visible) {
                requestWeather(false);
                drawFrame();
                handler.post(frameRunnable);
                handler.postDelayed(weatherRefreshRunnable, WEATHER_REFRESH_MS);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            surfaceReady = true;
            width = holder.getSurfaceFrame().width();
            height = holder.getSurfaceFrame().height();
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int newWidth, int newHeight) {
            super.onSurfaceChanged(holder, format, newWidth, newHeight);
            surfaceReady = true;
            width = newWidth;
            height = newHeight;
            drawFrame();
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            super.onSurfaceRedrawNeeded(holder);
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            handler.removeCallbacks(frameRunnable);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onOffsetsChanged(
                float newXOffset,
                float yOffset,
                float xOffsetStep,
                float yOffsetStep,
                int xPixelOffset,
                int yPixelOffset
        ) {
            xOffset = newXOffset;
            if (visible) {
                drawFrame();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            reloadSettings();

            if (WeatherRepository.isWeatherCacheKey(key)) {
                WeatherSnapshot updated = WeatherRepository.cached(
                        WeatherWallpaperService.this
                );
                if (updated != null) {
                    applySnapshot(updated);
                }
            }

            if (visible) {
                drawFrame();
            }
        }

        @Override
        public void onDestroy() {
            destroyed = true;
            visible = false;
            handler.removeCallbacksAndMessages(null);
            AppPreferences.prefs(WeatherWallpaperService.this)
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        private void reloadSettings() {
            showInfo = AppPreferences.showInfo(WeatherWallpaperService.this);
            animate = AppPreferences.animate(WeatherWallpaperService.this);
        }

        private long frameDelayMillis() {
            if (previousSnapshot != null) {
                return 50L;
            }
            if (!animate) {
                return 1000L;
            }
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isPowerSaveMode()) {
                return 150L;
            }
            return 50L;
        }

        private void requestWeather(boolean force) {
            WeatherRepository.fetch(
                    WeatherWallpaperService.this,
                    force,
                    new WeatherRepository.Callback() {
                        @Override
                        public void onSuccess(WeatherSnapshot updated, boolean fromCache) {
                            if (destroyed) {
                                return;
                            }
                            applySnapshot(updated);
                            if (visible) {
                                drawFrame();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            // Continue displaying the last cached or fallback scene.
                        }
                    }
            );
        }


        private void applySnapshot(WeatherSnapshot updated) {
            if (updated == null) {
                return;
            }
            if (snapshot != null && snapshot.fetchedAtMillis != updated.fetchedAtMillis) {
                previousSnapshot = snapshot;
                transitionStartedAtMillis = System.currentTimeMillis();
            }
            snapshot = updated;
        }

        private float transitionProgress(long nowMillis) {
            if (previousSnapshot == null || transitionStartedAtMillis <= 0L) {
                return 1f;
            }
            return Math.max(
                    0f,
                    Math.min(
                            1f,
                            (nowMillis - transitionStartedAtMillis) / (float) TRANSITION_DURATION_MS
                    )
            );
        }

        private float easeInOut(float value) {
            return value * value * (3f - 2f * value);
        }

        private void drawFrame() {
            if (!surfaceReady || destroyed) {
                return;
            }

            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) {
                    return;
                }
                int canvasWidth = width > 0 ? width : canvas.getWidth();
                int canvasHeight = height > 0 ? height : canvas.getHeight();
                long nowMillis = System.currentTimeMillis();
                float transitionProgress = transitionProgress(nowMillis);

                if (previousSnapshot != null && transitionProgress < 1f) {
                    renderer.draw(
                            canvas,
                            canvasWidth,
                            canvasHeight,
                            nowMillis,
                            previousSnapshot,
                            xOffset,
                            animate,
                            showInfo
                    );
                    int alpha = Math.max(
                            0,
                            Math.min(255, Math.round(255f * easeInOut(transitionProgress)))
                    );
                    int layer = canvas.saveLayerAlpha(
                            0f,
                            0f,
                            canvasWidth,
                            canvasHeight,
                            alpha
                    );
                    renderer.draw(
                            canvas,
                            canvasWidth,
                            canvasHeight,
                            nowMillis,
                            snapshot,
                            xOffset,
                            animate,
                            showInfo
                    );
                    canvas.restoreToCount(layer);
                } else {
                    previousSnapshot = null;
                    renderer.draw(
                            canvas,
                            canvasWidth,
                            canvasHeight,
                            nowMillis,
                            snapshot,
                            xOffset,
                            animate,
                            showInfo
                    );
                }
            } catch (RuntimeException ignored) {
                // Surface transitions can briefly invalidate the drawing canvas.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // The surface may have been destroyed during the draw.
                    }
                }
            }
        }
    }
}
