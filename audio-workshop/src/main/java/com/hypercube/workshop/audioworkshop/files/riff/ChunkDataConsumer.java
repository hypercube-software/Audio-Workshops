package com.hypercube.workshop.audioworkshop.files.riff;

import java.nio.ByteBuffer;

public interface ChunkDataConsumer {
    public void onNewBuffer(ByteBuffer data, int nbRead);
}
