package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.NotObservable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MainModel {
    private List<String> devices = new ArrayList<>();
    private List<String> deviceModes = new ArrayList<>();
    private List<String> modeCategories = new ArrayList<>();
    private List<String> modeBanks = new ArrayList<>();
    private List<Patch> patches = new ArrayList<>();
    private String currentDeviceName;
    private String currentModeName;
    private String currentModeBankName;
    private String currentPatchNameFilter;
    private List<Integer> currentSelectedCategories = new ArrayList<>();
    @NotObservable
    private Map<String, DeviceState> deviceStates = new HashMap<>();
}
