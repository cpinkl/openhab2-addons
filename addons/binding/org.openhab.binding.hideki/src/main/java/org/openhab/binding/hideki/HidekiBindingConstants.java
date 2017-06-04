/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HidekiBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiBindingConstants {

    private static final String BINDING_ID = "hideki";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_RECEIVER = new ThingTypeUID(BINDING_ID, "receiver");
    public static final ThingTypeUID THING_TYPE_THERMOMETER = new ThingTypeUID(BINDING_ID, "thermometer");
    public static final ThingTypeUID THING_TYPE_PLUVIOMETER = new ThingTypeUID(BINDING_ID, "pluviometer");
    public static final ThingTypeUID THING_TYPE_ANEMOMETER = new ThingTypeUID(BINDING_ID, "anemometer");
    public static final ThingTypeUID THING_TYPE_UVMETER = new ThingTypeUID(BINDING_ID, "uvmeter");

    // Common channels
    public static final String RECEIVED_UPDATE = "updated";
    public static final String SENSOR_ID = "sensor";
    public static final String SENSOR_CHANNEL = "channel";
    public static final String SENSOR_TYPE = "type";
    public static final String MESSAGE_NUMBER = "message";
    public static final String TEMPERATURE = "temperature";

    // Thermometer channels
    public static final String HUMIDITY = "humidity";
    public static final String BATTERY = "battery";

    // Anemometer channels
    public static final String CHILL = "chill";
    public static final String SPEED = "speed";
    public static final String GUST = "gust";
    public static final String DIRECTION = "direction";

    // Pluviometer channels
    public static final String RAIN_LEVEL = "rain";

    // UV-Meter channels
    public static final String MED = "med";
    public static final String UV = "uv";
}
