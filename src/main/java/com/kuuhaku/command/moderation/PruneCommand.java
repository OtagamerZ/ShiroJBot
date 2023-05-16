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

package com.kuuhaku.command.moderation;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Command(
        name = "prune",
        category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
        "<amount:number:r>",
        "<user:user:r> <amount:number>",
        "<action:word:r>[all]"
})
@Requires({Permission.MESSAGE_HISTORY, Permission.MANAGE_CHANNEL})
public class PruneCommand implements Executable {
    private static final Set<String> queue = new HashSet<>();

    @Override
    public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
        if (queue.contains(event.guild().getId())) {
            event.channel().sendMessage(locale.get("error/prune_in_progress")).queue();
            return;
        }

        if (args.has("action")) {
            try {
                CompletableFuture<Boolean> allow = Utils.confirm(locale.get("question/prune_all"), event.channel(), w -> true, event.user());
                if (!allow.get()) return;

                Utils.confirm(locale.get("question/prune_all_confirm"), event.channel(), w -> {
                    StandardGuildMessageChannel chn = (StandardGuildMessageChannel) event.message().getChannel();

                    try {
                        queue.add(event.guild().getId());
                        MessageChannel mc = Pages.subGet(chn.createCopy());
                        event.channel().delete()
                                .flatMap(c -> mc.sendMessage(locale.get("success/prune_all")))
                                .submit().get();
                    } catch (ExecutionException | InterruptedException ignore) {
                    } catch (InsufficientPermissionException e) {
                        event.channel().sendMessage(locale.get("error/missing_perms") + " " + locale.get("perm/" + e.getPermission().name())).queue();
                    } finally {
                        queue.remove(event.guild().getId());
                    }

                    return true;
                }, event.user());
            } catch (PendingConfirmationException e) {
                event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
            } catch (ExecutionException | InterruptedException ignore) {
            }

            return;
        }

        User target;
        if (args.has("user")) {
            target = event.message().getMentions().getUsers().get(0);
        } else {
            target = null;
        }

        int amount = args.getInt("amount");
        if (!Utils.between(amount, 1, 1000)) {
            event.channel().sendMessage(locale.get("error/invalid_value_range", 1, 1000)).queue();
            return;
        }

        List<String> ids = new ArrayList<>();

        queue.add(event.guild().getId());
        event.channel().getIterableHistory()
                .forEachAsync(m -> {
                    if (!m.equals(event.message()) && (target == null || m.getAuthor().equals(target))) {
                        ids.add(m.getId());
                    }

                    return ids.size() < amount;
                })
                .thenApply(v -> event.channel().purgeMessagesById(ids))
                .thenAccept(act -> CompletableFuture.allOf(act.toArray(CompletableFuture[]::new))
                        .thenRun(() -> {
                            queue.remove(event.guild().getId());
                            event.channel().sendMessage(locale.get("success/prune")).queue();
                        })
                );
    }
}
