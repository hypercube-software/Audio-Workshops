package com.hypercube.workshop.audioworkshop.common.insights.dft;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;

public interface DFTCalculator extends SampleBufferConsumer {
    PCMBufferFormat getFormat();

    java.util.List<DFTResult>[] getMagnitudes();
}
