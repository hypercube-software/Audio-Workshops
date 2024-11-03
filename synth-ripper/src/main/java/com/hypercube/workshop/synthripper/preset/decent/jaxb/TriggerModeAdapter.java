package com.hypercube.workshop.synthripper.preset.decent.jaxb;

import com.hypercube.workshop.synthripper.preset.decent.model.TriggerMode;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class TriggerModeAdapter extends XmlAdapter<String, TriggerMode> {
    @Override
    public TriggerMode unmarshal(String s) throws Exception {
        return TriggerMode.valueOf(s.toUpperCase());
    }

    @Override
    public String marshal(TriggerMode triggerMode) throws Exception {
        return triggerMode != null ? triggerMode.name()
                .toLowerCase() : null;
    }
}
