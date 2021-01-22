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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.persistence.NoResultException;

public class BugHuntCommand extends Command {

    public BugHuntCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public BugHuntCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public BugHuntCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public BugHuntCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (message.getMentionedUsers().size() > 0) {
            if (message.getMentionedUsers().size() == 1) {
                resolveBugHuntByMention(message, channel);
            } else {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_too-many-users-nv")).queue();
            }
        } else {
            try {
                if (Main.getInfo().getUserByID(args[0]) != null) {
                    try {
                        resolveBugHuntById(args, channel);
                    } catch (NoResultException e) {
                        TagDAO.addUserTagsToDB(args[0]);
                        resolveBugHuntById(args, channel);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user-nv")).queue();
            }
        }
    }

    private void resolveBugHuntById(String[] args, MessageChannel channel) {
        Account acc = AccountDAO.getAccount(args[0]);

        acc.addBug();
        acc.addCredit(1000, this.getClass());
        AccountDAO.saveAccount(acc);
		channel.sendMessage("<@" + args[0] + "> ajudou a matar um bug! (Ganhou 1.000 créditos)").queue();
    }

    private void resolveBugHuntByMention(Message message, MessageChannel channel) {
        Account acc = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

        acc.addBug();
        acc.addCredit(1000, this.getClass());
		AccountDAO.saveAccount(acc);
		channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " ajudou a matar um bug! (Ganhou 1.000 créditos)").queue();
    }
}
