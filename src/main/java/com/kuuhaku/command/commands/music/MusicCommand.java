package com.kuuhaku.command.commands.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

public class MusicCommand extends Command {

	public MusicCommand() {
		super("musica", new String[]{"music", "m"}, "Controla a fila de músicas.", Category.MUSICA);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle("Comandos de controle de música");
			eb.addField(prefix + "resume", "Continua a fila de músicas caso esteja pausada.", true);
			eb.addField(prefix + "pause", "Pausa a fila de músicas.", true);
			eb.addField(prefix + "clear", "Para e limpa a fila de músicas.", true);
			eb.addField(prefix + "skip", "Pula a música atual.", true);
			eb.addField(prefix + "volume", "Define o volume do som.", true);
			eb.addField(prefix + "info", "Mostra a música atual.", true);

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
			default:
				channel.sendMessage(":x: | Comando de música inválido.").queue();
		}
	}
}
