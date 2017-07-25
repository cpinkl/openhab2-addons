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
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
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
import org.openhab.binding.plclogo.internal.config.PLCPulseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The {@link PLCPulseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCPulseHandler extends PLCCommonHandler {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_PULSE);

    private final Logger logger = LoggerFactory.getLogger(PLCPulseHandler.class);
    private PLCPulseConfiguration config = getConfigAs(PLCPulseConfiguration.class);

    /**
     * Constructor.
     */
    public PLCPulseHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (!isThingOnline()) {
            return;
        }

        final Channel channel = thing.getChannel(channelUID.getId());
        Objects.requireNonNull(channel, "PLCPulseHandler: Invalid channel found.");

        final @NonNull PLCLogoClient client = getLogoClient();
        final @NonNull String name = getBlockFromChannel(channel);

        final int bit = getBit(name);
        final int address = getAddress(name);
        if ((address != INVALID) && (bit != INVALID)) {
            final byte[] buffer = new byte[1];
            if (command instanceof RefreshType) {
                int result = client.readDBArea(1, address, buffer.length, S7Client.S7WLByte, buffer);
                if (result == 0) {
                    updateChannel(channel, S7.GetBitAt(buffer, 0, bit));
                } else {
                    logger.warn("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else if ((command instanceof OpenClosedType) || (command instanceof OnOffType)) {
                final String type = channel.getAcceptedItemType();
                if ((type != null) && type.equalsIgnoreCase(DIGITAL_INPUT_ITEM)) {
                    S7.SetBitAt(buffer, 0, 0, ((OpenClosedType) command) == OpenClosedType.CLOSED);
                } else if ((type != null) && type.equalsIgnoreCase(DIGITAL_OUTPUT_ITEM)) {
                    S7.SetBitAt(buffer, 0, 0, ((OnOffType) command) == OnOffType.ON);
                } else {
                    logger.warn("Channel {} will not accept {} items.", channelUID, type);
                }
                int result = client.writeDBArea(1, 8 * address + bit, buffer.length, S7Client.S7WLBit, buffer);
                if (result != 0) {
                    logger.warn("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Channel {} received not supported command {}.", channelUID, command);
            }
        } else {
            logger.warn("Invalid channel {} found.", channelUID);
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

            final int bit = getBit(name);
            final int address = getAddress(name);
            if ((address != INVALID) && (bit != INVALID)) {
                final DecimalType state = (DecimalType) getOldValue(channelUID.getId());
                if (STATE_CHANNEL.equalsIgnoreCase(channelUID.getId())) {
                    boolean value = S7.GetBitAt(data, address - getBase(name), bit);
                    if ((state == null) || ((value ? 1 : 0) != state.intValue())) {
                        updateChannel(channel, value);
                    }
                } else if (OBSERVE_CHANNEL.equalsIgnoreCase(channelUID.getId())) {
                    handleCommand(channelUID, RefreshType.REFRESH);
                    if ((state != null) && !state.equals(getOldValue(channelUID.getId()))) {
                        final Integer pulse = config.getPulseLength();
                        scheduler.schedule(new Runnable() {
                            private final @NonNull String name = config.getBlockName();
                            private final @NonNull PLCLogoClient client = getLogoClient();

                            @Override
                            public void run() {
                                final byte[] data = new byte[1];
                                S7.SetBitAt(data, 0, 0, state.intValue() == 1 ? true : false);
                                final int address = 8 * getAddress(name) + getBit(name);
                                int result = client.writeDBArea(1, address, data.length, S7Client.S7WLBit, data);
                                if (result == 0) {
                                    setOldValue(channelUID.getId(), null);
                                } else {
                                    logger.warn("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                                }
                            }
                        }, pulse.longValue(), TimeUnit.MILLISECONDS);
                    }
                } else {
                    logger.warn("Invalid channel {} found.", channelUID);
                }
            } else {
                logger.warn("Invalid channel {} found.", channelUID);
            }
        }

        /*
         * if (logger.isTraceEnabled()) {
         * final String raw = "[" + Integer.toBinaryString((data[0] & 0xFF) + 0x100).substring(1) + "]";
         * logger.trace("Channel {} accepting {} received {}.", channel.getUID(), type, raw);
         * }
         */
    }

    @Override
    protected void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);
        DecimalType value = state.as(DecimalType.class);
        if (state instanceof OpenClosedType) {
            final OpenClosedType type = (OpenClosedType) state;
            value = new DecimalType(type == OpenClosedType.CLOSED ? 1 : 0);
        }

        final Channel channel = thing.getChannel(channelUID.getId());
        Objects.requireNonNull(channel, "PLCPulseHandler: Invalid channel found.");

        setOldValue(channelUID.getId(), value);
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        synchronized (config) {
            config = getConfigAs(PLCPulseConfiguration.class);
        }
    }

    @Override
    protected boolean isValid(final @NonNull String name) {
        if (3 <= name.length() && (name.length() <= 7)) {
            final String kind = config.getObservedBlockKind();
            if (Character.isDigit(name.charAt(2))) {
                final String bKind = getBlockKind();
                boolean valid = kind.equalsIgnoreCase("NI") || kind.equalsIgnoreCase("NQ");
                valid = name.startsWith(kind) && (valid || kind.equalsIgnoreCase("VB"));
                return (name.startsWith(bKind) && bKind.equalsIgnoreCase("VB")) || valid;
            } else if (Character.isDigit(name.charAt(1))) {
                boolean valid = kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("Q");
                return name.startsWith(kind) && (valid || kind.equalsIgnoreCase("M"));
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
        return 2;
    }

    @Override
    protected int getAddress(final @NonNull String name) {
        int address = super.getAddress(name);
        if (address != INVALID) {
            final int base = getBase(name);
            if (base != 0) {
                address = base + (address - 1) / 8;
            }
        } else {
            logger.warn("Wrong configurated LOGO! block {} found.", name);
        }
        return address;
    }

    @Override
    protected void doInitialization() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "PLCPulseHandler: Thing may not be null.");

        logger.debug("Initialize LOGO! {} pulse handler.");

        synchronized (config) {
            config = getConfigAs(PLCPulseConfiguration.class);
        }

        super.doInitialization();
        if (ThingStatus.OFFLINE != thing.getStatus()) {
            ThingBuilder tBuilder = editThing();
            tBuilder.withLabel(getBridge().getLabel() + ": digital pulse in/output");

            final String bName = config.getBlockName();
            final String bType = config.getChannelType();
            final ChannelUID uid = new ChannelUID(thing.getUID(), STATE_CHANNEL);
            ChannelBuilder cBuilder = ChannelBuilder.create(uid, bType);
            cBuilder.withType(new ChannelTypeUID(BINDING_ID, bType.toLowerCase()));
            cBuilder.withLabel(bName);
            cBuilder.withDescription("Control block " + bName);
            cBuilder.withProperties(ImmutableMap.of(BLOCK_PROPERTY, bName));
            tBuilder.withChannel(cBuilder.build());
            setOldValue(STATE_CHANNEL, null);

            final String oName = config.getObservedBlock();
            final String oType = config.getObservedChannelType();
            cBuilder = ChannelBuilder.create(new ChannelUID(thing.getUID(), OBSERVE_CHANNEL), oType);
            cBuilder.withType(new ChannelTypeUID(BINDING_ID, oType.toLowerCase()));
            cBuilder.withLabel(oName);
            cBuilder.withDescription("Observed block " + oName);
            cBuilder.withProperties(ImmutableMap.of(BLOCK_PROPERTY, oName));
            tBuilder.withChannel(cBuilder.build());
            setOldValue(OBSERVE_CHANNEL, null);

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
            } else if (parts.length == 1) {
                if (Character.isDigit(parts[0].charAt(1))) {
                    bit = Integer.parseInt(parts[0].substring(1));
                } else if (Character.isDigit(parts[0].charAt(2))) {
                    bit = Integer.parseInt(parts[0].substring(2));
                } else if (Character.isDigit(parts[0].charAt(3))) {
                    bit = Integer.parseInt(parts[0].substring(3));
                }
                bit = (bit - 1) % 8;
            }
        } else {
            logger.warn("Wrong configurated LOGO! block {} found.", name);
        }

        return bit;
    }

    private void updateChannel(final @NonNull Channel channel, boolean value) {
        final ChannelUID channelUID = channel.getUID();
        Objects.requireNonNull(channelUID, "PLCPulseHandler: Invalid channel uid found.");

        final String type = channel.getAcceptedItemType();
        if ((type != null) && type.equalsIgnoreCase(DIGITAL_INPUT_ITEM)) {
            updateState(channelUID, value ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
            logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
        } else if ((type != null) && type.equalsIgnoreCase(DIGITAL_OUTPUT_ITEM)) {
            updateState(channelUID, value ? OnOffType.ON : OnOffType.OFF);
            logger.debug("Channel {} accepting {} was set to {}.", channelUID, type, value);
        } else {
            logger.warn("Channel {} will not accept {} items.", channelUID, type);
        }
    }

}
