package com.hypercube.util.javafx.yaml;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.List;
import java.util.stream.Collectors;

public class ObservableSerializer extends BeanSerializerModifier {
    private final String suffixToIgnore = "Property";

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        return beanProperties.stream()
                .filter(property -> !property.getName()
                        .endsWith(this.suffixToIgnore))
                .collect(Collectors.toList());
    }
}
