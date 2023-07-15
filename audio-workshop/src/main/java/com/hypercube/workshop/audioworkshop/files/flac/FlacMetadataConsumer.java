package com.hypercube.workshop.audioworkshop.files.flac;

import com.hypercube.workshop.audioworkshop.files.flac.meta.FlacMetadata;
import org.springframework.util.function.ThrowingConsumer;

@FunctionalInterface
public interface FlacMetadataConsumer extends ThrowingConsumer<FlacMetadata> {
}
