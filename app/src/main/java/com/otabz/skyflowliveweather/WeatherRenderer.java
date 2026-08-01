package com.otabz.skyflowliveweather;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Locale;

public final class WeatherRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public WeatherRenderer(float density) {
        this.density = density;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void draw(
            Canvas canvas,
            int width,
            int height,
            long nowMillis,
            WeatherSnapshot snapshot,
            float xOffset,
            boolean animate,
            boolean showInfo
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float time = animate ? nowMillis / 1000f : 0f;
        WeatherSnapshot.Kind kind = snapshot.kind();

        drawSky(canvas, width, height, snapshot.isDay, kind);
        if (!snapshot.isDay) {
            drawStars(canvas, width, height, time);
        }
        drawSunOrMoon(canvas, width, height, snapshot.isDay, xOffset);

        int cloudCount = cloudCount(kind, snapshot.cloudCoverPercent);
        drawCloudLayer(canvas, width, height, time, xOffset, cloudCount, kind, snapshot.isDay);

        switch (kind) {
            case DRIZZLE:
                drawRain(canvas, width, height, time, 55, 0.55f);
                break;
            case RAIN:
                drawRain(canvas, width, height, time, 100, 0.9f);
                break;
            case SNOW:
                drawSnow(canvas, width, height, time);
                break;
            case FOG:
                drawFog(canvas, width, height, time);
                break;
            case STORM:
                drawRain(canvas, width, height, time, 120, 1.0f);
                drawLightning(canvas, width, height, time);
                break;
            default:
                break;
        }

        drawHills(canvas, width, height, snapshot.isDay, kind, xOffset);

        if (showInfo) {
            drawWeatherCard(canvas, width, snapshot);
        }
    }

