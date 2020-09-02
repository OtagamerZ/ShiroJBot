/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.utils.ExceedEnum;
import com.kuuhaku.utils.Tag;
import com.kuuhaku.utils.TagIcons;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tags")
public class Tags {
    @Id
    @Column(columnDefinition = "VARCHAR(191)")
    private String id;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean Partner = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean Toxic = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean Verified = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean Reader = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean Sponsor = false;

    public static List<String> getUserBadges(String id) {
        String pattern = "https://cdn.discordapp.com/emojis/{id}.png?v=1";
        String exceed = ExceedDAO.getExceed(id);
        Member mb = MemberDAO.getMemberByMid(id).stream().sorted(Comparator.comparingLong(Member::getLevel).reversed()).collect(Collectors.toList()).get(0);

        List<String> badges = new ArrayList<>();

        if (!exceed.isEmpty()) {
            badges.add(pattern.replace("{id}", TagIcons.getExceedId(ExceedEnum.getByName(exceed))));
        }

        Set<Tag> tags = Tag.getTags(Main.getInfo().getUserByID(mb.getMid()), Main.getInfo().getGuildByID(mb.getSid()).getMemberById(mb.getMid()));
        tags.forEach(t -> badges.add(t.getEmote(mb) == null ? "" : pattern.replace("{id}", t.getEmote(mb).getId(mb.getLevel()))));

        return badges;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPartner() {
        return Partner;
    }

    public void setPartner(boolean isPartner) {
        Partner = isPartner;
    }

    public boolean isToxic() {
        return Toxic;
    }

    public void setToxic(boolean isToxic) {
        Toxic = isToxic;
    }

    public boolean isVerified() {
        return Verified;
    }

    public void setVerified(boolean verified) {
        Verified = verified;
    }

    public boolean isReader() {
        return Reader;
    }

    public void setReader(boolean reader) {
        Reader = reader;
    }

    public boolean isSponsor() {
        return Sponsor;
    }

    public void setSponsor(boolean sponsor) {
        Sponsor = sponsor;
    }
}
