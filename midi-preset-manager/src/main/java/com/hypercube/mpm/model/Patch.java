package com.hypercube.mpm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Patch {
    private String device;
    private String mode;
    private String bank;
    private String name;
    private int score;
}
