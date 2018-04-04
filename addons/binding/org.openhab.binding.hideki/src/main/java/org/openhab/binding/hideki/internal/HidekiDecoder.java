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

public class HidekiDecoder {

    final HidekiReceiver receiver;

    // forbid object construction
    private HidekiDecoder() {
        receiver = null;
    }

    public HidekiDecoder(final @NonNull HidekiReceiver receiver, int pin) {
        this.receiver = receiver;
        create(this.receiver.getId(), pin);
    }

    public int getId() {
        return System.identityHashCode(this);
    }

    public native void setTimeOut(int timeout);

    public native boolean start();

    public native boolean stop();

    public native int[] getDecodedData();

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    private native void create(int receiver, int pin);

    private native void destroy();
}
