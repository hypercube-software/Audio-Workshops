package com.hypercube.util.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class BindingManager implements Closeable {
    private Map<String, PathBinding> pathListeners = new HashMap<>();
    private final Object rootObject;

    public void observePath(String path, InvalidationListener listener) {
        String[] pathElements = path.split("\\.");
        PathBinding pathBinding = pathListeners.get(path);
        if (pathBinding == null) {
            ObjectBinding<?> binding = Bindings.select(rootObject, pathElements);
            pathBinding = new PathBinding(path, binding, new ArrayList<>());
            pathListeners.put(path, pathBinding);
        }
        pathBinding.listeners()
                .add(listener);
        pathBinding.binding()
                .addListener(listener);

        // if the path is valid, notify all listeners
        if (pathBinding.binding()
                .isValid()) {
            pathBinding.binding()
                    .invalidate();
        }
    }

    /**
     * Since observable JavaFX properties are auto-generated, we use this method to reach them dynamically
     */
    public <P> P resolvePropertyPath(String path) {
        if (path == null) {
            return null;
        }
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(path + "Property");
        try {
            return (P) expression.getValue(rootObject);
        } catch (SpelEvaluationException e) {
            log.warn("Unable to resolve path " + path, e);
            return null;
        }
    }


    @Override
    public void close() throws IOException {
        pathListeners.keySet()
                .forEach(path -> removeListeners(path));
    }

    private void removeListeners(String path) {
        PathBinding pl = pathListeners.get(path);
        pl.listeners()
                .forEach(l -> pl.binding()
                        .removeListener(l));
        pathListeners.remove(path);
    }
}
