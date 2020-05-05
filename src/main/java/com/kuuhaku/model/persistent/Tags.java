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
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.TagIcons;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "tags")
public class Tags {
    @Id
    @Column(columnDefinition = "VARCHAR(191)")
    private String id;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Partner = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Toxic = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Verified = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Reader = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Sponsor = false;

    public static List<String> getUserBadges(String id) {
        MessageFormat mf = new MessageFormat("https://cdn.discordapp.com/emojis/{1}.png?v=1");
        String exceed = ExceedDAO.getExceed(id);
        Member mb = MemberDAO.getMemberByMid(id).stream().sorted(Comparator.comparingLong(Member::getLevel).reversed()).collect(Collectors.toList()).get(0);

        List<String> badges = new ArrayList<>();

        if (!exceed.isEmpty()) {
            badges.add(TagIcons.getExceedId(ExceedEnums.getByName(exceed)));
        }

        if (id.equals(Main.getInfo().getNiiChan())) {
            badges.add(mf.format("697879726018003115"));
        } else {
            if (id.equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(id))
                badges.add(mf.format(TagIcons.getId(TagIcons.DEV)));

            if (Main.getInfo().getSupports().contains(id)) {
                badges.add(mf.format(TagIcons.getId(TagIcons.SUPPORT)));
            }

            if (Main.getInfo().getEditors().contains(id))
                badges.add(mf.format(TagIcons.getId(TagIcons.EDITOR)));

            try {
                if (TagDAO.getTagById(id).isReader())
                    badges.add(mf.format(TagIcons.getId(TagIcons.READER)));
            } catch (Exception ignore) {
            }

            try {
                if (mb.getLevel() >= 70)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL70)));
                else if (mb.getLevel() >= 60)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL60)));
                else if (mb.getLevel() >= 50)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL50)));
                else if (mb.getLevel() >= 40)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL40)));
                else if (mb.getLevel() >= 30)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL30)));
                else if (mb.getLevel() >= 20)
                    badges.add(mf.format(TagIcons.getId(TagIcons.LVL20)));
            } catch (Exception ignore) {
            }

            try {
                if (TagDAO.getTagById(id).isVerified())
                    badges.add(mf.format(TagIcons.getId(TagIcons.VERIFIED)));
            } catch (Exception ignore) {
            }

            try {
                if (TagDAO.getTagById(id).isToxic())
                    badges.add(mf.format(TagIcons.getId(TagIcons.TOXIC)));
            } catch (Exception ignore) {
            }

            try {
                if (!com.kuuhaku.model.persistent.Member.getWaifu(Main.getInfo().getUserByID(id)).isEmpty())
                    badges.add(mf.format(TagIcons.getId(TagIcons.MARRIED)));
            } catch (Exception ignore) {
            }
        }

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
