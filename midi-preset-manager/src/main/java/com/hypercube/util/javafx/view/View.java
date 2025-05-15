package com.hypercube.util.javafx.view;

import com.hypercube.util.javafx.controller.Controller;
import javafx.scene.Node;

/**
 * This interface enforce the link between a {@link Node} and a {@link Controller}
 * <p>Thereforce generated {@link Node} will implement this interface for convenience
 *
 * @param <C> Type used for the controller bound to this widget
 */
public interface View<C extends Controller<?, ?>> {
    C getController();
}
