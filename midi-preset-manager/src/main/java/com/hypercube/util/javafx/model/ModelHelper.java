package com.hypercube.util.javafx.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This class generate an observable model from a domain model.
 * <ul>
 *     <li>In some way, we create a ViewModel in the sense of MVVM from a Domain model (POJO)</li>
 *     <li>This whitchery relies on the mighty ByteBuddy</li>
 * </ul>
 * What is this observable model ?
 * <ul>
 *     <li>All Lists are wrapped inside an {@link ObservableList}</li>
 *     <li>All fields will be bound to a JavaFX property</li>
 *     <li>We generate an observable object for each class of the domain</li>
 *     <li>The observable class inherits from the domain class</li>
 * </ul>
 */
@Slf4j
public class ModelHelper {
    private static SetterInterceptor setterInterceptor = new SetterInterceptor();
    private static GetterInterceptor getterInterceptor = new GetterInterceptor();
    private static GetterPropertyInterceptor getterPropertyInterceptor = new GetterPropertyInterceptor();
    private static final Map<String, Field> assignedFields = new HashMap<>();
    private static final Map<String, Method> propertySetters = new HashMap<>();
    private static final Map<String, Method> observableSetters = new HashMap<>();
    private static final Map<String, Field> observableProperties = new HashMap<>();
    private static final Map<Class, Class> observableModelClasses = new HashMap<>();

    public static <T> T forgeMMVM(T model) {
        try {
            if (model == null) {
                return null;
            } else if (model.getClass()
                    .getName()
                    .contains("ByteBuddy")) {
                return model; // already observable
            } else if (model instanceof String || model instanceof Boolean || model instanceof Integer || model instanceof Long || model instanceof Float || model instanceof Double || model.getClass()
                    .isEnum()) {
                return model;
            }
            //log.info("forgeMMVM(" + model.getClass().getName() + ")");
            Class<T> modelClass = (Class<T>) model.getClass();
            Class<T> observableModelClass = observableModelClasses.get(modelClass);
            if (observableModelClass == null) {
                if (!hasDefaultConstructor(modelClass)) {
                    throw new ModelHelperException("No default constructor for class " + modelClass.getName());
                }
                DynamicType.Builder<T> builder = new ByteBuddy()
                        .subclass(modelClass);
                for (Field field : modelClass
                        .getDeclaredFields()) {
                    if (field.getAnnotation(NotObservable.class) == null) {
                        builder = forgeObservableProperty(field, builder);
                    }
                }
                DynamicType.Unloaded unloadedType = builder.make();
                observableModelClass = unloadedType.load(modelClass
                                .getClassLoader())
                        .getLoaded();
                observableModelClasses.put(modelClass, observableModelClass);
            }
            T instance = observableModelClass.newInstance();
            for (Field field : modelClass
                    .getDeclaredFields()) {
                if (field.getAnnotation(NotObservable.class) == null) {
                    if (!field.getType()
                            .getName()
                            .contains("Logger") && !Modifier.isStatic(field.getModifiers())) {
                        instanciateObservableProperty(field, model, instance);
                    }
                } else {
                    field.setAccessible(true);
                    field.set(instance, field.get(model));
                }
            }
            return instance;
        } catch (Exception e) {
            throw new ModelHelperException(e);
        }
    }

