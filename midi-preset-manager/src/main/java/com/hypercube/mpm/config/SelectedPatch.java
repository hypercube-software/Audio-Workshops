package com.hypercube.mpm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelectedPatch {
    private String mode;
    private String bank;
    private String name;
    private String category;
    private String command;
}
