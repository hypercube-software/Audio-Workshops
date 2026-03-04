package com.hypercube.workshop.midiworkshop.api.devices.remote.msg;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

import java.io.IOException;
import java.io.OutputStream;

public class CloseSessionNetworkMessage extends NetworkMessage {
    public CloseSessionNetworkMessage(NetWorkMessageOrigin origin, long networkId) {
        super(origin, NetworkMessageType.CLOSE_SESSION, networkId);
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
