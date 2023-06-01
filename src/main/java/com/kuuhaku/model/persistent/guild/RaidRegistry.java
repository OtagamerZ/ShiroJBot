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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.records.RSEntry;
import com.kuuhaku.util.Certifier;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name = "raid_registry")
public class RaidRegistry extends DAO<RaidRegistry> {
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "guild", nullable = false)
    private String guild;

    @Column(name = "targeted_invite")
    private String targetedInvite;

    @ElementCollection
    @Column(name = "users")
    @CollectionTable(name = "raid_users")
    private Set<String> users = new HashSet<>();

    @Column(name = "start_timestamp", nullable = false)
    private ZonedDateTime startTimestamp = ZonedDateTime.now(ZoneId.of("GMT-3"));

    @Column(name = "end_timestamp", nullable = false)
    private ZonedDateTime endTimestamp;

    @Transient
    private transient final Map<String, Invite> initInvites = new HashMap<>();

    @Transient
    private transient final List<RSEntry> initUsers = new ArrayList<>();

    public RaidRegistry() {
    }

    public RaidRegistry(String guild) {
        this.guild = guild;
    }

    public int getId() {
        return id;
    }

    public String getGuild() {
        return guild;
    }

    public String getTargetedInvite() {
        return targetedInvite;
    }

    public void setTargetedInvite(String targetedInvite) {
        this.targetedInvite = targetedInvite;
    }

    public Set<String> getUsers() {
        return users;
    }

    public ZonedDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public ZonedDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(ZonedDateTime endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public long getDuration() {
        if (endTimestamp == null) return System.currentTimeMillis() / 1000 - startTimestamp.getLong(ChronoField.INSTANT_SECONDS);

        return startTimestamp.until(endTimestamp, ChronoUnit.SECONDS);
    }

    public Map<String, Invite> getInitInvites() {
        return initInvites;
    }

    public List<RSEntry> getInitUsers() {
        return initUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaidRegistry that = (RaidRegistry) o;
        return id == that.id && Objects.equals(guild, that.guild) && Objects.equals(startTimestamp, that.startTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guild, startTimestamp);
    }

    @Override
    public String toString() {
        XStringBuilder sb = new XStringBuilder();

        Guild g = Main.getApp().getShiro().getGuildById(guild);
        if (g == null) return "UNAVAILABLE";

        sb.appendNewLine("R.A.ID REPORT Nº " + guild + "-" + Utils.separate(id));
        sb.appendNewLine("GUILD: " + g.getName());
        sb.appendNewLine("DURATION: " + format.format(startTimestamp) + " - " + format.format(endTimestamp));
        if (targetedInvite != null) {
            sb.appendNewLine("INVITE USED: " + targetedInvite);
        }
        sb.appendNewLine("BAN COUNT: " + Utils.separate(users.size()));
        sb.separator('-');
        for (String id : users.stream().sorted().toList()) {
            User user = Main.getApp().getUserById(id);

            sb.appendNewLine(user.getId());
            sb.appendIndent(user.getName(), 3);
        }

        String print = Certifier.sign(sb.toString());

        sb.separator('-', "RSA FINGERPRINT");
        sb.appendNewLine(print);
        sb.nextLine();
        sb.appendNewLine(Constants.BOT_NAME + " " + Constants.BOT_VERSION.call());

        return sb.toString();
    }
}
