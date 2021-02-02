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

package com.kuuhaku.command.commands.discord.misc;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Command(
		name = "diga",
		aliases = {"say", "repita", "repeat"},
		usage = "req_text",
		category = Category.MISC
)
public class SayCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa definir uma mensagem.").queue();
			return;
		} else if (args[0].length() > 1024) {
			channel.sendMessage("❌ | Essa mensagem é muito longa.").queue();
			return;
		}

		com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(author.getId() + guild.getId());
		MessageBuilder mb = new MessageBuilder();
		mb.append(Helper.makeEmoteFromMention(args));

		try {
			Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro", Main.getShiroShards());
			Map<String, Runnable> s = Helper.sendEmotifiedString(guild, mb.getStringBuilder().toString());

			WebhookMessageBuilder wmb = new WebhookMessageBuilder();
			wmb.setContent(String.valueOf(s.keySet().toArray()[0]));
			if (m.getPseudoAvatar() == null || m.getPseudoAvatar().isBlank()) wmb.setAvatarUrl(author.getAvatarUrl());
			else try {
				wmb.setAvatarUrl(m.getPseudoAvatar());
			} catch (RuntimeException e) {
				m.setPseudoAvatar("");
				MemberDAO.updateMemberConfigs(m);
			}
			if (m.getPseudoName() == null || m.getPseudoName().isBlank()) wmb.setUsername(author.getName());
			else try {
				wmb.setUsername(m.getPseudoName());
			} catch (RuntimeException e) {
				m.setPseudoName("");
				MemberDAO.updateMemberConfigs(m);
			}

			assert wh != null;
			WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
			try {
				message.delete().queue(null, Helper::doNothing);
				wc.send(wmb.build()).thenAccept(rm -> s.get(String.valueOf(s.keySet().toArray()[0])).run()).get();
			} catch (InterruptedException | ExecutionException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		} catch (IndexOutOfBoundsException | InsufficientPermissionException | ErrorResponseException | NullPointerException | InterruptedException | ExecutionException e) {
			channel.sendMessage(mb.build()).queue();
			if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
				message.delete().queue(null, Helper::doNothing);
		}
	}
}
