package com.hypercube.mpm.javafx.widgets.dialog.project;

import com.hypercube.mpm.model.DeviceState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjectDialogViewModel {
    private List<DeviceState> deviceStates;
}
