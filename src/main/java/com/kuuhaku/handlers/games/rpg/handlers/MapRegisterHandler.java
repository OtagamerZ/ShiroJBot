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

package com.kuuhaku.handlers.games.rpg.handlers;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.rpg.exceptions.NameTakenException;
import com.kuuhaku.handlers.games.rpg.world.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class MapRegisterHandler extends ListenerAdapter {
	private final TextChannel channel;
	private final JDA jda;
	private final User user;
	private String image = "";
	private int[] startPos = new int[2];
	private int page = 0;
	private final EmbedBuilder eb = new EmbedBuilder();
	private Message msg;
	private final TextChannel chn;
	private final boolean[] complete = new boolean[]{false, false};

	private static final String PREVIOUS = "\u25C0";
	private static final String CANCEL = "\u274E";
	private static final String NEXT = "\u25B6";
	private static final String ACCEPT = "\u2705";

	public MapRegisterHandler(TextChannel channel, JDA jda, User user) {
		this.channel = channel;
		this.jda = jda;
		this.user = user;
		this.chn = channel;
		eb.setTitle("Registro de mapa");
		eb.setDescription("Clique nas setas para mudar as páginas.");
		channel.sendMessage(eb.build()).queue(m -> {
			msg = m;
			msg.addReaction(CANCEL).queue();
			msg.addReaction(NEXT).queue();
		});
		jda.addEventListener(this);
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor() != user || event.getChannel() != chn) return;
		try {
			switch (page) {
				case 0:
					msg.addReaction(NEXT).queue();
					render(msg);
					break;
				case 1:
					if (Main.getInfo().getGames().get(event.getGuild().getId()).getPlayers().values().stream().anyMatch(p -> p.getCharacter().getName().equals(event.getMessage().getContentRaw())))
						throw new NameTakenException();
					image = event.getMessage().getContentRaw();
					try {
						HttpURLConnection con = (HttpURLConnection) new URL(image).openConnection();
						con.setRequestProperty("User-Agent", "Mozilla/5.0");
						BufferedImage map = ImageIO.read(con.getInputStream());

						event.getChannel().sendMessage("Mapa trocado com sucesso!").queue();
						complete[0] = true;
						render(msg);
					} catch (IOException e) {
						e.printStackTrace();
						event.getChannel().sendMessage(":x: | Imagem inválida, veja se pegou o link corretamente.").queue();
					}
					break;
				case 2:
					String[] args = event.getMessage().getContentRaw().split(";");
					startPos = Arrays.stream(args).mapToInt(Integer::parseInt).toArray();

					complete[1] = true;
					render(msg);
					event.getChannel().sendMessage("Registro completo!\nConfirme o cadastro na mensagem inicial ou use os botões para alterar valores anteriores!").queue();
					break;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | As coordenadas devem ser números inteiros.").queue();
		}
		event.getMessage().delete().queue();
	}

	@Override
	public void onGenericGuildMessageReaction(GenericGuildMessageReactionEvent event) {
		if (event.getUser().isBot() || event.getUser() != user || event.getChannel() != chn) return;
		switch (event.getReactionEmote().getName()) {
			case CANCEL:
				channel.sendMessage("Registro abortado!").queue();
				jda.removeEventListener(this);
				msg.delete().queue();
				break;
			case ACCEPT:
				try {
					Main.getInfo().getGames().get(event.getGuild().getId()).addMap(new Map(image, startPos));
					jda.removeEventListener(this);
					channel.sendMessage("Registrado com sucesso!").queue();
				} catch (IllegalArgumentException e) {
					channel.sendMessage(":x: | Mapa com tamanho muito grande, por favor escolha imagem.").queue();
				} catch (IOException ignore) {
				}
				msg.clearReactions().queue();
				break;
			case PREVIOUS:
				page--;
				render(msg);
				break;
			case NEXT:
				page++;
				render(msg);
				break;
		}
	}

	private void render(Message msg) {
		try {
			msg.clearReactions().complete();
			Thread.sleep(500);
			eb.clear();
			eb.setTitle("Registro de mapa");
			switch (page) {
				case 0:
				case 1:
					eb.setDescription("Escolha uma imagem de mapa (deve possuir altura e largura menor que 1664px)");
					msg.addReaction(CANCEL).queue(s -> {
						if (complete[0]) msg.addReaction(NEXT).queue();
					});
					break;
				case 2:
					eb.setDescription("Digite a posição inicial dos jogadores no seguinte formato `X;Y`");

					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[1]) msg.addReaction(ACCEPT).queue();
							}));
					break;
			}
			if (complete[1]) {
				msg.editMessage(user.getAsMention()).embed(eb.build()).queue();
			} else {
				msg.editMessage(eb.build()).queue();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
