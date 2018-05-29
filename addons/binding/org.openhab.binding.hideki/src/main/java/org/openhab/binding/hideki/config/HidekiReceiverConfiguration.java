/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki.config;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link PLCLogoBridgeConfiguration} hold configuration of Siemens LOGO! PLCs.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiReceiverConfiguration {

    private @NonNull String receiver;
    private Integer pin;

    private @NonNull Integer refresh = 1;
    private @NonNull Integer timeout = -1;
    private @NonNull String device = "/dev/spidev0.0";
    private @NonNull Integer interrupt = 0;

    public HidekiReceiverConfiguration() {
    }

    public @NonNull String getReceiver() {
        return receiver;
    }

    public void setReceiver(final @NonNull String receiver) {
        Objects.requireNonNull(receiver, "Receiver may not be null");
        this.receiver = receiver.trim();
    }

    /**
     * Get configured GPIO pin receiver connected to.
     *
     * @return Configured GPIO pin
     */
    public @NonNull Integer getGpioPin() {
        return pin;
    }

    /**
     * Set GPIO pin receiver connected to.
     *
     * @param pin GPIO pin receiver connected to
     */
    public void setGpioPin(final @NonNull Integer pin) {
        Objects.requireNonNull(pin, "GPIO pin may not be null");
        this.pin = pin;
    }

    /**
     * Get configured refresh rate for new decoder data check in seconds.
     *
     * @return Configured refresh rate for new decoder data check
     */
    public @NonNull Integer getRefreshRate() {
        return refresh;
    }

    /**
     * Set refresh rate for new decoder data check in seconds.
     *
     * @param rate Refresh rate for new decoder data check
     */
    public void setRefreshRate(final @NonNull Integer rate) {
        Objects.requireNonNull(rate, "Refresh rate may not be null");
        this.refresh = rate;
    }

    /**
     * Get configured wait period for edge on GPIO pin in milliseconds.
     *
     * @return Configured wait period for edge on GPIO pin
     */
    public @NonNull Integer getTimeout() {
        return timeout;
    }

    /**
     * Set wait period for edge detection on GPIO pin in milliseconds.
     * If 0, decoder will return immediately, even if no edge detected.
     * If smaller 0, decoder will wait infinite.
     *
     * @param timeout Wait period for edge on GPIO pin
     */
    public void setTimeout(final @NonNull Integer timeout) {
        Objects.requireNonNull(timeout, "Timeout may not be null");
        this.timeout = timeout;
    }

    public @NonNull String getDevice() {
        return device;
    }

    public void setDevice(final @NonNull String device) {
        Objects.requireNonNull(device, "Communication device may not be null");
        this.device = device.trim();
    }

    public @NonNull Integer getInterrupt() {
        return interrupt;
    }

    public void setInterrupt(final @NonNull Integer interrupt) {
        Objects.requireNonNull(device, "Interrupt pin may not be null");
        this.interrupt = interrupt;
    }

}
