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

package com.kuuhaku.command.commands.partner;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

public class MyTagsCommand extends Command {

	public MyTagsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MyTagsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MyTagsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MyTagsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();
		String exceed = MemberDAO.getMemberByMid(author.getId()).get(0).getExceed();

		eb.setTitle(":label: Emblemas de " + author.getName());
		try {
			eb.setColor(Helper.colorThief(author.getAvatarUrl()));
        } catch (IOException e) {
            eb.setColor(Helper.getRandomColor());
        }

        StringBuilder badges = new StringBuilder();

        if (!exceed.isEmpty()) {
            badges.append(TagIcons.getExceed(ExceedEnums.getByName(exceed)));
        }

        if (author.getId().equals(Main.getInfo().getNiiChan())) {
            badges.append("<:niichan:623841337610862602>");
        } else {
            if (author.getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(author.getId()))
                badges.append(TagIcons.getTag(TagIcons.DEV));

            if (Main.getInfo().getSupports().contains(author.getId())) {
				badges.append(TagIcons.getTag(TagIcons.SHERIFF));
			}

            if (Main.getInfo().getEditors().contains(author.getId()))
                badges.append(TagIcons.getTag(TagIcons.EDITOR));

            try {
                if (TagDAO.getTagById(author.getId()).isReader())
                    badges.append(TagIcons.getTag(TagIcons.READER));
            } catch (Exception ignore) {
            }

            if (member.hasPermission(Permission.MANAGE_CHANNEL))
                badges.append(TagIcons.getTag(TagIcons.MODERATOR));

            try {
                if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 70)
                    badges.append(TagIcons.getTag(TagIcons.LVL70));
                else if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 60)
                    badges.append(TagIcons.getTag(TagIcons.LVL60));
                else if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 50)
                    badges.append(TagIcons.getTag(TagIcons.LVL50));
                else if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 40)
                    badges.append(TagIcons.getTag(TagIcons.LVL40));
                else if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 30)
                    badges.append(TagIcons.getTag(TagIcons.LVL30));
                else if (MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= 20)
                    badges.append(TagIcons.getTag(TagIcons.LVL20));
            } catch (Exception ignore) {
            }

            try {
                if (TagDAO.getTagById(author.getId()).isVerified())
                    badges.append(TagIcons.getTag(TagIcons.VERIFIED));
            } catch (Exception ignore) {
            }

            try {
                if (TagDAO.getTagById(author.getId()).isToxic())
                    badges.append(TagIcons.getTag(TagIcons.TOXIC));
            } catch (Exception ignore) {
            }

            try {
                if (!MemberDAO.getMemberById(author.getId() + guild.getId()).getWaifu().isEmpty())
                    badges.append(TagIcons.getTag(TagIcons.MARRIED));
            } catch (Exception ignore) {
            }
        }

        eb.addField("Emblemas:", badges.toString(), false);

        channel.sendMessage(eb.build()).queue();
    }
}
