package com.hypercube.mpm.javafx.widgets.browser;

import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.javafx.event.EditButtonClickedEvent;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
import com.hypercube.mpm.model.DeviceStateId;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class PatchBrowserController extends Controller<PatchBrowser, Void> {
    @Autowired
    DeviceToolBox deviceToolBox;

    @FXML
    public void onEditButtonClick(EditButtonClickedEvent event) {
        log.info("Click on {} {}", event.getAttributeSelectorId(), event.getButtonId());
        if ("bank".equals(event.getAttributeSelectorId())) {
            if ("addButton".equals(event.getButtonId())) {
                createBank();
            } else if ("removeButton".equals(event.getButtonId())) {
                deleteBank();
            }
        }
    }

    private void deleteBank() {
        DeviceStateId deviceStateId = MainModel.getObservableInstance()
                .getCurrentDeviceState()
                .getId();
        List<String> selectedBankNames = MainModel.getObservableInstance()
                .getCurrentDeviceState()
                .getSelectedBankNames();
        String bankName = selectedBankNames.size() == 1 ? selectedBankNames.getFirst() : null;
        if (bankName != null && GenericDialogController.ask("Delete bank", "Are you sure you want to delete bank '%s' for device'%s' ?".formatted(bankName, deviceStateId.getName()))) {
            deviceToolBox.deleteBank(deviceStateId.getName(), deviceStateId.getMode(), bankName);
        }
    }

    private void createBank() {
        GenericDialogController.input("Create new bank", "Bank name:")
                .ifPresent(bankName -> {
                    DeviceStateId deviceStateId = MainModel.getObservableInstance()
                            .getCurrentDeviceState()
                            .getId();
                    deviceToolBox.createBank(deviceStateId.getName(), deviceStateId.getMode(), bankName);
                });
    }
}
