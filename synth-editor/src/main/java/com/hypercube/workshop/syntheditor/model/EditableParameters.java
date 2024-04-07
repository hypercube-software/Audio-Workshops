package com.hypercube.workshop.syntheditor.model;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EditableParameters {
    private Map<Integer, EditableParameter> parameters = new HashMap<>();

    public void add(EditableParameter editableParameter) {
        parameters.put(editableParameter.getAddress(), editableParameter);
    }

    public void dump() {
        log.info("-------------------------");
        log.info("Editable Parameters");
        log.info("-------------------------");
        getAll().forEach(p -> log.info("0x%06X %s".formatted(p.getAddress(), p.getPath())));
    }

    public void clear() {
        parameters.clear();
    }

    public List<EditableParameter> getAll() {
        return parameters.values()
                .stream()
                .sorted((a, b) -> a.getPath()
                        .compareTo(b.getPath()))
                .toList();
    }
}
