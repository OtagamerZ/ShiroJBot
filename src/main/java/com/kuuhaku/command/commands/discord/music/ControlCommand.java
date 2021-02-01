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

package com.kuuhaku.command.commands.discord.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class ControlCommand implements Executable {

	public ControlCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ControlCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ControlCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ControlCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle("Comandos de controle de música");
			eb.addField(prefix + "c resume", "Continua a fila de músicas caso esteja pausada.", true);
			eb.addField(prefix + "c pause", "Pausa a fila de músicas.", true);
			eb.addField(prefix + "c clear", "Para e limpa a fila de músicas.", true);
			eb.addField(prefix + "c skip", "Pula a música atual.", true);
			eb.addField(prefix + "c volume", "Define o volume do som.", true);
			eb.addField(prefix + "c info", "Mostra a música atual.", true);
			eb.addField(prefix + "c queue", "Mostra a fila atual.", true);

			channel.sendMessage(eb.build()).queue();
			return;
		}

		if (Music.getGuildAudioPlayer(guild, (TextChannel) channel).player.getPlayingTrack() == null) {
			channel.sendMessage("❌ | Não há nenhuma música tocando no momento.").queue();
			return;
		} else if (member.getVoiceState().getChannel() == null || !member.getVoiceState().getChannel().getMembers().contains(guild.getSelfMember())) {
			channel.sendMessage("❌ | Este comando só pode ser usado se estiver em um canal de voz com a Shiro.").queue();
			return;
		} else if (!Helper.hasPermission(member, PrivilegeLevel.MOD) && !((User) Music.getGuildAudioPlayer(guild, (TextChannel) channel).player.getPlayingTrack().getUserData()).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Apenas quem adicionou esta música pode controlá-la (exceto moderadores).").queue();
			return;
		}

		switch (args[0]) {
			case "resume" -> Music.resumeTrack((TextChannel) channel);
			case "pause" -> Music.pauseTrack((TextChannel) channel);
			case "clear" -> Music.clearQueue((TextChannel) channel);
			case "skip" -> Music.skipTrack((TextChannel) channel);
			case "volume" -> {
				if (args.length > 1 && StringUtils.isNumeric(args[1]))
					Music.setVolume((TextChannel) channel, Integer.parseInt(args[1]));
				else channel.sendMessage("❌ | O volume deve ser um valor inteiro entre 0 e 100.").queue();
			}
			case "info" -> Music.trackInfo((TextChannel) channel);
			case "queue" -> Music.queueInfo((TextChannel) channel);
			default -> channel.sendMessage("❌ | Comando de música inválido.").queue();
		}
	}
}
