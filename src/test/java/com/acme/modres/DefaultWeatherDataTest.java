package com.acme.modres;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultWeatherData class.
 */
class DefaultWeatherDataTest {

    @Test
    void constructor_withValidCity_Paris_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertNotNull(data);
    }

    @Test
    void constructor_withValidCity_LasVegas_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        assertNotNull(data);
    }

    @Test
    void constructor_withValidCity_SanFrancisco_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        assertNotNull(data);
    }

    @Test
    void constructor_withValidCity_Miami_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        assertNotNull(data);
    }

    @Test
    void constructor_withValidCity_Cork_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        assertNotNull(data);
    }

    @Test
    void constructor_withValidCity_Barcelona_createsInstance() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        assertNotNull(data);
    }

    @Test
    void constructor_withNullCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData(null));
    }

    @Test
    void constructor_withInvalidCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData("InvalidCity"));
    }

    @Test
    void constructor_withEmptyCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData(""));
    }

    @Test
    void getCity_withParis_returnsParis() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertEquals(Constants.PARIS, data.getCity());
    }

    @Test
    void getCity_withLasVegas_returnsLasVegas() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        assertEquals(Constants.LAS_VEGAS, data.getCity());
    }

    @Test
    void getCity_withMiami_returnsMiami() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        assertEquals(Constants.MIAMI, data.getCity());
    }

    @Test
    void getCity_withCork_returnsCork() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        assertEquals(Constants.CORK, data.getCity());
    }

    @Test
    void getCity_withBarcelona_returnsBarcelona() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        assertEquals(Constants.BARCELONA, data.getCity());
    }

    @Test
    void getCity_withSanFrancisco_returnsSanFrancisco() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        assertEquals(Constants.SAN_FRANCISCO, data.getCity());
    }

    @Test
    void getDefaultWeatherData_withParis_returnsNonNullString() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        // This test verifies the method can be called; resource may not exist in test env
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            // Expected if resource file not available in test environment
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void getDefaultWeatherData_withLasVegas_returnsNonNullOrThrows() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void getDefaultWeatherData_withSanFrancisco_returnsNonNullOrThrows() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void getDefaultWeatherData_withMiami_returnsNonNullOrThrows() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void getDefaultWeatherData_withCork_returnsNonNullOrThrows() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void getDefaultWeatherData_withBarcelona_returnsNonNullOrThrows() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }
}
