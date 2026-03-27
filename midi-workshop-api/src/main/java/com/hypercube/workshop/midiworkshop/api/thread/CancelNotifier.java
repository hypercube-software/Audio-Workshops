package com.hypercube.workshop.midiworkshop.api.thread;

import java.util.concurrent.CancellationException;

/**
 * Used to "notify" the worker thread that the UI want to stop
 */
public class CancelNotifier {
    private boolean pendingCancel = false;

    /**
     * Called from the UI
     */
    public void cancel() {
        pendingCancel = true;
    }

    /**
     * called by the worker
     */
    public void checkIfShouldStop() {
        if (pendingCancel) {
            throw new CancellationException();
        }
    }
}
