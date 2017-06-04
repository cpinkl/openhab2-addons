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

/**
 * The {@link PLCLogoBridgeConfiguration} hold configuration of Siemens LOGO! PLCs.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiReceiverConfiguration {

    private Integer pin = null;
    private Integer refresh = 100;
    private Integer timeout = 5000;

    public HidekiReceiverConfiguration() {
    }

    /**
     * Get configured GPIO pin receiver connected to.
     *
     * @return Configured GPIO pin
     */
    public Integer getGpioPin() {
        return pin;
    }

    /**
     * Set GPIO pin receiver connected to.
     *
     * @param pin GPIO pin receiver connected to
     */
    public void setGpioPin(final Integer pin) {
        Objects.requireNonNull(pin, "GPIO pin may not be null");
        this.pin = pin;
    }

    /**
     * Get configured refresh rate for new decoder data check in milliseconds.
     *
     * @return Configured refresh rate for new decoder data check
     */
    public Integer getRefreshRate() {
        return refresh;
    }

    /**
     * Set refresh rate for new decoder data check in milliseconds.
     *
     * @param rate Refresh rate for new decoder data check
     */
    public void setRefreshRate(Integer rate) {
        Objects.requireNonNull(rate, "Refresh rate may not be null");
        this.refresh = rate;
    }

    /**
     * Get configured wait period for edge on GPIO pin in milliseconds.
     *
     * @return Configured wait period for edge on GPIO pin
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Set wait period for edge detection on GPIO pin in milliseconds.
     * If 0, decoder will return immediately, even if no edge detected.
     * If smaller 0, decoder will wait infinite.
     *
     * @param timeout Wait period for edge on GPIO pin
     */
    public void setTimeout(Integer timeout) {
        Objects.requireNonNull(timeout, "Timeout may not be null");
        this.timeout = timeout;
    }

}
