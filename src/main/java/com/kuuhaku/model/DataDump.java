/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.model;

import com.kuuhaku.controller.mysql.MemberDAO;

import java.util.ArrayList;
import java.util.List;

public class DataDump {
    private final List<CustomAnswers> caDump;
    private final List<Member> mDump;
    private final List<GuildConfig> gcDump;

    public DataDump(List<CustomAnswers> caDump, List<Member> mDump, List<GuildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = mDump;
    }

    public DataDump(List<CustomAnswers> caDump, List<GuildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = new ArrayList<>();
    }

    public DataDump(List<Member> mDump) {
        this.caDump = new ArrayList<>();
        this.gcDump = new ArrayList<>();

        List<Member> oldMembers = MemberDAO.getMembers();
        mDump.removeAll(oldMembers);

        this.mDump = mDump;
    }

    public List<CustomAnswers> getCaDump() {
        return caDump;
    }

    public List<Member> getmDump() {
        return mDump;
    }

    public List<GuildConfig> getGcDump() {
        return gcDump;
    }
}
