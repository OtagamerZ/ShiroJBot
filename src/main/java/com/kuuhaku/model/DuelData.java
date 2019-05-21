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
    transient private boolean d1, d2;
    transient private boolean p1turn = true;

    public DuelData(User p1, User p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.b1 = Database.getBeyblade(p1.getId());
        this.b2 = Database.getBeyblade(p2.getId());
    }

    public User getP1() {
        return p1;
    }

    public User getP2() {
        return p2;
    }

    public Beyblade getB1() {
        return b1;
    }

    public Beyblade getB2() {
        return b2;
    }

    public boolean isD1() {
        return d1;
    }

    public void setD1(boolean d1) {
        this.d1 = d1;
    }

    public boolean isD2() {
        return d2;
    }

    public void setD2(boolean d2) {
        this.d2 = d2;
    }

    public boolean isP1turn() {
        return p1turn;
    }

    public void setP1turn(boolean p1turn) {
        this.p1turn = p1turn;
    }
}
