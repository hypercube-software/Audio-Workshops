package com.hypercube.util.javafx.worker;

public record LongWork(String threadName, Runnable code) {
}
