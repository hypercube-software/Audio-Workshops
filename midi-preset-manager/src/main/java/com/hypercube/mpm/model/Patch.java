package com.hypercube.mpm.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Patch {
    private String device;
    private String mode;
    private String bank;
    private String name;
    @EqualsAndHashCode.Exclude
    private int score;
}
