/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.util;

import org.apache.commons.lang3.StringUtils;

public class XStringBuilder {
    private final StringBuilder sb;

    public XStringBuilder() {
        sb = new StringBuilder();
    }

    public XStringBuilder(String value) {
        sb = new StringBuilder(value);
    }

    public XStringBuilder append(Object value) {
        sb.append(value);
        return this;
    }

    public XStringBuilder appendNewLine(Object value) {
        if (sb.length() > 0)
            sb.append("\n").append(value);
        else
            sb.append(value);
        return this;
    }

    public XStringBuilder appendIndent(Object value, int indent) {
        sb.append(StringUtils.repeat("\t", indent)).append(value);
        return this;
    }

    public XStringBuilder appendIndentNewLine(Object value, int indent) {
        sb.append("\n").append(StringUtils.repeat("\t", indent)).append(value);
        return this;
    }

    public boolean isEmpty() {
        return sb.toString().isEmpty();
    }

    public boolean isBlank() {
        return sb.toString().isBlank();
    }

    public void clear() {
        sb.setLength(0);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
