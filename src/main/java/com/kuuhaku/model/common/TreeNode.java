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

import com.kuuhaku.util.XStringBuilder;

import java.util.Map;
import java.util.TreeMap;

public abstract class TreeNode {
    protected final Map<String, TreeNode> children = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Map<String, TreeNode> getChildren() {
        return children;
    }

    public TreeNode addNode(StringTree.NamedNode node) {
        children.put(node.getName(), node);
        return this;
    }

    public abstract void print(XStringBuilder buffer, int level, boolean hasNext);
}