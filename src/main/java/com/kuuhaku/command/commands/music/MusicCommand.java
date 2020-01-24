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

package com.kuuhaku.command.commands.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

public class MusicCommand extends Command {

	public MusicCommand() {
		super("musica", new String[]{"music", "m"}, "Controla a fila de músicas.", Category.MUSICA);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle("Comandos de controle de música");
			eb.addField(prefix + "m resume", "Continua a fila de músicas caso esteja pausada.", true);
			eb.addField(prefix + "m pause", "Pausa a fila de músicas.", true);
			eb.addField(prefix + "m clear", "Para e limpa a fila de músicas.", true);
			eb.addField(prefix + "m skip", "Pula a música atual.", true);
			eb.addField(prefix + "m volume", "Define o volume do som.", true);
			eb.addField(prefix + "m info", "Mostra a música atual.", true);
			eb.addField(prefix + "m queue", "Mostra a fila atual.", true);

			channel.sendMessage(eb.build()).queue();
			return;
		}
		switch (args[0]) {
			case "resume":
				Music.resumeTrack((TextChannel) channel);
				break;
			case "pause":
				Music.pauseTrack((TextChannel) channel);
				break;
			case "clear":
				Music.clearQueue((TextChannel) channel);
				break;
			case "skip":
				Music.skipTrack((TextChannel) channel);
				break;
			case "volume":
				if (args.length > 1 && StringUtils.isNumeric(args[1])) Music.setVolume((TextChannel) channel, Integer.parseInt(args[1]));
				else channel.sendMessage(":x: | O volume deve ser um valor inteiro entre 0 e 100").queue();
				break;
			case "info":
				Music.trackInfo((TextChannel) channel);
				break;
			case "queue":
				Music.queueInfo((TextChannel) channel);
				break;
			default:
				channel.sendMessage(":x: | Comando de música inválido.").queue();
		}
	}
}
