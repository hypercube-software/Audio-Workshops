package com.hypercube.util.javafx.view.lists;

@FunctionalInterface
public interface SelectionChangedListener<T> {
    void changed(boolean byUser, T oldValue, T newValue);
}
