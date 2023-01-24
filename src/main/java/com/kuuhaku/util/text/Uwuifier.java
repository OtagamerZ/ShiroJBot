/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.util.text;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;

public class Uwuifier {
    public static final Uwuifier INSTANCE = new Uwuifier();

    private static final String[] punctuation = {"!?", "?!!", "?!?1", "!!11", "?!?!"};
    private static final String[] faces = {
            "(・`ω´・)", ";;w;;", "OwO",
            "UwU", ">w<", "^w^",
            "ÚwÚ", "^-^", ":3",
            "x3", "q(≧▽≦q)", "(✿◡‿◡)"
    };
    private static final String[] actions = {
            "str/uwu_blush", "str/uwu_whisper", "str/uwu_sob",
            "str/uwu_scream", "str/uwu_shy", "str/uwu_run",
            "str/uwu_screech", "str/uwu_stare", "str/uwu_hug",
            "str/uwu_smile"
    };
    private static final List<Pair<String, String>> exp = List.of(
            Pair.of("[rl]", "w"),
            Pair.of("[RL]", "W"),
            Pair.of("n([AEIOUaeiou])", "ny$1"),
            Pair.of("N([AEIOUaeiou])", "Ny$1"),
            Pair.of("ove", "uv")
    );

    private final double faceFac;
    private final double actionFac;
    private final double stutterFac;

    public Uwuifier() {
        this.faceFac = 0.05;
        this.actionFac = 0.075;
        this.stutterFac = 0.1;
    }

    public Uwuifier(double faceFac, double actionFac, double stutterFac, double none) {
        double[] norm = Calc.sumToOne(faceFac, actionFac, stutterFac, none);

        this.faceFac = norm[0];
        this.actionFac = norm[1];
        this.stutterFac = norm[2];
    }

    public String uwu(I18N locale, String text) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] words = line.split("(?<=\\S )");
            for (int j = 0; j < words.length; j++) {
                String word = words[j];
                if (UrlValidator.getInstance().isValid(word) || word.matches(":.+:|<.+>")) continue;

                for (Pair<String, String> p : exp) {
                    word = word.replaceAll(p.getLeft(), p.getRight());
                }

                words[j] = word.replace("!", Utils.getRandomEntry(punctuation));
            }

            String out = String.join("", words);
            while (Utils.regex(out, " [A-z]").find()) {
                out = out.replaceFirst(" ([A-z])", replaceSpace(locale));
            }

            lines[i] = out.replace("§", " ");
        }

        return String.join("\n", lines);
    }

    private String replaceSpace(I18N locale) {
        if (Calc.chance(faceFac * 100)) {
            return "§" + Utils.getRandomEntry(faces) + "§$1";
        } else if (Calc.chance(actionFac * 100)) {
            return "§" + locale.get(Utils.getRandomEntry(actions)) + "§$1";
        } else if (Calc.chance(stutterFac * 100)) {
            return "§" + ("$1" + "-").repeat(Calc.rng(2)) + "$1";
        }

        return "§$1";
    }
}