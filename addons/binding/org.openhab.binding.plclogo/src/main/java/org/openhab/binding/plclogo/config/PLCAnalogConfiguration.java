/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.config;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.ANALOG_ITEM;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link PLCAnalogConfiguration} is a class for configuration
 * of Siemens LOGO! PLC analog input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCAnalogConfiguration extends PLCDigitalConfiguration {

    private @NonNull Integer threshold = 0;

    /**
     * Get Siemens LOGO! blocks update threshold.
     *
     * @return Configured Siemens LOGO! update threshold
     */
    public @NonNull Integer getThreshold() {
        return threshold;
    }

    /**
     * Set Siemens LOGO! blocks update threshold.
     *
     * @param force Force update of Siemens LOGO! blocks
     */
    public void setThreshold(final @NonNull Integer threshold) {
        Objects.requireNonNull(threshold, "PLCAnalogConfiguration: Threshold may not be null.");
        this.threshold = threshold;
    }

    @Override
    public @NonNull String getChannelType() {
        return ANALOG_ITEM;
    }

}
