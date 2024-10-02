package com.hypercube.workshop.audioworkshop.common.insights.dft;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBufferConsumer;

public interface DFTCalculator extends SampleBufferConsumer {
    java.util.List<DFTResult>[] getMagnitudes();
}
