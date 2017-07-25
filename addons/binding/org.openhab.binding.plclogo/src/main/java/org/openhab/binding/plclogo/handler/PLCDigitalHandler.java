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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.openhab.binding.plclogo.internal.config.PLCDigitalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The {@link PLCDigitalHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCDigitalHandler extends PLCCommonHandler {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DIGITAL);

    private final Logger logger = LoggerFactory.getLogger(PLCDigitalHandler.class);
    private PLCDigitalConfiguration config = getConfigAs(PLCDigitalConfiguration.class);

    private static final Map<String, Integer> LOGO_BLOCKS_0BA7 = ImmutableMap.of(
    // @formatter:off
        "I", 24, // 24 digital inputs
        "Q", 16, // 16 digital outputs
        "M", 27  // 27 digital markers
    // @formatter:on
    );

    private static final Map<String, Integer> LOGO_BLOCKS_0BA8 = ImmutableMap.of(
    // @formatter:off
         "I", 24, // 24 digital inputs
         "Q", 20, // 20 digital outputs
         "M", 64, // 64 digital markers
        "NI", 64, // 64 network inputs
        "NQ", 64  // 64 network outputs
    // @formatter:on
    );

    private static final Map<String, Map<String, Integer>> LOGO_BLOCK_NUMBER = ImmutableMap.of(
    // @formatter:off
        LOGO_0BA7, LOGO_BLOCKS_0BA7,
        LOGO_0BA8, LOGO_BLOCKS_0BA8
    // @formatter:on
    );

    /**
     * Constructor.
     */
    public PLCDigitalHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (!isThingOnline()) {
            return;
        }

        final Channel channel = thing.getChannel(channelUID.getId());
        Objects.requireNonNull(channel, "PLCDigitalHandler: Invalid channel found.");

        final @NonNull PLCLogoClient client = getLogoClient();
        final @NonNull String name = getBlockFromChannel(channel);

        final int bit = getBit(name);
        final int address = getAddress(name);
        if ((address != INVALID) && (bit != INVALID)) {
            if (command instanceof RefreshType) {
                final int base = getBase(name);
                final byte[] buffer = new byte[getBufferLength()];
                int result = client.readDBArea(1, base, buffer.length, S7Client.S7WLByte, buffer);
                if (result == 0) {
                    updateChannel(channel, S7.GetBitAt(buffer, address - base, bit));
                } else {
                    logger.warn("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else if ((command instanceof OpenClosedType) || (command instanceof OnOffType)) {
                final byte[] buffer = new byte[1];
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
                final DecimalType state = (DecimalType) getOldValue(name);
                boolean value = S7.GetBitAt(data, address - getBase(name), bit);
                if ((state == null) || ((value ? 1 : 0) != state.intValue()) || config.isUpdateForced()) {
                    updateChannel(channel, value);
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
        Objects.requireNonNull(channel, "PLCDigitalHandler: Invalid channel found.");

        setOldValue(getBlockFromChannel(channel), value);
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        synchronized (config) {
            config = getConfigAs(PLCDigitalConfiguration.class);
        }
    }

    @Override
    protected boolean isValid(final @NonNull String name) {
        if (2 <= name.length() && (name.length() <= 4)) {
            final String kind = getBlockKind();
            if (Character.isDigit(name.charAt(1)) || Character.isDigit(name.charAt(2))) {
                boolean valid = kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI");
                valid = valid || kind.equalsIgnoreCase("Q") || kind.equalsIgnoreCase("NQ");
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
        final String kind = getBlockKind();
        final String family = getLogoFamily();
        logger.debug("Get block number of {} LOGO! for {} blocks.", family, kind);

        return LOGO_BLOCK_NUMBER.get(family).get(kind).intValue();
    }

    @Override
    protected int getAddress(final @NonNull String name) {
        int address = super.getAddress(name);
        if (address != INVALID) {
            address = getBase(name) + (address - 1) / 8;
        } else {
            logger.warn("Wrong configurated LOGO! block {} found.", name);
        }
        return address;
    }

    @Override
    protected void doInitialization() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "PLCDigitalHandler: Thing may not be null.");

        logger.debug("Initialize LOGO! digital input blocks handler.");

        synchronized (config) {
            config = getConfigAs(PLCDigitalConfiguration.class);
        }

        super.doInitialization();
        if (ThingStatus.OFFLINE != thing.getStatus()) {
            final String kind = getBlockKind();
            final String text = kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI") ? "input" : "output";

            ThingBuilder tBuilder = editThing();
            tBuilder.withLabel(getBridge().getLabel() + ": digital " + text + "s");

            final String type = config.getChannelType();
            for (int i = 0; i < getNumberOfChannels(); i++) {
                final String name = kind + String.valueOf(i + 1);
                final ChannelUID uid = new ChannelUID(thing.getUID(), name);
                ChannelBuilder cBuilder = ChannelBuilder.create(uid, type);
                cBuilder.withType(new ChannelTypeUID(BINDING_ID, type.toLowerCase()));
                cBuilder.withLabel(name);
                cBuilder.withDescription("Digital " + text + " block " + name);
                cBuilder.withProperties(ImmutableMap.of(BLOCK_PROPERTY, name));
                tBuilder.withChannel(cBuilder.build());
                setOldValue(name, null);
            }

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
            if (Character.isDigit(name.charAt(1))) {
                bit = Integer.parseInt(name.substring(1));
            } else if (Character.isDigit(name.charAt(2))) {
                bit = Integer.parseInt(name.substring(2));
            }
            bit = (bit - 1) % 8;
        } else {
            logger.warn("Wrong configurated LOGO! block {} found.", name);
        }

        return bit;
    }

    private void updateChannel(final @NonNull Channel channel, boolean value) {
        final ChannelUID channelUID = channel.getUID();
        Objects.requireNonNull(channelUID, "PLCDigitalHandler: Invalid channel uid found.");

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
