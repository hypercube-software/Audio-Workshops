package com.hypercube.workshop.midiworkshop.api.ports.remote.msg;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

@Slf4j
public class CloseSessionNetworkMessage extends NetworkMessage {
    public CloseSessionNetworkMessage(NetWorkMessageOrigin origin, long networkId) {
        super(origin, NetworkMessageType.CLOSE_SESSION, networkId);
    }

    @Override
    public void serialize(OutputStream out) {
        try {
            serializeHeader(out);
        } catch (SocketException e) {
            log.warn("Can't send message: {}", e.getMessage());
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
