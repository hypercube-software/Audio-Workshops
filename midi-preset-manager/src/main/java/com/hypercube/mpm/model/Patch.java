package com.hypercube.mpm.model;

import com.hypercube.mpm.config.SelectedPatch;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Patch {
    private String device;
    private String mode;
    private String bank;
    private String name;
    private String category;
    private String command;
    @EqualsAndHashCode.Exclude
    private int score;

    /**
     * Compare all fields except score
     */
    public boolean sameAs(SelectedPatch selectedPatch) {
        if (selectedPatch == null) {
            return false;
        }
        return name.equals(selectedPatch.getName()) &&
                command.equals(selectedPatch.getCommand()) &&
                category.equals(selectedPatch.getCategory()) &&
                bank.equals(selectedPatch.getBank()) &&
                mode.equals(selectedPatch.getMode()) &&
                device.equals(selectedPatch.getDevice());
    }
}
