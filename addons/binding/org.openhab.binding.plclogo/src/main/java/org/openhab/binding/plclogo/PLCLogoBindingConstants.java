/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableMap;

/**
 * The {@link PLCLogoBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoBindingConstants {

    public static final @NonNull String BINDING_ID = "plclogo";

    // List of all thing type UIDs
    public static final @NonNull ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final @NonNull ThingTypeUID THING_TYPE_ANALOG = new ThingTypeUID(BINDING_ID, "analog");
    public static final @NonNull ThingTypeUID THING_TYPE_MEMORY = new ThingTypeUID(BINDING_ID, "memory");
    public static final @NonNull ThingTypeUID THING_TYPE_DIGITAL = new ThingTypeUID(BINDING_ID, "digital");
    public static final @NonNull ThingTypeUID THING_TYPE_DATETIME = new ThingTypeUID(BINDING_ID, "datetime");
    public static final @NonNull ThingTypeUID THING_TYPE_PULSE = new ThingTypeUID(BINDING_ID, "pulse");

    // List of all channels
    public static final @NonNull String STATE_CHANNEL = "state";
    public static final @NonNull String OBSERVE_CHANNEL = "observed";
    public static final @NonNull String VALUE_CHANNEL = "value";
    public static final @NonNull String RTC_CHANNEL = "rtc";

    // List of all channel properties
    public static final @NonNull String BLOCK_PROPERTY = "block";

    // List of all item types
    public static final @NonNull String ANALOG_ITEM = "Number";
    public static final @NonNull String DATE_TIME_ITEM = "DateTime";
    public static final @NonNull String DIGITAL_INPUT_ITEM = "Contact";
    public static final @NonNull String DIGITAL_OUTPUT_ITEM = "Switch";

    // LOGO! family definitions
    public static final @NonNull String LOGO_0BA7 = "0BA7";
    public static final @NonNull String LOGO_0BA8 = "0BA8";

    // LOGO! diagnostics memory
    public static final @NonNull Integer LOGO_STATE = 984; // Diagnostics

    private static final Map<Integer, String> LOGO_STATES_0BA7 = new ImmutableMap.Builder<Integer, String>()
    // @formatter:off
        /*
         *   put(   "VB", 0);
         * - Network access errors
         * - Expansion module bus errors
         * - SD card read/write errors
         * - SD card write protection
         *
         * - Netzwerkzugriffsfehler
         * - Erweiterungsmodul-Busfehler
         * - Fehler beim Lesen oder Schreiben der SD-Karte
         * - Schreibschutz der SD-Karte
         */
        .build();
    // @formatter:on

    private static final Map<Integer, String> LOGO_STATES_0BA8 = new ImmutableMap.Builder<Integer, String>()
    // @formatter:off
            /*
            *   put(   "VB", 0);
            * - Ethernet link errors
            * - Expansion module changed
            * - SD card read/write errors
            */
        .put(8, "SD card does not exist")
            /*
            * - SD card is full
            *
            * - Netzwerk Verbindungsfehler
            * - Ausgetauschtes Erweiterungsmodul
            * - Fehler beim Lesen oder Schreiben der SD-Karte
            *
            * put(8, "SD-Karte nicht vorhanden");
            *
            * - SD-Karte voll
            */
        .build();
    // @formatter:on

    public static final Map<String, Map<Integer, String>> LOGO_STATES = ImmutableMap.of(
    // @formatter:off
        LOGO_0BA7, LOGO_STATES_0BA7,
        LOGO_0BA8, LOGO_STATES_0BA8
    // @formatter:on
    );

    // LOGO! RTC memory
    public static final @NonNull Integer LOGO_DATE = 985; // RTC date for 3 bytes: year month day
    public static final @NonNull Integer LOGO_TIME = 988; // RTC time for 3 bytes: hour minute second

    public static final class Layout {
        public final int address;
        public final int length;

        public Layout(final int address, final int length) {
            this.address = address;
            this.length = length;
        }
    }

    private static final Map<String, Layout> LOGO_MEMORY_0BA7 = new ImmutableMap.Builder<String, Layout>()
    // @formatter:off
        .put("VB", new Layout(0, 850))
        .put("VD", new Layout(0, 850))
        .put("VW", new Layout(0, 850))
        .put("I", new Layout(923, 3)) // Digital inputs starts at 923 for 3 bytes
        .put("Q", new Layout(942, 2)) // Digital outputs starts at 942 for 2 bytes
        .put("M", new Layout(948, 4)) // Digital markers starts at 948 for 4 bytes
        .put("AI", new Layout(926, 16)) // Analog inputs starts at 926 for 16 bytes -> 8 words
        .put("AQ", new Layout(944, 4)) // Analog outputs starts at 944 for 4 bytes -> 2 words
        .put("AM", new Layout(952, 32)) // Analog markers starts at 952 for 32 bytes -> 16 words
        .put("SIZE", new Layout(0, 984)) // Size of memory block for LOGO! 7
        .build();
    // @formatter:on

    private static final Map<String, Layout> LOGO_MEMORY_0BA8 = new ImmutableMap.Builder<String, Layout>()
    // @formatter:off
        .put(   "VB", new Layout(0, 850))
        .put(   "VD", new Layout(0, 850))
        .put(   "VW", new Layout(0, 850))
        .put(    "I", new Layout(1024, 8))   // Digital inputs starts at 1024 for 8 bytes
        .put(    "Q", new Layout(1064, 8))   // Digital outputs starts at 1064 for 8 bytes
        .put(    "M", new Layout(1104, 14))  // Digital markers starts at 1104 for 14 bytes
        .put(   "AI", new Layout(1032, 32))  // Analog inputs starts at 1032 for 32 bytes -> 16 words
        .put(   "AQ", new Layout(1072, 32))  // Analog outputs starts at 1072 for 32 bytes -> 16 words
        .put(   "AM", new Layout(1118, 128)) // Analog markers starts at 1118 for 128 bytes -> 64 words
        .put(   "NI", new Layout(1246, 16))  // Network inputs starts at 1246 for 16 bytes
        .put(  "NAI", new Layout(1262, 128)) // Network analog inputs starts at 1262 for 128 bytes -> 64 words
        .put(   "NQ", new Layout(1390, 16))  // Network outputs starts at 1390 for 16 bytes
        .put(  "NAQ", new Layout(1406, 64))  // Network analog inputs starts at 1406 for 64 bytes -> 32 words
        .put( "SIZE", new Layout(0, 1470))   // Size of memory block for LOGO! 8
        .build();
    // @formatter:on

    public static final Map<String, Map<String, Layout>> LOGO_MEMORY_BLOCK = ImmutableMap.of(
    // @formatter:off
        LOGO_0BA7, LOGO_MEMORY_0BA7,
        LOGO_0BA8, LOGO_MEMORY_0BA8
    // @formatter:on
    );

}
