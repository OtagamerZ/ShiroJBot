/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import net.jodah.expiringmap.ExpiringMap;
import org.intellij.lang.annotations.Language;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PatternCache {
    private static final ExpiringMap<String, Pattern> cache = ExpiringMap.builder().expiration(1, TimeUnit.HOURS).build();

    public static Pattern compile(@Language("RegExp") String regex) {
        return cache.computeIfAbsent(regex, k -> Pattern.compile(regex));
    }
}
