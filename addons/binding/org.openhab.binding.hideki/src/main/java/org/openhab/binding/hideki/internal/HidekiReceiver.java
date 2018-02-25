/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki.internal;

import org.eclipse.jdt.annotation.NonNull;

public class HidekiReceiver {

    // forbid object construction
    private HidekiReceiver() {
    }

    public enum Kind {
        RXB,
        CC1101
    }

    public HidekiReceiver(final Kind kind, final @NonNull String device, final @NonNull Integer interrupt) {
        create(kind.ordinal(), device, interrupt.intValue());
    }

    public int getId() {
        return System.identityHashCode(this);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    private native void create(int kind, @NonNull String device, int interrupt);

    private native void destroy();
}
