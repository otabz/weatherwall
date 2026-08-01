package com.otabz.skyflowliveweather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeatherSnapshotTest {
    @Test
    public void mapsOpenMeteoCodesToWallpaperKinds() {
        assertEquals(WeatherSnapshot.Kind.CLEAR, WeatherSnapshot.kindForCode(0));
        assertEquals(WeatherSnapshot.Kind.CLOUDS, WeatherSnapshot.kindForCode(3));
        assertEquals(WeatherSnapshot.Kind.FOG, WeatherSnapshot.kindForCode(45));
        assertEquals(WeatherSnapshot.Kind.DRIZZLE, WeatherSnapshot.kindForCode(53));
        assertEquals(WeatherSnapshot.Kind.RAIN, WeatherSnapshot.kindForCode(82));
        assertEquals(WeatherSnapshot.Kind.SNOW, WeatherSnapshot.kindForCode(86));
        assertEquals(WeatherSnapshot.Kind.STORM, WeatherSnapshot.kindForCode(99));
    }

    @Test
    public void unknownCodeFallsBackToClouds() {
        assertEquals(WeatherSnapshot.Kind.CLOUDS, WeatherSnapshot.kindForCode(999));
    }
}
