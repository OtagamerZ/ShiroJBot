package com.kuuhaku.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Uwuifier {
    private final double faceFac;
    private final double actionFac;
    private final double stutterFac;

    private static final String[] punctuation = {"!?", "?!!", "?!?1", "!!11", "?!?!"};
    private static final String[] faces = {
            "(・`ω´・)",
            ";;w;;",
            "OwO",
            "UwU",
            ">w<",
            "^w^",
            "ÚwÚ",
            "^-^",
            ":3",
            "x3",
            "q(≧▽≦q)",
            "(✿◡‿◡)"
    };
    private static final String[] actions = {
            "\\\\*cora\\\\*",
            "\\\\*murmura\\\\*",
            "\\\\*chora\\\\*",
            "\\\\*grita\\\\*",
            "\\\\*vergonha\\\\*",
            "\\\\*foge\\\\*",
            "\\\\*gritinhos\\\\*",
            "\\\\*observa\\\\*",
            "\\\\*abraça\\\\*",
            "\\\\*ri\\\\*"
    };
    private static final List<Pair<String, String>> exp = List.of(
            Pair.of("[rl]", "w"),
            Pair.of("[RL]", "W"),
            Pair.of("n([AEIOUaeiou])", "ny$1"),
            Pair.of("N([AEIOUaeiou])", "Ny$1"),
            Pair.of("ove", "uv")
    );

    public Uwuifier() {
        this.faceFac = 0.05;
        this.actionFac = 0.075;
        this.stutterFac = 0.1;
    }

    public Uwuifier(double faceFac, double actionFac, double stutterFac, double none) {
        double[] norm = Helper.sumToOne(faceFac, actionFac, stutterFac, none);

        this.faceFac = norm[0];
        this.actionFac = norm[1];
        this.stutterFac = norm[2];
    }

    public String uwu(String text) {
        String[] split = StringUtils.normalizeSpace(text).split(" ");

        for (int i = 0; i < split.length; i++) {
            String word = split[i];
            if (Helper.isUrl(word) || word.matches(":.+:|<.+>")) continue;

            for (Pair<String, String> p : exp) {
                word = word.replaceAll(p.getLeft(), p.getRight());
            }

            split[i] = word.replace("!", Helper.getRandomEntry(punctuation));
        }

        String out = String.join(" ", split);
        while (Helper.regex(out, " [A-z]").find()) {
            out = out.replaceFirst(" ([A-z])", replaceSpace());
        }

        return out.replace("§", " ");
    }

    private String replaceSpace() {
        if (Helper.chance(faceFac * 100)) {
            return "§" + Helper.getRandomEntry(faces) + "§$1";
        } else if (Helper.chance(actionFac * 100)) {
            return "§" + Helper.getRandomEntry(actions) + "§$1";
        } else if (Helper.chance(stutterFac * 100)) {
            return "§" + ("$1" + "-").repeat(Helper.rng(2)) + "$1";
        }

        return "§$1";
    }
}
