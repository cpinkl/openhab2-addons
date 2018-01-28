/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HidekiBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiBindingConstants {

    private static final @NonNull String BINDING_ID = "hideki";

    // List of all Thing Type UIDs
    public static final @NonNull ThingTypeUID THING_TYPE_RECEIVER = new ThingTypeUID(BINDING_ID, "receiver");
    public static final @NonNull ThingTypeUID THING_TYPE_THERMOMETER = new ThingTypeUID(BINDING_ID, "thermometer");
    public static final @NonNull ThingTypeUID THING_TYPE_PLUVIOMETER = new ThingTypeUID(BINDING_ID, "pluviometer");
    public static final @NonNull ThingTypeUID THING_TYPE_ANEMOMETER = new ThingTypeUID(BINDING_ID, "anemometer");
    public static final @NonNull ThingTypeUID THING_TYPE_UVMETER = new ThingTypeUID(BINDING_ID, "uvmeter");

    // Common channels
    public static final @NonNull String RECEIVED_UPDATE = "updated";
    public static final @NonNull String RSSI_VALUE = "rssi";
    public static final @NonNull String SENSOR_ID = "sensor";
    public static final @NonNull String SENSOR_CHANNEL = "channel";
    public static final @NonNull String SENSOR_TYPE = "type";
    public static final @NonNull String MESSAGE_NUMBER = "message";
    public static final @NonNull String TEMPERATURE = "temperature";

    // Thermometer channels
    public static final @NonNull String HUMIDITY = "humidity";
    public static final @NonNull String BATTERY = "battery";

    // Anemometer channels
    public static final @NonNull String CHILL = "chill";
    public static final @NonNull String SPEED = "speed";
    public static final @NonNull String GUST = "gust";
    public static final @NonNull String DIRECTION = "direction";

    // Pluviometer channels
    public static final @NonNull String RAIN_LEVEL = "rain";

    // UV-Meter channels
    public static final @NonNull String MED = "med";
    public static final @NonNull String UV_INDEX = "uv";
}
