package com.hypercube.mpm.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ProjectConfiguration {
    private List<ProjectDevice> devices;
}
