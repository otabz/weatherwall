package com.otabz.skyflowliveweather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CitySearchResultTest {
    @Test
    public void displayLabelCombinesDistinctLocationParts() {
        CitySearchResult result = new CitySearchResult(
                1L,
                "Auckland",
                "Auckland",
                "New Zealand",
                "NZ",
                "Pacific/Auckland",
                -36.8485,
                174.7633
        );

        assertEquals("Auckland, New Zealand", result.displayLabel());
        assertEquals("-36.8485, 174.7633", result.coordinateText());
    }

    @Test
    public void displayLabelIncludesRegionWhenDifferent() {
        CitySearchResult result = new CitySearchResult(
                2L,
                "Albany",
                "New York",
                "United States",
                "US",
                "America/New_York",
                42.6526,
                -73.7562
        );

        assertEquals("Albany, New York, United States", result.displayLabel());
    }
}
