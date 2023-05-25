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

import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StringTree {
    private record Node(String name, Map<String, Node> children) {
        public Node(String name) {
            this(name, new HashMap<>());
        }

        public String toString() {
            XStringBuilder buffer = new XStringBuilder();
            print(buffer, "", "");
            return buffer.toString();
        }

        private void print(XStringBuilder buffer, String prefix, String childrenPrefix) {
            buffer.appendNewLine(prefix + name);

            Iterator<Node> it = children.values().iterator();
            while (it.hasNext()) {
                Node next = it.next();
                if (next == null) break;

                if (it.hasNext()) {
                    next.print(buffer, childrenPrefix + "  ├ ", childrenPrefix + "  │ ");
                } else {
                    next.print(buffer, childrenPrefix + "  └ ", childrenPrefix + "    ");
                }
            }
        }
    }

    private final Map<String, Node> root = new HashMap<>();

    public void addElement(Object elem, String... path) {
        Map<String, Node> node = root;
        for (int i = 0; i < path.length - 1; i++) {
            String p = path[i];
            node = node.compute(p, (k, v) -> Utils.getOr(v, new Node(k))).children;
        }

        node.put(String.valueOf(elem), null);
    }

    @Override
    public String toString() {
        XStringBuilder buffer = new XStringBuilder();

        for (Node next : root.values()) {
            next.print(buffer, "", "");
        }

        return buffer.toString();
    }
}
