package com.hypercube.workshop.audioworkshop.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CachedRegExp {
    private static final Map<String, Pattern> patterns = new HashMap<>();

    public static Matcher get(String exp, String target) {
        return get(exp, target, 0);
    }

    public static Matcher get(String exp, String target, int flags) {
        patterns.computeIfAbsent(exp, key -> Pattern.compile(key, flags));
        return patterns.get(exp)
                .matcher(target);
    }

    private CachedRegExp() {
    }
}
