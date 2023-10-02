/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.misc;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.Uwuifier;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Command(
		name = "uwu",
		aliases = {"owo"},
		usage = "req_text",
		category = Category.MISC
)
public class UwuCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você pwecisa indicaw o texto que deseja townyaw uwu.").queue();
			return;
		} else if (args[0].length() > 1024) {
			channel.sendMessage("❌ | Essa mensagem é muito w-wonga.").queue();
			return;
		}

		com.kuuhaku.model.persistent.Member m = MemberDAO.getMember(author.getId(), guild.getId());
		try {
			Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro");
			String s = Helper.sendEmotifiedString(guild, Helper.replaceTags(argsAsText, author, guild, message));
			s = new Uwuifier().uwu(s);

			WebhookMessageBuilder wmb = new WebhookMessageBuilder()
					.setAllowedMentions(AllowedMentions.none())
					.setContent(s);

			String avatar = Helper.getOr(m.getPseudoAvatar(), author.getAvatarUrl());
			String name = Helper.getOr(m.getPseudoName(), author.getName());

			try {
				Member nii = guild.getMember(Main.getInfo().getUserByID(ShiroInfo.getNiiChan()));
				wmb.setUsername(nii != null && name.equals(nii.getUser().getName()) ? name + " (FAKE)" : name);
				wmb.setAvatarUrl(avatar);
			} catch (RuntimeException e) {
				m.setPseudoName("");
				m.setPseudoAvatar("");
				MemberDAO.saveMember(m);
			}

			assert wh != null;
			WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
			try {
				message.delete().queue(null, Helper::doNothing);
				wc.send(wmb.build()).get();
			} catch (InterruptedException | ExecutionException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		} catch (IndexOutOfBoundsException | InsufficientPermissionException | ErrorResponseException | NullPointerException | InterruptedException | ExecutionException e) {
			String s = new Uwuifier().uwu(Helper.replaceTags(argsAsText, author, guild, message));
			channel.sendMessage(s).allowedMentions(List.of()).queue();
			if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
				message.delete().queue(null, Helper::doNothing);
		}
	}
}
