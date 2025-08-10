package com.hypercube.workshop.audioworkshop.api.insights.dft;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;

public interface DFTCalculator extends SampleBufferConsumer {
    PCMBufferFormat getFormat();

    java.util.List<DFTResult>[] getMagnitudes();
}
