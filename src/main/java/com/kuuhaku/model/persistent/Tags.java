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
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tags")
public class Tags {
    @Id
    @Column(columnDefinition = "VARCHAR(191)")
    private String uid;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean beta = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean toxic = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean verified = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean reader = false;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean sponsor = false;

    public Tags(String uid) {
        this.uid = uid;
    }

    public Tags() {
    }

    public static List<String> getUserBadges(String id) {
        String pattern = "https://cdn.discordapp.com/emojis/%s.png?v=1";
        String exceed = ExceedDAO.getExceed(id);
        Member mb = MemberDAO.getMemberByMid(id).stream().sorted(Comparator.comparingLong(Member::getLevel).reversed()).collect(Collectors.toList()).stream().findFirst().orElse(null);

        if (mb == null) return new ArrayList<>();

        List<String> badges = new ArrayList<>();

        if (!exceed.isEmpty()) {
            badges.add(pattern.formatted(TagIcons.getExceedId(ExceedEnum.getByName(exceed))));
        }

        Set<Tag> tags = Tag.getTags(Main.getInfo().getUserByID(mb.getUid()), Main.getInfo().getGuildByID(mb.getSid()).getMemberById(mb.getUid()));
        for (Tag t : tags) {
            badges.add(t.getEmote(mb) == null ? "" : pattern.replace("{id}", Objects.requireNonNull(t.getEmote(mb)).getId(mb.getLevel())));
        }

        return badges;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String id) {
        this.uid = id;
    }

    public boolean isBeta() {
        return beta;
    }

    public void setBeta(boolean isBeta) {
        beta = isBeta;
    }

    public boolean isToxic() {
        return toxic;
    }

    public void setToxic(boolean isToxic) {
        toxic = isToxic;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isReader() {
        return reader;
    }

    public void setReader(boolean reader) {
        this.reader = reader;
    }

    public boolean isSponsor() {
        return sponsor;
    }

    public void setSponsor(boolean sponsor) {
        this.sponsor = sponsor;
    }
}
