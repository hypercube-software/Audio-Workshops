package com.hypercube.mpm.config;

import com.hypercube.mpm.model.Patch;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Favorites {
    private List<Patch> favorites = new ArrayList<>();

    public void updateFavorites(Patch patch) {
        int idx = favorites.indexOf(patch);
        if (idx == -1) {
            if (patch.getScore() > 0) {
                favorites.add(patch);
            }
        } else {
            if (patch.getScore() > 0) {
                favorites.set(idx, patch);
            } else {
                favorites.remove(idx);
            }
        }
    }
}
