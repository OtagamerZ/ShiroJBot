package com.kuuhaku.command.commands.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;
import java.util.Objects;

public class VideoCommand extends Command {

	public VideoCommand() {
		super("video", new String[]{"vid"}, "Busca um vídeo específico no YouTube.", Category.MUSICA);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa digitar um nome para pesquisar.").queue();
			return;
		}
		channel.sendMessage("<a:Loading:598500653215645697> Buscando video...").queue(m -> {
			try {
				YoutubeVideo v = Youtube.getSingleData(String.join(" ", args));
				EmbedBuilder eb = new EmbedBuilder();

				m.editMessage(":mag: Resultados da busca").queue(s -> {
					try {
						eb.setTitle(v.getTitle(), v.getUrl());
						eb.setDescription(v.getDesc());
						eb.setImage(v.getThumb());
						eb.setColor(Helper.colorThief(v.getThumb()));
						eb.setFooter("Link: " + v.getUrl(), null);
						channel.sendMessage(eb.build()).queue(msg -> {
							if (Objects.requireNonNull(member.getVoiceState()).inVoiceChannel()) msg.addReaction("\u25B6").queue();
						});
					} catch (IOException e) {
						m.editMessage(":x: | Nenhum vídeo encontrado.").queue();
						Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
					}
				});
			} catch (IOException e) {
				m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
