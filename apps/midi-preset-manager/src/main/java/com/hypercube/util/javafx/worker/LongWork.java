package com.hypercube.util.javafx.worker;

import javafx.concurrent.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class LongWork<T> {
    private final String threadName;
    private final Supplier<T> code;
    @Setter
    private Task<T> task;
}
