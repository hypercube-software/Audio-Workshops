package com.hypercube.mpm.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class MainModel {
    private List<String> devices = new ArrayList<>();
    private List<Patch> patches = new ArrayList<>();
}
