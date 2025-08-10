package com.hypercube.workshop.audioworkshop.api.insights.signal;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class SignalSegment {
    private long sampleStart;
    private long sampleEnd;
    @EqualsAndHashCode.Include
    private SignalSegmentType type;
    private SignalSegment next;

    public SignalSegment(long sampleStart, long sampleEnd, SignalSegmentType type) {
        this.sampleStart = sampleStart;
        this.sampleEnd = sampleEnd;
        this.type = type;
    }

    public void link(SignalSegment next) {
        this.next = next;
    }

    /**
     * Try to merge current segment with the next one
     *
     * @return last segment
     */
    public SignalSegment tryToMerge() {
        if (next != null)
            if (next.equals(this)) {
                sampleEnd = next.sampleEnd;
                next = next.next;
                return this;
            } else {
                return next;
            }
        else {
            return this;
        }
    }

    /**
     * remove silent in the signal
     */
    public void purge() {
        SignalSegment current = this;
        while (current != null && current.next != null && current.next.next != null) {
            if (current.type == SignalSegmentType.SIGNAL && current.next.type == SignalSegmentType.SILENT && current.next.next.type == SignalSegmentType.SIGNAL) {
                current.next = current.next.next;
                current.tryToMerge();
            } else {
                current = current.next;
            }
        }
    }
}
