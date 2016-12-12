package org.radarcns;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;

public class RadarConfiguration {
    public static final String RADAR_PREFIX = "org.radarcns.android.";

    public static final String KAFKA_REST_PROXY_URL_KEY = "kafka_rest_proxy_url";
    public static final String SCHEMA_REGISTRY_URL_KEY = "schema_registry_url";
    public static final String DEVICE_GROUP_ID_KEY = "device_group_id";
    public static final String EMPATICA_API_KEY = "empatica_api_key";
    public static final String UI_REFRESH_RATE_KEY = "ui_refresh_rate_millis";
    public static final String KAFKA_UPLOAD_RATE_KEY = "kafka_upload_rate";
    public static final String DATABASE_COMMIT_RATE_KEY = "database_commit_rate";
    public static final String KAFKA_CLEAN_RATE_KEY = "kafka_clean_rate";
    public static final String KAFKA_RECORDS_SEND_LIMIT_KEY = "kafka_records_send_limit";
    public static final String SENDER_CONNECTION_TIMEOUT_KEY = "sender_connection_timeout";
    public static final String DATA_RETENTION_KEY = "data_retention_ms";
    public static final String FETCH_SETTINGS_MS_KEY = "fetch_settings_ms";

    public static final Set<String> LONG_VALUES = new HashSet<>(Arrays.asList(
            UI_REFRESH_RATE_KEY, KAFKA_UPLOAD_RATE_KEY, DATABASE_COMMIT_RATE_KEY,
            KAFKA_CLEAN_RATE_KEY, SENDER_CONNECTION_TIMEOUT_KEY, DATA_RETENTION_KEY,
            FETCH_SETTINGS_MS_KEY));

    public static final Set<String> INT_VALUES = new HashSet<>(Arrays.asList(
            KAFKA_RECORDS_SEND_LIMIT_KEY));

    private static final Object syncObject = new Object();
    private static RadarConfiguration instance = null;
    private final FirebaseRemoteConfig config;

    private RadarConfiguration(@NonNull FirebaseRemoteConfig config) {
        this.config = config;
        // TODO: fetch settings with a timer and add listeners for that
    }

    public FirebaseRemoteConfig getFirebase() {
        return config;
    }

    public boolean isInDevelopmentMode() {
        return config.getInfo().getConfigSettings().isDeveloperModeEnabled();
    }

    public static boolean hasInstance() {
        synchronized (syncObject) {
            return instance != null;
        }
    }

    public static RadarConfiguration getInstance() {
        synchronized (syncObject) {
            if (instance == null) {
                throw new IllegalStateException("RadarConfiguration instance is not yet "
                        + "initialized");
            }
            return instance;
        }
    }

    public static RadarConfiguration configure(@NonNull FirebaseRemoteConfigSettings settings,
                                               int defaultSettings) {
        synchronized (syncObject) {
            if (instance == null) {
                FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
                config.setConfigSettings(settings);
                config.setDefaults(defaultSettings);

                instance = new RadarConfiguration(config);
            }
            return instance;
        }
    }

    public String getString(@NonNull String key) {
        String result = config.getString(key);

        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("Key does not have a value");
        }

        return result;
    }

    public String getString(@NonNull String key, String defaultValue) {
        String result = config.getString(key);

        if (result == null || result.isEmpty()) {
            return defaultValue;
        }

        return result;
    }

    /**
     * Get a configured long value.
     * @param key key of the value
     * @return long value
     * @throws NumberFormatException if the configured value is not a Long
     * @throws IllegalArgumentException if the key does not have an associated value
     */
    public long getLong(@NonNull String key) {
        return Long.parseLong(getString(key));
    }

    /**
     * Get a configured int value.
     * @param key key of the value
     * @return int value
     * @throws NumberFormatException if the configured value is not an Integer
     * @throws IllegalArgumentException if the key does not have an associated value
     */
    public int getInt(@NonNull String key) {
        return Integer.parseInt(getString(key));
    }

    /**
     * Get a configured long value. If the configured value is not present or not a valid long,
     * return a default value.
     * @param key key of the value
     * @param defaultValue default value
     * @return configured long value, or defaultValue if no suitable value was found.
     */
    public long getLong(@NonNull String key, long defaultValue) {
        try {
            String result = config.getString(key);
            if (result != null && !result.isEmpty()) {
                return Long.parseLong(result);
            }
        } catch (NumberFormatException ex) {
            // return default
        }
        return defaultValue;
    }

    /**
     * Get a configured long value. If the configured value is not present or not a valid long,
     * return a default value.
     * @param key key of the value
     * @param defaultValue default value
     * @return configured long value, or defaultValue if no suitable value was found.
     */
    public int getInt(@NonNull String key, int defaultValue) {
        try {
            String result = config.getString(key);
            if (result != null && !result.isEmpty()) {
                return Integer.parseInt(result);
            }
        } catch (NumberFormatException ex) {
            // return default
        }
        return defaultValue;
    }

    public boolean containsKey(@NonNull String key) {
        return config.getKeysByPrefix(key).contains(key);
    }

    public Set<String> keySet() {
        Set<String> baseKeys = new HashSet<>(config.getKeysByPrefix(null));
        Iterator<String> iter = baseKeys.iterator();
        while (iter.hasNext()) {
            if (getString(iter.next(), null) == null) {
                iter.remove();
            }
        }
        return baseKeys;
    }

    public boolean equals(Object obj) {
        return obj != null
                && !obj.getClass().equals(getClass())
                && config.equals(((RadarConfiguration) obj).config);
    }

    public int hashCode() {
        return config.hashCode();
    }

    public void putExtras(Bundle bundle, String... extras) {
        for (String extra : extras) {
            try {
                if (LONG_VALUES.contains(extra)) {
                    bundle.putLong(RADAR_PREFIX + extra, getLong(extra));
                } else if (INT_VALUES.contains(extra)) {
                    bundle.putInt(RADAR_PREFIX + extra, getInt(extra));
                } else {
                    bundle.putString(RADAR_PREFIX + extra, getString(extra));
                }
            } catch (IllegalArgumentException ex) {
                // do nothing
            }
        }
    }

    public static boolean hasExtra(Bundle bundle, String key) {
        return bundle.containsKey(RADAR_PREFIX + key);
    }

    public static int getIntExtra(Bundle bundle, String key, int defaultValue) {
        return bundle.getInt(RADAR_PREFIX + key, defaultValue);
    }

    public static int getIntExtra(Bundle bundle, String key) {
        return bundle.getInt(RADAR_PREFIX + key);
    }

    public static long getLongExtra(Bundle bundle, String key, long defaultValue) {
        return bundle.getLong(RADAR_PREFIX + key, defaultValue);
    }

    public static long getLongExtra(Bundle bundle, String key) {
        return bundle.getLong(RADAR_PREFIX + key);
    }

    public static String getStringExtra(Bundle bundle, String key, String defaultValue) {
        return bundle.getString(RADAR_PREFIX + key, defaultValue);
    }

    public static String getStringExtra(Bundle bundle, String key) {
        return bundle.getString(RADAR_PREFIX + key);
    }
}
