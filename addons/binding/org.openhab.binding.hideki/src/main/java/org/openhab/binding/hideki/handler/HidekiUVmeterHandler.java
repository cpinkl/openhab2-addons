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
 * The {@link HidekiUVmeterHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiUVmeterHandler extends HidekiBaseHandler {

    private final Logger logger = LoggerFactory.getLogger(HidekiUVmeterHandler.class);

    private static final int TYPE = 0x0D;
    private int[] data = null;

    public HidekiUVmeterHandler(Thing thing) {
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
            } else if (MED.equals(channelId)) {
                updateState(channelUID, new DecimalType(getMED()));
            } else if (UV_INDEX.equals(channelId)) {
                updateState(channelUID, new DecimalType(getUVIndex()));
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
        Objects.requireNonNull(thing, "HidekiUVmeterHandler: Thing may not be null.");

        logger.debug("Initialize Hideki UV-meter handler.");

        super.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose UV-meter handler.");
        super.dispose();
        data = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final int[] data) {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiUVmeterHandler: Thing may not be null.");
        if (ThingStatus.ONLINE != thing.getStatus()) {
            return;
        }

        if (TYPE == getDecodedType(data)) {
            super.setData(data); // Decode common parts first
            if (data.length == getDecodedLength()) {
                if (logger.isTraceEnabled()) {
                    final String raw = Arrays.toString(data);
                    logger.trace("Got new UV-meter data: {}.", raw);
                }

                synchronized (this) {
                    if (this.data == null) {
                        this.data = new int[data.length];
                    }
                    System.arraycopy(data, 0, this.data, 0, data.length);
                }

                final Channel tChannel = thing.getChannel(TEMPERATURE);
                updateState(tChannel.getUID(), new DecimalType(getTemperature()));

                final Channel mChannel = thing.getChannel(MED);
                updateState(mChannel.getUID(), new DecimalType(getMED()));

                final Channel uChannel = thing.getChannel(UV_INDEX);
                updateState(uChannel.getUID(), new DecimalType(getUVIndex()));
            } else {
                logger.error("Got wrong UV-meter data length {}.", data.length);
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
        return (data[4] >> 4) + (data[4] & 0x0F) / 10.0 + (data[5] & 0x0F) * 10.0;
    }

    /**
     * Returns decoded MED. MED stay for "minimal erythema dose". Some definitions
     * says: 1 MED/h = 2.778 UV-Index, another 1 MED/h = 2.33 UV-Index
     *
     * @return Decoded MED
     */
    private double getMED() {
        return (data[5] >> 4) / 10.0 + (data[6] & 0x0F) + (data[6] >> 4) * 10.0;
    }

    /**
     * Returns decoded UV-index.
     *
     * @return Decoded UV-index
     */
    private double getUVIndex() {
        return (data[7] >> 4) + (data[7] & 0x0F) / 10.0 + (data[8] & 0x0F) * 10.0;
    }

}
