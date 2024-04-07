package com.hypercube.workshop.syntheditor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class EditableParameter {
    private String path;
    private int address;
    private int size;
    @Setter
    private int value;

}
