/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki.handler;

import static org.openhab.binding.hideki.HidekiBindingConstants.*;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HidekiAnemometerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiAnemometerHandler extends HidekiBaseHandler {

    private final Logger logger = LoggerFactory.getLogger(HidekiAnemometerHandler.class);

    private static final int TYPE = 0x0C;
    private int[] data = null;

    public HidekiAnemometerHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handle command {} on channel {}", command, channelUID);

        if (command instanceof RefreshType && (data != null)) {
            final String channelId = channelUID.getId();
            if (TEMPERATURE.equals(channelId)) {
                updateState(channelUID, new DecimalType(getTemperature()));
            } else if (CHILL.equals(channelId)) {
                updateState(channelUID, new DecimalType(getWindChill()));
            } else if (SPEED.equals(channelId)) {
                updateState(channelUID, new DecimalType(getWindSpeed()));
            } else if (GUST.equals(channelId)) {
                updateState(channelUID, new DecimalType(getWindGust()));
            } else if (DIRECTION.equals(channelId)) {
                updateState(channelUID, new DecimalType(getWindDirection()));
            } else {
                super.handleCommand(channelUID, command);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiAnemometerHandler: Thing may not be null.");

        logger.debug("Initialize Hideki anemometer handler.");

        super.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose anemometer handler.");
        super.dispose();
        data = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final int[] data) {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiAnemometerHandler: Thing may not be null.");
        if (ThingStatus.ONLINE != thing.getStatus()) {
            return;
        }

        super.setData(data); // Decode common parts first
        if (TYPE == getDecodedType()) {
            if (data.length == getDecodedLength()) {
                if (logger.isTraceEnabled()) {
                    final String raw = Arrays.toString(data);
                    logger.trace("Got new anemometer data: {}.", raw);
                }

                synchronized (this.data) {
                    if (this.data == null) {
                        this.data = new int[data.length];
                    }
                    System.arraycopy(data, 0, this.data, 0, data.length);
                }

                final Channel tChannel = thing.getChannel(TEMPERATURE);
                updateState(tChannel.getUID(), new DecimalType(getTemperature()));

                final Channel cChannel = thing.getChannel(CHILL);
                updateState(cChannel.getUID(), new DecimalType(getWindChill()));

                final Channel sChannel = thing.getChannel(SPEED);
                updateState(sChannel.getUID(), new DecimalType(getWindSpeed()));

                final Channel gChannel = thing.getChannel(GUST);
                updateState(gChannel.getUID(), new DecimalType(getWindGust()));

                final Channel dChannel = thing.getChannel(DIRECTION);
                updateState(dChannel.getUID(), new DecimalType(getWindDirection()));
            } else {
                logger.error("Got wrong anemometer data length {}.", data.length);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getSensorType() {
        return TYPE;
    }

    /**
     * Returns decoded temperature.
     *
     * @return Decoded temperature
     */
    private double getTemperature() {
        double value = (data[5] & 0x0F) * 10 + (data[4] >> 4) + (data[4] & 0x0F) * 0.1;
        if ((data[5] >> 4) != 0x0C) {
            value = (data[5] >> 4) == 0x04 ? -value : Double.MAX_VALUE;
        }

        return value;
    }

    /**
     * Returns decoded wind chill.
     *
     * @return Decoded wind chill
     */
    private double getWindChill() {
        double value = (data[7] & 0x0F) * 10 + (data[6] >> 4) + (data[6] & 0x0F) * 0.1;
        if ((data[7] >> 4) != 0x0C) {
            value = (data[7] >> 4) == 0x04 ? -value : Double.MAX_VALUE;
        }

        return value;
    }

    /**
     * Returns decoded wind speed.
     *
     * @return Decoded wind speed
     */
    private double getWindSpeed() {
        return 1.60934 * ((data[8] >> 4) + (data[8] & 0x0F) / 10.0 + (data[9] & 0x0F) * 10.0);
    }

    /**
     * Returns decoded wind gust.
     *
     * @return Decoded wind gust
     */
    private double getWindGust() {
        return 1.60934 * ((data[9] >> 4) / 10.0 + (data[10] & 0x0F) + (data[10] >> 4) * 10.0);
    }

    /**
     * Returns decoded wind direction.
     *
     * @return Decoded wind direction
     */
    private double getWindDirection() {
        int segment = (data[11] >> 4);
        segment = segment ^ (segment & 8) >> 1;
        segment = segment ^ (segment & 4) >> 1;
        segment = segment ^ (segment & 2) >> 1;
        return 22.5 * (-segment & 0xF);
    }

}
