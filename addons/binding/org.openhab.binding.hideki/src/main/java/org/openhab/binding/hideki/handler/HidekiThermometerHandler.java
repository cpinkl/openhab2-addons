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
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HidekiThermometerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiThermometerHandler extends HidekiBaseHandler {

    private final Logger logger = LoggerFactory.getLogger(HidekiThermometerHandler.class);

    private static final int TYPE = 0x1E;
    private int[] data = null;

    public HidekiThermometerHandler(Thing thing) {
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
            } else if (HUMIDITY.equals(channelId)) {
                updateState(channelUID, new DecimalType(getHumidity()));
            } else if (BATTERY.equals(channelId)) {
                updateState(channelUID, getBatteryState() ? OnOffType.ON : OnOffType.OFF);
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
        Objects.requireNonNull(thing, "HidekiThermometerHandler: Thing may not be null.");

        logger.debug("Initialize Hideki thermometer handler.");

        super.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose thermometer handler.");
        super.dispose();
        data = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final int[] data) {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiThermometerHandler: Thing may not be null.");
        if (ThingStatus.ONLINE != thing.getStatus()) {
            return;
        }

        super.setData(data); // Decode common parts first
        if (TYPE == getDecodedType()) {
            if (data.length == getDecodedLength()) {
                if (logger.isTraceEnabled()) {
                    final String raw = Arrays.toString(data);
                    logger.trace("Got new thermometer data: {}.", raw);
                }

                synchronized (this.data) {
                    if (this.data == null) {
                        this.data = new int[data.length];
                    }
                    System.arraycopy(data, 0, this.data, 0, data.length);
                }

                final Channel tChannel = thing.getChannel(TEMPERATURE);
                updateState(tChannel.getUID(), new DecimalType(getTemperature()));

                final Channel hChannel = thing.getChannel(HUMIDITY);
                updateState(hChannel.getUID(), new DecimalType(getHumidity()));

                final Channel bChannel = thing.getChannel(BATTERY);
                updateState(bChannel.getUID(), getBatteryState() ? OnOffType.ON : OnOffType.OFF);
            } else {
                logger.error("Got wrong thermometer data length {}.", data.length);
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
     * Returns decoded humidity.
     *
     * @return Decoded humidity
     */
    private double getHumidity() {
        return (data[6] >> 4) * 10 + (data[6] & 0x0F);
    }

    /**
     * Returns decoded battery state. Is true, if battery is ok
     * and false otherwise
     *
     * @return Decoded battery state
     */
    private boolean getBatteryState() {
        return (data[2] >> 6) > 0;
    }

}
