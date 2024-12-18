package com.hypercube.workshop.synthripper.log;

import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This class allow us to log in a slow priority thread to no slow down the recording thread
 */
@Slf4j
public class ThreadLogger {
    private Thread thread;
    private Queue<String> messages = new ConcurrentLinkedDeque<>();
    private boolean running;

    public void start() {
        thread = new Thread(this::threadLoop);
        thread.setPriority(Thread.MIN_PRIORITY);
        running = true;
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        drain();
    }

    public void log(String msg) {
        messages.add(msg);
        notifyMessage();
    }

    private void notifyMessage() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void threadLoop() {
        while (running) {
            drain();
            waitMessage();
        }
    }

    private void drain() {
        for (; ; ) {
            String msg = messages.poll();
            if (msg != null) {
                log.info(msg);
            } else {
                break;
            }
        }
    }

    private void waitMessage() {
        try {
            synchronized (this) {
                this.wait(1000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
