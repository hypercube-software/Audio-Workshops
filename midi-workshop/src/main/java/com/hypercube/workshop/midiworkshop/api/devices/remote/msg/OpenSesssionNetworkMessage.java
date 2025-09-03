package com.hypercube.workshop.midiworkshop.api.devices.remote.msg;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

import java.io.IOException;
import java.io.OutputStream;

public class OpenSesssionNetworkMessage extends NetworkMessage {
    public OpenSesssionNetworkMessage(NetWorkMessageOrigin origin, long networkId) {
        super(origin, NetworkMessageType.OPEN_SESSION, networkId);
    }

    @Override
    public void serialize(OutputStream out) {
        try {
            serializeHeader(out);
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