    private void drawSky(
            Canvas canvas,
            int width,
            int height,
            boolean isDay,
            WeatherSnapshot.Kind kind
    ) {
        int top;
        int bottom;

        if (!isDay) {
            top = kind == WeatherSnapshot.Kind.STORM ? Color.rgb(8, 16, 31) : Color.rgb(10, 31, 63);
            bottom = kind == WeatherSnapshot.Kind.STORM ? Color.rgb(31, 38, 52) : Color.rgb(35, 77, 116);
        } else if (kind == WeatherSnapshot.Kind.STORM) {
            top = Color.rgb(48, 61, 75);
            bottom = Color.rgb(98, 111, 124);
        } else if (kind == WeatherSnapshot.Kind.RAIN || kind == WeatherSnapshot.Kind.FOG) {
            top = Color.rgb(94, 126, 151);
            bottom = Color.rgb(165, 187, 201);
        } else if (kind == WeatherSnapshot.Kind.SNOW) {
            top = Color.rgb(134, 179, 210);
            bottom = Color.rgb(222, 237, 246);
        } else {
            top = Color.rgb(40, 137, 221);
            bottom = Color.rgb(142, 210, 244);
        }

        paint.setShader(new LinearGradient(
                0,
                0,
                0,
                height,
                top,
                bottom,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawStars(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < 58; i++) {
            float x = pseudo(i * 37 + 11) * width;
            float y = pseudo(i * 83 + 7) * height * 0.66f;
            float radius = dp(0.8f + pseudo(i * 29) * 1.5f);
            float twinkle = 0.55f + 0.45f * (float) Math.sin(time * 1.3f + i * 0.9f);
            paint.setColor(Color.argb((int) (255 * twinkle), 255, 255, 230));
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private void drawSunOrMoon(Canvas canvas, int width, int height, boolean isDay, float xOffset) {
        float shift = (xOffset - 0.5f) * width * 0.08f;
        float cx = width * 0.76f - shift;
        float cy = height * 0.18f;
        float radius = Math.min(width, height) * 0.075f;

        if (isDay) {
            paint.setColor(Color.argb(48, 255, 232, 130));
            canvas.drawCircle(cx, cy, radius * 1.65f, paint);
            paint.setColor(Color.rgb(255, 218, 88));
            canvas.drawCircle(cx, cy, radius, paint);
        } else {
            paint.setColor(Color.rgb(242, 241, 219));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(Color.rgb(26, 57, 90));
            canvas.drawCircle(cx + radius * 0.38f, cy - radius * 0.18f, radius * 0.82f, paint);
        }
    }

    private int cloudCount(WeatherSnapshot.Kind kind, double cloudCover) {
        int base = (int) Math.round(2 + Math.max(0, Math.min(100, cloudCover)) / 22.0);
        if (kind == WeatherSnapshot.Kind.CLEAR) {
            return Math.min(2, base);
        }
        if (kind == WeatherSnapshot.Kind.STORM || kind == WeatherSnapshot.Kind.RAIN) {
            return Math.max(7, base);
        }
        return Math.max(4, base);
    }

    private void drawCloudLayer(
            Canvas canvas,
            int width,
            int height,
            float time,
            float xOffset,
            int count,
            WeatherSnapshot.Kind kind,
            boolean isDay
    ) {
        boolean dark = kind == WeatherSnapshot.Kind.RAIN
                || kind == WeatherSnapshot.Kind.STORM
                || kind == WeatherSnapshot.Kind.FOG;
        for (int i = 0; i < count; i++) {
            float scale = 0.72f + pseudo(i * 41) * 0.75f;
            float speed = 8f + pseudo(i * 13) * 14f;
            float span = width + dp(240);
            float x = wrap(pseudo(i * 59) * span + time * speed, span) - dp(120);
            x += (xOffset - 0.5f) * width * (0.05f + i * 0.008f);
            float y = height * (0.22f + pseudo(i * 31) * 0.32f);
            float alpha = 0.58f + pseudo(i * 17) * 0.34f;
            drawCloud(canvas, x, y, scale, alpha, dark, isDay);
        }
    }

    private void drawCloud(
            Canvas canvas,
            float x,
            float y,
            float scale,
            float alpha,
            boolean dark,
            boolean isDay
    ) {
        int base;
        if (dark) {
            base = isDay ? Color.rgb(88, 103, 116) : Color.rgb(38, 47, 62);
        } else {
            base = isDay ? Color.rgb(246, 250, 252) : Color.rgb(126, 151, 177);
        }
        paint.setColor(withAlpha(base, alpha));

        float w = dp(125) * scale;
        float h = dp(48) * scale;
        canvas.drawOval(new RectF(x, y, x + w, y + h), paint);
        canvas.drawCircle(x + w * 0.28f, y + h * 0.18f, h * 0.54f, paint);
        canvas.drawCircle(x + w * 0.52f, y - h * 0.06f, h * 0.68f, paint);
        canvas.drawCircle(x + w * 0.75f, y + h * 0.16f, h * 0.48f, paint);
    }

    private void drawRain(Canvas canvas, int width, int height, float time, int count, float intensity) {
        strokePaint.setStrokeWidth(dp(1.7f + intensity));
        strokePaint.setColor(Color.argb((int) (145 + intensity * 80), 188, 226, 251));
        float speed = 520f + intensity * 360f;

        for (int i = 0; i < count; i++) {
            float x = wrap(i * 73.1f + time * 85f, width + dp(90)) - dp(45);
            float y = wrap(i * 131.7f + time * speed, height + dp(120)) - dp(60);
            float length = dp(16 + pseudo(i * 23) * 18);
            canvas.drawLine(x, y, x - length * 0.28f, y + length, strokePaint);
        }
    }

    private void drawSnow(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < 78; i++) {
            float fallSpeed = 34f + pseudo(i * 19) * 58f;
            float y = wrap(i * 97.3f + time * fallSpeed, height + dp(50)) - dp(25);
            float baseX = pseudo(i * 61) * width;
            float x = baseX + (float) Math.sin(time * 0.85f + i) * dp(18);
            float radius = dp(1.8f + pseudo(i * 43) * 3.3f);
            paint.setColor(Color.argb(180 + (int) (pseudo(i) * 70), 255, 255, 255));
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private void drawFog(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < 8; i++) {
            float bandHeight = dp(34 + i * 4);
            float y = height * (0.28f + i * 0.075f);
            float offset = wrap(time * (8 + i * 1.7f) + i * 87, width + dp(180)) - dp(90);
            paint.setColor(Color.argb(35 + i * 5, 244, 249, 252));
            canvas.drawRoundRect(
                    new RectF(offset - width, y, offset + width * 1.25f, y + bandHeight),
                    bandHeight / 2,
                    bandHeight / 2,
                    paint
            );
        }
    }

    private void drawLightning(Canvas canvas, int width, int height, float time) {
        float cycle = time % 7.5f;
        boolean flash = cycle < 0.18f || (cycle > 0.32f && cycle < 0.42f);
        if (!flash) {
            return;
        }

        paint.setColor(Color.argb(55, 235, 244, 255));
        canvas.drawRect(0, 0, width, height, paint);

        float startX = width * 0.58f;
        float startY = height * 0.24f;
        Path bolt = new Path();
        bolt.moveTo(startX, startY);
        bolt.lineTo(startX - dp(18), startY + dp(72));
        bolt.lineTo(startX + dp(6), startY + dp(64));
        bolt.lineTo(startX - dp(30), startY + dp(142));
        bolt.lineTo(startX - dp(4), startY + dp(123));
        bolt.lineTo(startX - dp(12), startY + dp(185));

        strokePaint.setColor(Color.argb(245, 255, 246, 159));
        strokePaint.setStrokeWidth(dp(4.2f));
        canvas.drawPath(bolt, strokePaint);
    }

    private void drawHills(
            Canvas canvas,
            int width,
            int height,
            boolean isDay,
            WeatherSnapshot.Kind kind,
            float xOffset
    ) {
        float horizon = height * 0.79f;
        float shift = (xOffset - 0.5f) * width * 0.06f;

        Path back = new Path();
        back.moveTo(-width * 0.2f + shift, height);
        back.lineTo(-width * 0.2f + shift, horizon + dp(30));
        back.cubicTo(
                width * 0.18f + shift, horizon - dp(70),
                width * 0.48f + shift, horizon + dp(55),
                width * 0.76f + shift, horizon - dp(35)
        );
        back.cubicTo(
                width * 0.95f + shift, horizon - dp(75),
                width * 1.22f + shift, horizon + dp(20),
                width * 1.3f + shift, horizon
        );
        back.lineTo(width * 1.3f + shift, height);
        back.close();

        int backColor = isDay ? Color.rgb(53, 118, 105) : Color.rgb(25, 57, 68);
        if (kind == WeatherSnapshot.Kind.SNOW) {
            backColor = isDay ? Color.rgb(118, 153, 163) : Color.rgb(47, 70, 81);
        }
        paint.setColor(backColor);
        canvas.drawPath(back, paint);

        Path front = new Path();
        front.moveTo(0, height);
        front.lineTo(0, horizon + dp(70));
        front.cubicTo(
                width * 0.24f, horizon - dp(12),
                width * 0.54f, horizon + dp(84),
                width * 0.77f, horizon + dp(20)
        );
        front.cubicTo(
                width * 0.92f, horizon - dp(25),
                width, horizon + dp(35),
                width, horizon + dp(35)
        );
        front.lineTo(width, height);
        front.close();
        paint.setColor(isDay ? Color.rgb(28, 93, 77) : Color.rgb(14, 43, 52));
        canvas.drawPath(front, paint);
    }

    private void drawWeatherCard(Canvas canvas, int width, WeatherSnapshot snapshot) {
        float margin = dp(22);
        float top = dp(58);
        float cardWidth = Math.min(width - margin * 2, dp(300));
        float cardHeight = dp(108);
        RectF card = new RectF(margin, top, margin + cardWidth, top + cardHeight);

        paint.setColor(Color.argb(snapshot.isDay ? 145 : 125, 15, 31, 46));
        canvas.drawRoundRect(card, dp(20), dp(20), paint);

        paint.setColor(Color.WHITE);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        paint.setTextSize(dp(34));
        canvas.drawText(snapshot.temperatureText(), margin + dp(18), top + dp(43), paint);

        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        paint.setTextSize(dp(16));
        canvas.drawText(snapshot.description(), margin + dp(18), top + dp(69), paint);

        paint.setColor(Color.argb(210, 255, 255, 255));
        paint.setTextSize(dp(12.5f));
        String details = String.format(
                Locale.getDefault(),
                "%s  ·  wind %.0f km/h",
                snapshot.locationLabel,
                snapshot.windSpeedKmh
        );
        canvas.drawText(ellipsize(details, 43), margin + dp(18), top + dp(92), paint);
    }

    private String ellipsize(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private float dp(float value) {
        return value * density;
    }

    private float wrap(float value, float max) {
        float result = value % max;
        return result < 0 ? result + max : result;
    }

    private float pseudo(int seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return (float) (value - Math.floor(value));
    }

    private int withAlpha(int color, float alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, (int) (alpha * 255))),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }
}
