package com.hypercube.workshop.synthripper;

import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class ThreadLogger {
    private Thread thread;
    private Queue<String> messages = new ConcurrentLinkedDeque<>();
    private boolean running;

    void start() {
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
    }

    public void info(String msg) {
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
            String msg = messages.poll();
            if (msg != null) {
                log.info(msg);
            }
            waitMessage();
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
