package com.hypercube.mpm.javafx.widgets.browser;

import com.hypercube.mpm.javafx.event.EditButtonClickedEvent;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PatchBrowserController extends Controller<PatchBrowser, Void> {
    @FXML
    public void onEditButtonClick(EditButtonClickedEvent event) {
        log.info("Click on {} {}", event.getAttributeSelectorId(), event.getButtonId());
    }
}