    private static boolean hasDefaultConstructor(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameters().length == 0)
                .findFirst()
                .isPresent();
    }

    /**
     * For a given field set its JavaFX property instance
     */
    private static <T> void instanciateObservableProperty(Field field, T model, T observableModel) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException {
        String name = field.getName();
        field.setAccessible(true);
        Object value = field.get(model);
        Field property = getObservableProperty(observableModel, name);
        property.set(observableModel, property.getType()
                .getConstructor()
                .newInstance());
        getObservableSetter(observableModel, name, field.getType()).invoke(observableModel, value);
    }

    /**
     * Create a JavFX property for a given field
     *
     * @param field   The field to make observable
     * @param builder The ByteBuddy builder to forge the property
     * @return builder chain
     */
    private static DynamicType.Builder forgeObservableProperty(Field field, DynamicType.Builder builder) {
        String name = field.getName();
        Class<?> propertyType = getPropertyType(field);
        String camelCaseFieldName = name.substring(0, 1)
                .toUpperCase() + name.substring(1);
        String setterName = "set" + camelCaseFieldName;
        String getterName = "get" + camelCaseFieldName;
        builder = builder.defineField(name + "Property", propertyType, Modifier.PUBLIC)
                .defineMethod(name + "Property", propertyType, Modifier.PUBLIC)
                .intercept(MethodDelegation.to(getterPropertyInterceptor))
                .method(named(getterName))
                .intercept(MethodDelegation.to(getterInterceptor))
                .method(named(setterName))
                .intercept(MethodDelegation.to(setterInterceptor));
        return builder;
    }

    /**
     * Select the right JavaFX property type for a given field type
     *
     * @param field Field for wich we want to create an observable property
     * @return The property class suitable for this field
     */
    private static Class<?> getPropertyType(Field field) {
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            return SimpleStringProperty.class;
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return SimpleBooleanProperty.class;
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return SimpleIntegerProperty.class;
        } else if (fieldType == long.class || fieldType == Long.class) {
            return SimpleLongProperty.class;
        } else if (fieldType == float.class || fieldType == Float.class) {
            return SimpleFloatProperty.class;
        } else if (fieldType == double.class || fieldType == Double.class) {
            return SimpleDoubleProperty.class;
        } else if (fieldType == List.class || fieldType == ObservableList.class) {
            return SimpleListProperty.class;
        } else if (fieldType == Map.class) {
            return SimpleMapProperty.class;
        } else {
            return SimpleObjectProperty.class;
        }
    }

    /**
     * Given a getter (for a JavaFX property or a field) return the field name
     */
    private static String getFieldNameFromAccessor(Method method) {
        String methodName = method.getName();
        if (methodName
                .endsWith("Property")) {
            return methodName
                    .substring(0, methodName
                            .length() - 8);
        }
        String camelCaseField = methodName
                .substring(3);
        return camelCaseField.substring(0, 1)
                .toLowerCase() + camelCaseField.substring(1);
    }

    /**
     * Field lookup in a class given its name
     * <p>Note: this is cached for performance</p>
     *
     * @return the Field
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        String key = getModelClassName(clazz) + "::" + fieldName;
        Field field = assignedFields.get(key);
        if (field == null) {
            while (field == null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    if (clazz.getSuperclass() == clazz) {
                        throw new RuntimeException(e);
                    } else {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            field.setAccessible(true);
            assignedFields.put(key, field);
        }
        return field;
    }

    // https://stackoverflow.com/questions/47104098/using-bytebuddy-to-intercept-setter
    // If the caller try to set a list, we need to encapsulate it in a JavaFX ObservableList
    public static class SetterInterceptor {

        @RuntimeType
        public void invoke(@This Object observableModel, @Origin Method method, @AllArguments Object[] args) throws Throwable {
            String fieldName = getFieldNameFromAccessor(method);
            Object value = args[0];
            var property = getObservablePropertyInstance(observableModel, fieldName);
            Method propertySetter = getObservablePropertySetter(property, fieldName);
            if (value instanceof List && !(value instanceof ObservableList<?>)) {
                List<?> newList = ((List<?>) value).stream()
                        .map(ModelHelper::forgeMMVM)
                        .toList();
                var observableValue = FXCollections.observableList(newList);
                getField(observableModel.getClass(), fieldName).set(observableModel, observableValue);
                // at this point the JavaFX SimplePropertyList will call the getter for some reason...
                propertySetter.invoke(property, observableValue);
            } else if (property instanceof SimpleObjectProperty<?>) {
                var observableObject = forgeMMVM(value);
                getField(observableModel.getClass(), fieldName).set(observableModel, observableObject);
                propertySetter.invoke(property, observableObject);
            } else {
                getField(observableModel.getClass(), fieldName).set(observableModel, value);
                propertySetter.invoke(property, value);
            }

        }
    }

    private static Method getObservablePropertySetter(Object property, String fieldName) {
        Method propertySetter = propertySetters.get(fieldName);
        if (propertySetter == null) {
            propertySetter = Arrays.stream(property.getClass()
                            .getMethods())
                    .filter(m -> m.getName()
                            .equals("set"))
                    .findFirst()
                    .orElseThrow();
            propertySetters.put(fieldName, propertySetter);
        }
        return propertySetter;
    }

    private static Object getObservablePropertyInstance(Object observableModel, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        return getObservableProperty(observableModel, fieldName)
                .get(observableModel);
    }

    private static Field getObservableProperty(Object observableModel, String fieldName) throws NoSuchFieldException {
        Class<?> observableModelClass = observableModel.getClass();
        String key = getModelClassName(observableModelClass) + "::" + fieldName;
        Field field = observableProperties.get(key);
        if (field == null) {
            field = observableModelClass
                    .getDeclaredField(fieldName + "Property");
            observableProperties.put(key, field);
        }
        return field;
    }

    private static Method getObservableSetter(Object observableModel, String name, Class<?> clazz) throws NoSuchMethodException {
        Class<?> observableModelClass = observableModel.getClass();
        String key = getModelClassName(observableModelClass) + "::" + name;
        Method setter = observableSetters.get(key);
        if (setter == null) {
            String regularSetter = "set" + name.substring(0, 1)
                    .toUpperCase() + name.substring(1);
            String fluentSetter = name;
            setter = Arrays.stream(observableModelClass
                            .getMethods())
                    .filter(m -> (m.getName()
                            .equals(regularSetter) && m.getReturnType() == Void.TYPE) || (m.getName()
                            .equals(fluentSetter) && m.getReturnType() == m.getDeclaringClass()) && m.getParameterCount() == 1)
                    .findFirst()
                    .orElseThrow(() -> new ModelHelperException("There is no observable setter for field '%s' of type '%s' in class %s".formatted(name, clazz.getName(), observableModelClass.getName())));
            observableSetters.put(key, setter);
        }
        return setter;
    }

    private static String getModelClassName(Class<?> observableModelClass) {
        return observableModelClass.getName();
    }


    // https://stackoverflow.com/questions/47104098/using-bytebuddy-to-intercept-setter
    // JavaFX will first ask for the JavaFX Property, a method nameProperty()
    // then ask for the getter of the field: getName()
    public static class GetterInterceptor {
        @RuntimeType
        public Object invoke(@This Object proxy, @Origin Method method, @AllArguments Object[] args) throws Throwable {
            String fieldName = getFieldNameFromAccessor(method);
            Class<?> clazz = proxy.getClass();
            // retreive the field value
            Field field = getField(clazz, fieldName);
            var value = field
                    .get(proxy);
            // wrap it if necessary
            if (value instanceof List && !(value instanceof ObservableList<?>)) {
                var observableValue = FXCollections.observableList((List<?>) value);
                field.set(proxy, observableValue);
                return observableValue;
            } else {
                return value;
            }
        }
    }

    public static class GetterPropertyInterceptor {
        @RuntimeType
        public Object invoke(@This Object proxy, @Origin Method method, @AllArguments Object[] args) throws Throwable {
            String fieldName = getFieldNameFromAccessor(method) + "Property";
            Class<?> clazz = proxy.getClass();
            // retreive the field value
            Field field = getField(clazz, fieldName);
            var value = field
                    .get(proxy);
            // wrap it if necessary
            if (value instanceof List && !(value instanceof ObservableList<?>)) {
                var observableValue = FXCollections.observableList((List<?>) value);
                field.set(proxy, observableValue);
                return observableValue;
            } else {
                return value;
            }
        }
    }
}
