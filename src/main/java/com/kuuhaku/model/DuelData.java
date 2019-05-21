/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

import com.kuuhaku.controller.Database;
import net.dv8tion.jda.core.entities.User;

public class DuelData {
    private User p1, p2;
    private Beyblade b1, b2;

    public DuelData(User p1, User p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.b1 = Database.getBeyblade(p1.getId());
        this.b2 = Database.getBeyblade(p2.getId());
    }

    public User getP1() {
        return p1;
    }

    public void setP1(User p1) {
        this.p1 = p1;
    }

    public User getP2() {
        return p2;
    }

    public void setP2(User p2) {
        this.p2 = p2;
    }

    public Beyblade getB1() {
        return b1;
    }

    public void setB1(Beyblade b1) {
        this.b1 = b1;
    }

    public Beyblade getB2() {
        return b2;
    }

    public void setB2(Beyblade b2) {
        this.b2 = b2;
    }
}
