package com.hypercube.util.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectBinding;

import java.util.List;

public record PathBinding(String path, ObjectBinding<?> binding, List<InvalidationListener> listeners) {
}
