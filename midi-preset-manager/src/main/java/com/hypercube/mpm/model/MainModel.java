package com.hypercube.mpm.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MainModel {
    private List<Patch> patches = new ArrayList<>();
}
