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

import com.kuuhaku.Main;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.persistent.guild.RaidRegistry;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.records.RSEntry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RaidSentry {
    public enum State {
        STANDBY, ENGAGED, ACTIVE
    }

    private final Map<String, RaidRegistry> entries = new ConcurrentHashMap<>();
    private final Map<String, Consumer<RaidRegistry>> finishers = new HashMap<>();
    private final Map<String, RaidRegistry> active = ExpiringMap.builder()
            .expiration(10, TimeUnit.SECONDS)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .<String, RaidRegistry>expirationListener((k, v) -> {
                Guild g = Main.getApp().getShiro().getGuildById(k);
                if (g != null) {
                    g.retrieveInvites()
                            .mapToResult()
                            .queue(i -> {
                                if (i.isSuccess()) {
                                    Invite likely = null;
                                    int uses = 0;
                                    for (Invite invite : i.get()) {
                                        Invite prev = v.getInitInvites().get(invite.getCode());
                                        if (prev != null) {
                                            int diff = invite.getUses() - prev.getUses();
                                            if (diff > 0 && (likely == null || diff > uses)) {
                                                likely = invite;
                                                uses = diff;
                                            }
                                        }
                                    }

                                    if (likely != null) {
                                        v.setTargetedInvite(likely.getCode());
                                    }
                                }

                                v.setEndTimestamp(ZonedDateTime.now(ZoneId.of("GMT-3")));
                                v.save();
                            });

                    Consumer<RaidRegistry> act = finishers.get(k);
                    if (act != null) {
                        act.accept(v);
                    }
                }
            })
            .build();

    public State watch(GuildConfig config, Guild guild, User user) {
        if (!guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS, Permission.MANAGE_PERMISSIONS, Permission.MANAGE_CHANNEL)) {
            return State.STANDBY;
        } else if (!config.getSettings().isFeatureEnabled(GuildFeature.ANTI_RAID)) {
            return State.STANDBY;
        }

        {
            RaidRegistry reg = active.get(guild.getId());
            if (reg != null) {
                reg.getUsers().add(user.getId());
                return State.ACTIVE;
            }
        }

        RaidRegistry reg = entries.computeIfAbsent(guild.getId(), k -> new RaidRegistry(guild.getId()));
        List<RSEntry> entries = reg.getInitUsers();
        entries.add(new RSEntry(user.getId(), System.currentTimeMillis()));
        if (entries.size() > 10) {
            entries.remove(0);
        } else if (entries.size() < 5) {
            return State.STANDBY;
        }

        int avgInterval = 0;
        int avgDelta = 0;
        for (int i = 0; i < entries.size() - 1; i++) {
            int interval = (int) (entries.get(i + 1).timestamp() - entries.get(i).timestamp());
            if (i == 0) {
                avgInterval = interval;
            } else {
                int prev = avgInterval;
                avgInterval = (avgInterval + interval) / 2;
                avgDelta = (avgDelta + Math.abs(prev - avgInterval)) / 2;
            }
        }

        System.out.println(avgInterval);
        System.out.println(avgDelta);

        int threshold = config.getSettings().getAntiRaidThreshold();
        if (avgDelta <= threshold || avgInterval <= threshold) {
            try {
                return State.ENGAGED;
            } finally {
                guild.retrieveInvites().queue(i -> {
                    for (Invite invite : i) {
                        reg.getInitInvites().put(invite.getCode(), invite);
                    }
                });
                active.put(guild.getId(), reg);
                entries.remove(guild.getId());

                for (RSEntry entry : List.copyOf(entries)) {
                    guild.ban(Main.getApp().getUserById(entry.uid()), 7, TimeUnit.DAYS)
                            .reason(config.getLocale().get("str/raid_ban_reason"))
                            .queue();
                }
            }
        }

        return State.STANDBY;
    }

    public synchronized void setFinishTask(String guild, Consumer<RaidRegistry> action) {
        if (!active.containsKey(guild)) return;

        finishers.put(guild, action);
    }
}
