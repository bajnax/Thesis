package com.savonia.thesis;

import java.util.HashMap;

public class GattAttributesSample {

    public static final String UUID_SENSORS_SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e";
    public static final String UUID_SENSORS_CHARACTERISTIC = "713d0002-503e-4c75-ba94-3148f18d941e";
    public static final String DEVICE_ADDRESS = "FF:F5:6F:20:5B:01";

    private static HashMap<String, String> attributes = new HashMap();

    static {
        attributes.put(UUID_SENSORS_SERVICE, "Temperature and gas service: ");
        attributes.put(UUID_SENSORS_CHARACTERISTIC, "Temperature and gas characteristic");
    }

    public static String getName(String uuid) {
        String name = attributes.get(uuid);
        return name;
    }
}
