package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

@Slf4j
public class TreeMerger {

    /**
     * Public entry point to merge two object trees of the same type.
     * The source object will overwrite matching properties in the target object.
     */
    public void merge(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        if (!source.getClass()
                .equals(target.getClass())) {
            throw new IllegalArgumentException("Objects must be of the exact same type.");
        }
        mergeInto(source, target, 0);
    }

    /**
     * Recursive method to merge fields, maintaining depth for indented debugging logs.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mergeInto(Object source, Object target, int depth) {
        String indent = "\t".repeat(depth);

        if (source == null || target == null) {
            return;
        }

        Class<?> clazz = source.getClass();
        log.info("{}-> Merging class: {}", indent, clazz.getSimpleName());

        // Iterate through all fields, including private ones
        for (Field field : clazz.getDeclaredFields()) {
            // Ignore static and final fields
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object sourceValue = field.get(source);
                Object targetValue = field.get(target);

                // If source has no value, we do not overwrite the target
                if (sourceValue == null) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                log.info("{}  * Field: {} ({})", indent, field.getName(), fieldType.getSimpleName());

                // --- Case 1: Primitives, Wrappers, String, and byte[] ---
                if (isPrimitiveOrBasicType(fieldType)) {
                    field.set(target, sourceValue);
                    log.info("{}    [Overwritten primitive/basic value]", indent);
                }

                // --- Case 2: Lists ---
                else if (List.class.isAssignableFrom(fieldType)) {
                    List sourceList = (List) sourceValue;
                    List targetList = (List) targetValue;

                    if (targetList == null) {
                        // If target has no list, assign the source list directly
                        field.set(target, sourceList);
                    } else {
                        log.info("{}    [Merging Lists] Source size: {}, Target size: {}",
                                indent, sourceList.size(), targetList.size());

                        for (int i = 0; i < sourceList.size(); i++) {
                            Object sourceItem = sourceList.get(i);

                            if (i < targetList.size()) {
                                // Target has an item at this index -> merge recursively
                                Object targetItem = targetList.get(i);
                                if (sourceItem == null) {
                                    continue;
                                }
                                if (isPrimitiveOrBasicType(sourceItem.getClass())) {
                                    targetList.set(i, sourceItem);
                                } else {
                                    mergeInto(sourceItem, targetItem, depth + 1);
                                }
                            } else {
                                // Source has more items -> append to target list
                                targetList.add(sourceItem);
                            }
                        }
                    }
                }

                // --- Case 3: Complex Objects (Recursive Merge) ---
                else {
                    if (targetValue == null) {
                        // If target nested object is null, assign the source object directly
                        field.set(target, sourceValue);
                    } else {
                        // Otherwise, dive deeper into the existing target object
                        mergeInto(sourceValue, targetValue, depth + 1);
                    }
                }

            } catch (IllegalAccessException e) {
                log.error("{}  [ERROR] Cannot access field: {}", indent, field.getName(), e);
                throw new RuntimeException("Error occurred during reflection-based merge", e);
            }
        }
    }

    /**
     * Determines if the class is a basic type that should be directly overwritten.
     */
    private boolean isPrimitiveOrBasicType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Byte.class
                || clazz == Boolean.class
                || clazz == Double.class
                || clazz == Float.class
                || clazz == Character.class
                || clazz == Short.class
                || clazz == byte[].class
                || clazz == int[].class
                || clazz == int[][].class;
    }
}
