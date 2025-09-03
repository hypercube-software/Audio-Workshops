package com.hypercube.workshop.midiworkshop.api.devices.remote.msg;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Getter
public class MidiNetworkMessage extends NetworkMessage {
    private CustomMidiEvent event;
    private long packetId;
    private long timeStamp;

    public MidiNetworkMessage(NetWorkMessageOrigin origin, long networkId, long packetId, long timeStamp, CustomMidiEvent event) {
        super(origin, NetworkMessageType.MIDI_EVENT, networkId);
        this.packetId = packetId;
        this.timeStamp = timeStamp;
        this.event = event;
    }

    MidiNetworkMessage(NetWorkMessageOrigin origin, long networkId) {
        super(origin, NetworkMessageType.MIDI_EVENT, networkId);
    }

    @Override
    public void serialize(OutputStream out) {
        try {
            serializeHeader(out);
            byte[] data = event.getMessage()
                    .getMessage();
            int size = event.getMessage()
                    .getLength();
            writeInt32(out, packetId);
            writeInt32(out, timeStamp);
            writeInt16(out, size);
            out.write(data);

        } catch (IOException e) {
            throw new MidiError(e);
        }
    }

    @Override
    protected void completeDeserialize(InputStream in) throws IOException {
        super.completeDeserialize(in);
        packetId = readInt32(in);
        timeStamp = readInt32(in);
        int size = readInt16(in);
        byte[] data = in.readNBytes(size);
        event = SysExBuilder.forgeCustomMidiEvent(data, timeStamp);
    }
}
