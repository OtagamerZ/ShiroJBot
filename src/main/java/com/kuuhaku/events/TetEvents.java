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

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class TetEvents extends ListenerAdapter {

	@Override
	public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
		if (event.getAuthor().isBot() || !ExceedDAO.hasExceed(event.getAuthor().getId())) return;
		List<ExceedMember> ems = ExceedDAO.getExceedMembers(ExceedEnum.getByName(ExceedDAO.getExceed(event.getAuthor().getId())));
		ems.removeIf(em -> em.getUid().equals(event.getAuthor().getId()));

		EmbedBuilder eb;
		if (event.getAuthor().getAvatarUrl() != null) eb = new EmbedBuilder();
		else eb = new ColorlessEmbedBuilder();
		eb.setDescription(event.getMessage().getContentRaw());
		eb.setAuthor(event.getAuthor().getName(), event.getAuthor().getEffectiveAvatarUrl());
		eb.setThumbnail(event.getAuthor().getEffectiveAvatarUrl());
		eb.setFooter(event.getAuthor().getId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(event.getAuthor().getEffectiveAvatarUrl()));
		} catch (IOException ignore) {
		}

		for (ExceedMember em : ems) {
			Main.getTet().retrieveUserById(em.getUid())
					.flatMap(User::openPrivateChannel)
					.flatMap(c -> c.sendMessage(eb.build()))
					.queue(null, Helper::doNothing);
		}

		event.getChannel().sendMessage("✅ | Mensagem enviada aos outros " + ExceedDAO.getExceed(event.getAuthor().getId()) + "s com sucesso!").queue();
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (event.getMessage().getContentRaw().trim().equals("<@" + Main.getTet().getSelfUser().getId() + ">") || event.getMessage().getContentRaw().trim().equals("<@!" + Main.getTet().getSelfUser().getId() + ">")) {
			event.getChannel().sendMessage("Eae jogador! Vejo que você se interessou pelos Exceeds. Se você já escolheu um, você pode me enviar uma mensagem no canal privado que ela será transmitida à outros membros do mesmo Exceed, não é incrível?").queue();
		}
	}
}