/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki.handler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.hideki.config.HidekiReceiverConfiguration;
import org.openhab.binding.hideki.internal.HidekiDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HidekiReceiverHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiReceiverHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(HidekiReceiverHandler.class);

    private Set<HidekiBaseHandler> handlers = new HashSet<HidekiBaseHandler>();
    private HidekiReceiverConfiguration config = getConfigAs(HidekiReceiverConfiguration.class);

    private ScheduledFuture<?> readerJob = null;
    private final Runnable dataReader = new Runnable() {
        @Override
        public void run() {
            final int[] data = HidekiDecoder.getDecodedData();
            if ((data != null) && (data[0] == 0x9F)) {
                synchronized (handlers) {
                    for (HidekiBaseHandler handler : handlers) {
                        if (handler == null) {
                            logger.warn("Skip processing of invalid handler.");
                            continue;
                        }

                        handler.setData(data);
                    }
                }
                // if (logger.isTraceEnabled()) {
                logger.info("Fetched new data: {}.", Arrays.toString(data));
                // }
            }
        }
    };

    public HidekiReceiverHandler(Bridge bridge) {
        super(bridge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (channelUID.getId().equals(CHANNEL_1)) {
        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final Thing thing = getThing();
        Objects.requireNonNull(thing, "HidekiReceiverHandler: Thing may not be null.");

        synchronized (config) {
            config = getConfigAs(HidekiReceiverConfiguration.class);
        }

        logger.debug("Initialize Hideki receiver handler.");

        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                boolean configured = (config.getGpioPin() != null);
                configured = configured && (config.getRefreshRate() != null);
                configured = configured && (config.getTimeout() != null);

                if (configured) {
                    final Integer pin = config.getGpioPin();
                    HidekiDecoder.setTimeOut(config.getTimeout().intValue());
                    HidekiDecoder.setLogFile("/var/log/openhab2/hideki.log");
                    if (HidekiDecoder.startDecoder(pin.intValue()) == 0) {
                        if (readerJob == null) {
                            final Integer interval = config.getRefreshRate();
                            logger.info("Creating new reader job on pin {} with interval {} ms.", pin, interval);
                            readerJob = scheduler.scheduleWithFixedDelay(dataReader, 100, interval,
                                    TimeUnit.MILLISECONDS);
                        }
                        HidekiReceiverHandler.super.initialize();
                    } else {
                        final String message = "Can not start decoder on pin: " + pin.toString() + ".";
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, message);
                    }
                } else {
                    final String message = "Can not initialize decoder. Please, check parameter.";
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose Hideki receiver handler.");
        super.dispose();

        if (readerJob != null) {
            readerJob.cancel(false);
            while (!readerJob.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    logger.error("Dispose Hideki receiver handler throw an error: {}.", exception.getMessage());
                    break;
                }
            }
            readerJob = null;

            HidekiDecoder.stopDecoder(config.getGpioPin().intValue());
            logger.info("Destroy hideki reader job.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);
        if (childHandler instanceof HidekiBaseHandler) {
            final HidekiBaseHandler handler = (HidekiBaseHandler) childHandler;
            synchronized (handlers) {
                final String type = String.format("%02X", handler.getSensorType());
                if (!handlers.contains(handler)) {
                    handlers.add(handler);
                    logger.debug("Insert handler for sensor 0x{}.", type);
                } else {
                    logger.info("Handler {} for sensor 0x{} already registered.", childThing.getUID(), type);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof HidekiBaseHandler) {
            final HidekiBaseHandler handler = (HidekiBaseHandler) childHandler;
            synchronized (handlers) {
                final String type = String.format("%02X", handler.getSensorType());
                if (handlers.contains(handler)) {
                    handlers.remove(handler);
                    logger.debug("Remove handler for sensor 0x{}.", type);
                } else {
                    logger.info("Handler {} for sensor 0x{} already disposed.", childThing.getUID(), type);
                }
            }
        }
        super.childHandlerDisposed(childHandler, childThing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        synchronized (config) {
            config = getConfigAs(HidekiReceiverConfiguration.class);
        }
    }

}
