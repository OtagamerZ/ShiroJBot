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

package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.persistent.Tags;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.persistence.NoResultException;

public class ToxicTagCommand extends Command {

    public ToxicTagCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public ToxicTagCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public ToxicTagCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public ToxicTagCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (message.getMentionedUsers().size() > 0) {
            if (message.getMentionedUsers().size() == 1) {
                try {
                    resolverToxicByMention(message, channel);
                } catch (NoResultException e) {
                    TagDAO.addUserTagsToDB(message.getMentionedUsers().get(0).getId());
                    resolverToxicByMention(message, channel);
                }
            } else {
                channel.sendMessage(":x: | Nii-chan, você mencionou usuários demais!").queue();
            }
        } else {
            try {
                if (Main.getInfo().getUserByID(args[0]) != null) {
                    try {
                        resolveToxicById(args, channel);
                    } catch (NoResultException e) {
                        TagDAO.addUserTagsToDB(args[0]);
                        resolveToxicById(args, channel);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                channel.sendMessage(":x: | Nii-chan bobo, você precisa mencionar um usuário!").queue();
            }
        }
    }

    private void resolveToxicById(String[] args, MessageChannel channel) {
        Tags t = TagDAO.getTagById(args[0]);
        if (t.isToxic()) {
TagDAO.removeTagToxic(args[0]);
channel.sendMessage("<@" + args[0] + "> não é mais tóxico, que bom!").queue();
} else {
TagDAO.giveTagToxic(args[0]);
TagDAO.removeTagVerified(args[0]);
channel.sendMessage("<@" + args[0] + "> agora é tóxico, reporta ele!").queue();
}
    }

    private void resolverToxicByMention(Message message, MessageChannel channel) {
        Tags t = TagDAO.getTagById(message.getMentionedUsers().get(0).getId());
        if (t.isToxic()) {
            TagDAO.removeTagToxic(message.getMentionedUsers().get(0).getId());
            channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " não é mais tóxico, que bom!").queue();
        } else {
            TagDAO.giveTagToxic(message.getMentionedUsers().get(0).getId());
            TagDAO.removeTagVerified(message.getMentionedUsers().get(0).getId());
            channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " agora é tóxico, reporta ele!").queue();
        }
    }
}
