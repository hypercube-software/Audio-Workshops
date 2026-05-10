package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.header.KFHeader;

import java.io.File;
import java.util.List;

public record KurzweilFile(File file, KFHeader header, List<KFObject> objects) {
}
