package com.hypercube.workshop.audioworkshop.common.consumer;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SampleBufferConsumerChain implements SampleBufferConsumer {
    final List<SampleBufferConsumer> chain;

    @Override
    public void reset() {
        chain.forEach(SampleBufferConsumer::reset);
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        chain.forEach(sc -> sc.onBuffer(samples, nbSamples, nbChannels));
    }
}
