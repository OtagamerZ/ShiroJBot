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

import java.util.*;

public class StringTree {
    /*
    root
      ├ level 1
      │  ├ level 2
      │  └ level 2
      ├ level 1
      └ level 1
     */

    private final Root root = new Root();

    public void addElement(Object elem, String... path) {
        TreeNode node = root;
        for (int i = 0; i < path.length - 1; i++) {
            String p = path[i];
            node = node.getChildren().compute(p, (k, v) -> Utils.getOr(v, new NamedNode(k)));
        }

        node.addNode(new NamedNode(String.valueOf(elem)));
    }

    @Override
    public String toString() {
        XStringBuilder buffer = new XStringBuilder();
        root.print(buffer, 0, true);

        return buffer.toString();
    }

    public static class Root extends TreeNode {
        @Override
        public void print(XStringBuilder buffer, int level, boolean hasNext) {
            Iterator<TreeNode> iterator = children.values().iterator();
            while (iterator.hasNext()) {
                TreeNode child = iterator.next();
                child.print(buffer, 0, iterator.hasNext());
            }
        }
    }

    public static class NamedNode extends TreeNode {
        private final String name;

        public NamedNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public void print(XStringBuilder buffer, int level, boolean hasNext) {
            buffer.nextLine();
            for (int i = 0; i < level; i++) {
                if (i < level - 1) {
                    buffer.append("  │ ");
                } else {
                    buffer.append(hasNext ? "  ├─" : "  └─");
                }
            }

            buffer.append(" " + name);
            Iterator<TreeNode> iterator = children.values().iterator();
            while (iterator.hasNext()) {
                TreeNode child = iterator.next();
                child.print(buffer, level + 1, iterator.hasNext());
            }
        }
    }
}
