/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.config;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link PLCPulseConfiguration} is a class for configuration
 * of Siemens LOGO! PLC memory input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCPulseConfiguration extends PLCMemoryConfiguration {

    private String observe;
    private @NonNull Integer pulse = 150;

    /**
     * Get observed Siemens LOGO! block name or memory address.
     *
     * @return Observed Siemens LOGO! block name or memory address
     */
    public @NonNull String getObservedBlock() {
        if (observe == null) {
            observe = getBlockName();
        }
        return observe;
    }

    /**
     * Set Siemens LOGO! block name or memory address to observe.
     *
     * @param name Siemens LOGO! memory block name or memory address
     */
    public void setObservedBlock(final @NonNull String name) {
        Objects.requireNonNull(name, "PLCPulseConfiguration: Observed block name may not be null.");
        this.observe = name;
    }

    public @NonNull String getObservedChannelType() {
        final String kind = getObservedBlockKind();
        return (kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI")) ? DIGITAL_INPUT_ITEM : DIGITAL_OUTPUT_ITEM;
    }

    public @NonNull String getObservedBlockKind() {
        if (observe == null) {
            observe = getBlockName();
        }
        return getBlockKind(observe);
    }

    public @NonNull Integer getPulseLength() {
        return pulse;
    }

    public void setPulseLength(@NonNull Integer pulse) {
        Objects.requireNonNull(pulse, "PLCPulseConfiguration: Pulse length name may not be null.");
        this.pulse = pulse;
    }

}
