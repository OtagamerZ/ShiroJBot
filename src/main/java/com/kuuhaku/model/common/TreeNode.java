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

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TreeNode {
    protected final Map<String, TreeNode> children = new LinkedHashMap<>();

    public Map<String, TreeNode> getChildren() {
        return children;
    }

    public TreeNode addNode(StringTree.NamedNode node) {
        children.put(node.getName(), node);
        return this;
    }

    public abstract void print(XStringBuilder buffer, int level, boolean parentHasNext, boolean hasNext);
}
