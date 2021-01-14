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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class ServerInfoCommand extends Command {

	private static final String STR_BOT_INFO_SERVERS = "str_bot-info-servers";

	public ServerInfoCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ServerInfoCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ServerInfoCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ServerInfoCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		int bots = (int) guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();

		eb.setTitle(":dividers: | Dados sobre o servidor")
				.setThumbnail(guild.getIconUrl())
				.setImage(guild.getBannerUrl())
				.addField(":beginner: | Nome", guild.getName(), false)
				.addField(":name_badge: | Dono", guild.getOwner() == null ? "Desconhecido" : guild.getOwner().getUser().getAsTag(), false)
				.addField(":card_box: | Shard", "Nº " + guild.getJDA().getShardInfo().getShardId(), false)
				.addField(":busts_in_silhouette: | Membros", """
						Usuários: %s
						Bots: %s
						""".formatted(guild.getMemberCount() - bots, bots), false)
				.addField(":calendar: | Estou aqui desde", guild.getSelfMember().hasTimeJoined() ? guild.getSelfMember().getTimeJoined().format(Helper.onlyDate) : "Não lembro", true);

		channel.sendMessage(eb.build()).queue();
	}
}
