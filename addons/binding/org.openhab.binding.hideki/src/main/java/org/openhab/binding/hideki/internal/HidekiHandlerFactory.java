/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hideki.internal;

import static org.openhab.binding.hideki.HidekiBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.hideki.handler.HidekiAnemometerHandler;
import org.openhab.binding.hideki.handler.HidekiPluviometerHandler;
import org.openhab.binding.hideki.handler.HidekiReceiverHandler;
import org.openhab.binding.hideki.handler.HidekiThermometerHandler;
import org.openhab.binding.hideki.handler.HidekiUVmeterHandler;

/**
 * The {@link HidekiHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class HidekiHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>();

    /**
     * Constructor.
     */
    public HidekiHandlerFactory() {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_RECEIVER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_THERMOMETER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_PLUVIOMETER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ANEMOMETER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_UVMETER);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        if (THING_TYPE_RECEIVER.equals(thing.getThingTypeUID()) && (thing instanceof Bridge)) {
            return new HidekiReceiverHandler((Bridge) thing);
        } else if (THING_TYPE_THERMOMETER.equals(thing.getThingTypeUID())) {
            return new HidekiThermometerHandler(thing);
        } else if (THING_TYPE_PLUVIOMETER.equals(thing.getThingTypeUID())) {
            return new HidekiPluviometerHandler(thing);
        } else if (THING_TYPE_ANEMOMETER.equals(thing.getThingTypeUID())) {
            return new HidekiAnemometerHandler(thing);
        } else if (THING_TYPE_UVMETER.equals(thing.getThingTypeUID())) {
            return new HidekiUVmeterHandler(thing);
        }

        return null;
    }
}
