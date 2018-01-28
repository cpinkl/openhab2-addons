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

import java.util.Calendar;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HidekiBaseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public abstract class HidekiBaseHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HidekiBaseHandler.class);

    private int[] data = null;

    public HidekiBaseHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
        logger.debug("Handle command {} on channel {}", command, channelUID);

        if (command instanceof RefreshType && (data != null)) {
            final String channelId = channelUID.getId();
            if (SENSOR_ID.equals(channelId)) {
                updateState(channelUID, new DecimalType(getSensorId()));
            } else if (SENSOR_CHANNEL.equals(channelId)) {
                updateState(channelUID, new DecimalType(getChannel()));
            } else if (MESSAGE_NUMBER.equals(channelId)) {
                updateState(channelUID, new DecimalType(getMessageNumber()));
            } else if (RSSI_VALUE.equals(channelId)) {
                updateState(channelUID, new DecimalType(getRSSIValue()));
            } else if (RECEIVED_UPDATE.equals(channelId)) {
                // No update of received time
            } else {
                logger.warn("Received command {} on unknown channel {}.", command, channelUID);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiBaseHandler: Thing may not be null.");

        logger.debug("Initialize Hideki base handler.");
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose Hideki base handler.");
        super.dispose();
        data = null;
    }

    /**
     * Update sensor values of current thing with new data.
     *
     * @param data Data value to update with
     */
    public void setData(final int[] data) {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiBaseHandler: Thing may not be null.");
        if (ThingStatus.ONLINE != thing.getStatus()) {
            return;
        }

        if (getSensorType() == getDecodedType(data)) {
            synchronized (this) {
                if (this.data == null) {
                    this.data = new int[data.length];
                }
                System.arraycopy(data, 0, this.data, 0, data.length);
            }

            if (data.length == getDecodedLength()) {
                final Channel uChannel = thing.getChannel(RECEIVED_UPDATE);
                if (uChannel != null) {
                    updateState(uChannel.getUID(), new DateTimeType(Calendar.getInstance()));
                }

                final Channel rChannel = thing.getChannel(RSSI_VALUE);
                if (rChannel != null) {
                    updateState(rChannel.getUID(), new DecimalType(getRSSIValue()));
                }

                final Channel iChannel = thing.getChannel(SENSOR_ID);
                if (iChannel != null) {
                    updateState(iChannel.getUID(), new DecimalType(getSensorId()));
                }

                final Channel cChannel = thing.getChannel(SENSOR_CHANNEL);
                if (cChannel != null) {
                    updateState(cChannel.getUID(), new DecimalType(getChannel()));
                }

                final Channel nChannel = thing.getChannel(MESSAGE_NUMBER);
                if (nChannel != null) {
                    updateState(nChannel.getUID(), new DecimalType(getMessageNumber()));
                }
            } else {
                logger.error("Got wrong sensor data length {}.", data.length);
            }
        }
    }

    /**
     * Returns sensor type supported by handler.
     *
     * @return Supported sensor type
     */
    protected abstract int getSensorType();

    /**
     * Returns decoded sensor type. Is negative, if decoder failed.
     *
     * @return Decoded sensor type
     */
    protected static int getDecodedType(final int[] data) {
        return (data == null) || (data.length < 4) ? -1 : data[3] & 0x1F;
    }

    /**
     * Returns decoded data length. Is negative, if decoder failed.
     *
     * @return Decoded sensor type
     */
    protected int getDecodedLength() {
        final int length = data.length < 3 ? -1 : (data[2] >> 1) & 0x1F;
        return length < 0 ? length : length + 2; // First byte is 0x9F preamble and last RSSI value
    }

    /**
     * Returns decoded sensor id. Is negative, if decoder failed.
     *
     * @return Decoded sensor id
     */
    private int getSensorId() {
        return data.length < 2 ? -1 : data[1] & 0x1F;
    }

    /**
     * Returns decoded sensor channel is used for transmission. Is not
     * zero for thermo/hygrometer only. Is negative, if decoder failed.
     *
     * @return Decoded sensor channel
     */
    private int getChannel() {
        int channel = data.length < 2 ? -1 : data[1] >> 5;
        if ((channel == 5) || (channel == 6)) {
            channel = channel - 1;
        } else if (channel > 3) {
            channel = 0;
        }

        return channel;
    }

    /**
     * Returns decoded received message number. Is negative, if
     * decoder failed.
     *
     * @return Decoded message number
     */
    private int getMessageNumber() {
        return data.length < 4 ? -1 : data[3] >> 6;
    }

    /**
     * Returns decoded RSSI value.
     *
     * @return Decoded RSSI value
     */
    private int getRSSIValue() {
        return data[data.length - 1];
    }
}
