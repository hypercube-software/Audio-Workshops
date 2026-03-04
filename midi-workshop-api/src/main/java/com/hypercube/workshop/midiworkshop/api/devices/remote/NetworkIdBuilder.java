package com.hypercube.workshop.midiworkshop.api.devices.remote;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

/**
 * The NetworkID play a central role in the protocol to designate a {@link com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition}
 * <ul>
 *     <li>It makes network packet small, it is a 32 Bits number</li>
 *     <li>It is simply a CRC32 of the device name</li>
 * </ul>
 */
@UtilityClass
@Slf4j
public class NetworkIdBuilder {

    public static long getDeviceNetworkId(String deviceName) {
        CRC32 crc32 = new CRC32();
        try {
            byte[] bytes = deviceName.getBytes("UTF-8");

            crc32.update(bytes, 0, bytes.length);
        } catch (UnsupportedEncodingException e) {
            log.error("Unexpected error", e);
            return -1;
        }

        return crc32.getValue();
    }
}
