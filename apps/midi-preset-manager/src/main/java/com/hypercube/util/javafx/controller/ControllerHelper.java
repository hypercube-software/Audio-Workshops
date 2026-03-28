package com.hypercube.util.javafx.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.InvocationTargetException;

/**
 * This class does the magic to make SceneBuilder happy
 * <ul>
 *     <li>When the widget is created, the FXML will be loaded</li>
 *     <li>The controller bound to this widget is created if springContext is set</li>
 *     <li>When SceneBuilder is used, springContext will be null, so the controller is not created</li>
 *     <li>Finally we bind the widget to the controller using {@link Node#setUserData(Object)}</li>
 * </ul>
 */
@UtilityClass
@Slf4j
public class ControllerHelper {
    @Getter
    @Setter
    private static boolean nonSceneBuilderLaunch = false;

    @Getter
    @Setter
    private static ConfigurableApplicationContext springContext;

    public static <T extends Node> void loadFXML(T widget) {
        loadFXML(widget, widget.getClass()
                .getName() + "Controller");
    }

    /**
     * This method is recursive, since FXML files use other FXML files
     * <p>The recursion occurs when we call {@link FXMLLoader}</p>
     *
     * @param widget     instance of the view widget
     * @param controller class name of the controller
     * @param <T>        type of the view widget
     */
    public static <T extends Node> void loadFXML(T widget, String controller) {
        var viewClass = widget.getClass();
        String fxmlFile = viewClass.getSimpleName() + ".fxml";
        var loader = new FXMLLoader(viewClass.getResource(fxmlFile));
        loader.setControllerFactory(ControllerHelper::forgeController);
        if (!nonSceneBuilderLaunch) {
            System.out.println("==============================================================");
            System.out.println("SceneBuilder context");
            System.out.println("View      : " + widget.getClass()
                    .getName());
            System.out.println("Controller: " + controller);
            System.out.println("==============================================================");
        }
        try {
            loader.setRoot(widget);
            T view = loader.load();
            Controller<T, ?> controllerInstance = loader.getController();
            boundControllerToView(controller, controllerInstance, view);
            assert (view == widget);
        } catch (Exception e) {
            throw new ControllerHelperException("Unable to load " + fxmlFile, e);
        }
    }

    /**
     * WARNING: This method is only called when the view inherit from {@link com.hypercube.util.javafx.view.View}, otherwise you have to call {@link Controller#setView(Node)} yourself
     */
    private static <T extends Node> void boundControllerToView(String controller, Controller<T, ?> controllerInstance, T view) {
        if (controllerInstance.getView() == null) {
            controllerInstance.setView(view);
            view.setUserData(controllerInstance);
            controllerInstance.onViewLoaded();
        } else {
            throw new ControllerHelperException("Your controller '%s' is shared between more than 1 views, this is forbidden.".formatted(controller));
        }
    }

    /**
     * Use Spring to create the controller and wire things together
     * <p>This method is called directly from {@link FXMLLoader}</p>
     */
    @SuppressWarnings("unchecked")
    private static <T extends Node> Controller<T, ?> forgeController(Class<?> type) {
        try {
            String controller = type.getName();
            Controller<T, ?> controllerInstance;
            if (springContext != null) {
                controllerInstance = (Controller<T, ?>) springContext.getBean(Class.forName(controller));
            } else {
                controllerInstance = (Controller<T, ?>) Class.forName(controller)
                        .getConstructor()
                        .newInstance();
            }

            return controllerInstance;
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new ControllerHelperException(e);
        }
    }

    /**
     * Convience method to retreive the controller from a widget (via {@link Node#getUserData()}
     *
     * @param widget The widget
     * @param <T>    The controller class
     * @return The instance of the controller bound to this widget
     */
    public static <T extends Controller> T getController(Node widget) {
        return (T) widget.getUserData();
    }
}
