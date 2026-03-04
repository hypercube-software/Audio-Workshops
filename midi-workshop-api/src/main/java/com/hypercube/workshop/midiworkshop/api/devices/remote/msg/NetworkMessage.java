package com.hypercube.workshop.midiworkshop.api.devices.remote.msg;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@AllArgsConstructor
@Getter
public abstract class NetworkMessage {
    private NetWorkMessageOrigin origin;
    private NetworkMessageType type;
    private long networkId;

    public static NetworkMessage deserialize(NetWorkMessageOrigin origin, InputStream in) {
        try {
            NetworkMessageType type = NetworkMessageType.values()[readInt8(in)];
            long networkId = readInt32(in);
            NetworkMessage msg = switch (type) {
                case OPEN_SESSION -> new OpenSesssionNetworkMessage(origin, networkId);
                case CLOSE_SESSION -> new CloseSessionNetworkMessage(origin, networkId);
                case MIDI_EVENT -> new MidiNetworkMessage(origin, networkId);
            };
            msg.completeDeserialize(in);
            return msg;
        } catch (IOException e) {
            throw new MidiConfigError(e);
        }
    }

    private static int readInt8(InputStream in) throws IOException {
        int v = in.read();
        if (v == -1) {
            throw new IOException("Connection lost");
        }
        return v;
    }

    protected static long readInt32(InputStream in) throws IOException {
        long a = in.read() & 0xFFL;
        long b = in.read() & 0xFFL;
        long c = in.read() & 0xFFL;
        long d = in.read() & 0xFFL;
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    protected static int readInt16(InputStream in) throws IOException {
        int a = in.read() & 0xFF;
        int b = in.read() & 0xFF;
        return (a << 8) | b;
    }

    public abstract void serialize(OutputStream out);

    protected void completeDeserialize(InputStream in) throws IOException {
        // does nothing by default
    }

    protected void writeInt32(OutputStream out, long value) throws IOException {
        long a = (value >> 24) & 0xFF;
        long b = (value >> 16) & 0xFF;
        long c = (value >> 8) & 0xFF;
        long d = (value >> 0) & 0xFF;
        out.write((byte) a);
        out.write((byte) b);
        out.write((byte) c);
        out.write((byte) d);
    }

    protected void writeInt16(OutputStream out, long value) throws IOException {
        long a = (value >> 8) & 0xFF;
        long b = (value >> 0) & 0xFF;
        out.write((byte) a);
        out.write((byte) b);
    }

    protected void serializeHeader(OutputStream out) throws IOException {
        out.write(type.ordinal());
        writeInt32(out, networkId);
    }

}
