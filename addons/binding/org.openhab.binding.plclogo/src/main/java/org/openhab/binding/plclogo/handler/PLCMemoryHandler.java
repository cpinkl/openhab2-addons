/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.handler;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.plclogo.internal.PLCLogoClient;
import org.openhab.binding.plclogo.internal.config.PLCMemoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The {@link PLCMemoryHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCMemoryHandler extends PLCCommonHandler {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_MEMORY);

    private final Logger logger = LoggerFactory.getLogger(PLCMemoryHandler.class);
    private PLCMemoryConfiguration config = getConfigAs(PLCMemoryConfiguration.class);

    /**
     * Constructor.
     */
    public PLCMemoryHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (!isThingOnline()) {
            return;
        }

        final Channel channel = thing.getChannel(channelUID.getId());
        Objects.requireNonNull(channel, "PLCMemoryHandler: Invalid channel found.");

        final @NonNull PLCLogoClient client = getLogoClient();
        final @NonNull String name = getBlockFromChannel(channel);

        final int address = getAddress(name);
        if (address != INVALID) {
            final String kind = getBlockKind();
            final String type = channel.getAcceptedItemType();
            if (command instanceof RefreshType) {
                final byte[] buffer = new byte[getBufferLength()];
                int result = client.readDBArea(1, 0, buffer.length, S7Client.S7WLByte, buffer);
                if (result == 0) {
                    if ((type != null) && type.equalsIgnoreCase(DIGITAL_OUTPUT_ITEM) && kind.equalsIgnoreCase("VB")) {
                        boolean value = S7.GetBitAt(buffer, address, getBit(name));
                        updateState(channelUID, value ? OnOffType.ON : OnOffType.OFF);
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VB")) {
                        int value = buffer[address];
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VW")) {
                        int value = S7.GetShortAt(buffer, address);
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VD")) {
                        int value = S7.GetDIntAt(buffer, address);
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    } else {
                        logger.warn("Channel {} will not accept {} items.", channelUID, type);
                    }
                } else {
                    logger.warn("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else if (command instanceof DecimalType) {
                final int length = kind.equalsIgnoreCase("VB") ? 1 : 2;
                final byte[] buffer = new byte[kind.equalsIgnoreCase("VD") ? 4 : length];
                if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VB")) {
                    buffer[0] = ((DecimalType) command).byteValue();
                } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VW")) {
                    S7.SetShortAt(buffer, 0, ((DecimalType) command).intValue());
                } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VD")) {
                    S7.SetDIntAt(buffer, 0, ((DecimalType) command).intValue());
                } else {
                    logger.warn("Channel {} will not accept {} items.", channelUID, type);
                }
                int result = client.writeDBArea(1, address, buffer.length, S7Client.S7WLByte, buffer);
                if (result != 0) {
                    logger.warn("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else if (command instanceof OnOffType) {
                final byte[] buffer = new byte[1];
                if ((type != null) && type.equalsIgnoreCase(DIGITAL_OUTPUT_ITEM) && kind.equalsIgnoreCase("VB")) {
                    S7.SetBitAt(buffer, 0, 0, ((OnOffType) command) == OnOffType.ON);
                } else {
                    logger.warn("Channel {} will not accept {} items.", channelUID, type);
                }
                int result = client.writeDBArea(1, 8 * address + getBit(name), buffer.length, S7Client.S7WLBit, buffer);
                if (result != 0) {
                    logger.warn("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Channel {} received not supported command {}.", channelUID, command);
            }
        }
    }

    @Override
    public void setData(final byte[] data) {
        if (!isThingOnline()) {
            return;
        }

        if (data.length != getBufferLength()) {
            logger.warn("Received and configured data sizes does not match.");
            return;
        }

        final List<Channel> channels = thing.getChannels();
        if (channels.size() != getNumberOfChannels()) {
            logger.warn("Received and configured channels sizes does not match.");
            return;
        }

        for (final Channel channel : channels) {
            final @NonNull ChannelUID channelUID = channel.getUID();
            final @NonNull String name = getBlockFromChannel(channel);

            final int address = getAddress(name);
            if (address != INVALID) {
                final String kind = getBlockKind();
                final String type = channel.getAcceptedItemType();
                final Boolean force = config.isUpdateForced();

                if ((type != null) && type.equalsIgnoreCase(DIGITAL_OUTPUT_ITEM) && kind.equalsIgnoreCase("VB")) {
                    final OnOffType state = (OnOffType) getOldValue(name);
                    final OnOffType value = S7.GetBitAt(data, address, getBit(name)) ? OnOffType.ON : OnOffType.OFF;
                    if ((state == null) || (value != state) || force) {
                        updateState(channelUID, value);
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    }
                } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VB")) {
                    final Integer threshold = config.getThreshold();
                    final DecimalType state = (DecimalType) getOldValue(name);
                    int value = data[address];
                    if ((state == null) || (Math.abs(value - state.intValue()) > threshold) || force) {
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    }
                } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VW")) {
                    final Integer threshold = config.getThreshold();
                    final DecimalType state = (DecimalType) getOldValue(name);
                    int value = S7.GetShortAt(data, address);
                    if ((state == null) || (Math.abs(value - state.intValue()) > threshold) || force) {
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    }
                } else if ((type != null) && type.equalsIgnoreCase(ANALOG_ITEM) && kind.equalsIgnoreCase("VD")) {
                    final Integer threshold = config.getThreshold();
                    final DecimalType state = (DecimalType) getOldValue(name);
                    int value = S7.GetDIntAt(data, address);
                    if ((state == null) || (Math.abs(value - state.intValue()) > threshold) || force) {
                        updateState(channelUID, new DecimalType(value));
                        logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
                    }
                } else {
                    logger.warn("Channel {} will not accept {} items.", channelUID, type);
                }
            } else {
                logger.warn("Invalid channel {} found.", channelUID);
            }
        }
    }

    @Override
    protected void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);

        final Channel channel = thing.getChannel(channelUID.getId());
        Objects.requireNonNull(channel, "PLCMemoryHandler: Invalid channel found.");

        setOldValue(getBlockFromChannel(channel), state);
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        synchronized (config) {
            config = getConfigAs(PLCMemoryConfiguration.class);
        }
    }

    @Override
    protected boolean isValid(final @NonNull String name) {
        if (3 <= name.length() && (name.length() <= 7)) {
            final String kind = getBlockKind();
            if (Character.isDigit(name.charAt(2))) {
                boolean valid = kind.equalsIgnoreCase("VB") || kind.equalsIgnoreCase("VW");
                return name.startsWith(kind) && (valid || kind.equalsIgnoreCase("VD"));
            }
        }
        return false;
    }

    @Override
    protected @NonNull String getBlockKind() {
        return config.getBlockKind();
    }

    @Override
    protected int getNumberOfChannels() {
        return 1;
    }

    @Override
    protected void doInitialization() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "PLCMemoryHandler: Thing may not be null.");

        logger.debug("Initialize LOGO! {} memory handler.");

        synchronized (config) {
            config = getConfigAs(PLCMemoryConfiguration.class);
        }

        super.doInitialization();
        if (ThingStatus.OFFLINE != thing.getStatus()) {
            final String kind = getBlockKind();
            final String name = config.getBlockName();
            boolean isDigital = kind.equalsIgnoreCase("VB") && (getBit(name) != INVALID);
            String text = isDigital ? "Digital" : "Analog";

            ThingBuilder tBuilder = editThing();
            tBuilder.withLabel(getBridge().getLabel() + ": " + text.toLowerCase() + " in/output");

            final String type = config.getChannelType();
            final ChannelUID uid = new ChannelUID(thing.getUID(), isDigital ? STATE_CHANNEL : VALUE_CHANNEL);
            ChannelBuilder cBuilder = ChannelBuilder.create(uid, type);
            cBuilder.withType(new ChannelTypeUID(BINDING_ID, type.toLowerCase()));
            cBuilder.withLabel(name);
            cBuilder.withDescription(text + " in/output block " + name);
            cBuilder.withProperties(ImmutableMap.of(BLOCK_PROPERTY, name));
            tBuilder.withChannel(cBuilder.build());
            setOldValue(name, null);

            updateThing(tBuilder.build());
            updateStatus(ThingStatus.ONLINE);
        }
    }

    /**
     * Calculate bit within address for block with given name.
     *
     * @param name Name of the LOGO! block
     * @return Calculated bit
     */
    private int getBit(final @NonNull String name) {
        int bit = INVALID;

        logger.debug("Get bit of {} LOGO! for block {} .", getLogoFamily(), name);

        if (isValid(name) && (getAddress(name) != INVALID)) {
            final String[] parts = name.trim().split("\\.");
            if (parts.length > 1) {
                bit = Integer.parseInt(parts[1]);
            }
        } else {
            logger.warn("Wrong configurated LOGO! block {} found.", name);
        }

        return bit;
    }

}
