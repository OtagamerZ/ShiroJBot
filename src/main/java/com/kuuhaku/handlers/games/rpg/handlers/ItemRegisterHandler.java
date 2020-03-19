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
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.enums.Equipment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ItemRegisterHandler extends ListenerAdapter {
	private final TextChannel channel;
	private final JDA jda;
	private final User user;
	private String name = "";
	private String image = "";
	private String desc = "";
	private Equipment type = null;
	private int price = 0;
	private int attrib1 = 0;
	private int attrib2 = 0;
	private int attrib3 = 0;
	private int page = 0;
	private final EmbedBuilder eb = new EmbedBuilder();
	private Message msg;
	private final TextChannel chn;
	private final boolean[] complete = new boolean[]{false, false, false, false, false, false};

	private static final String PREVIOUS = "◀";
	private static final String CANCEL = "❎";
	private static final String NEXT = "▶";
	private static final String ACCEPT = "✅";

	public ItemRegisterHandler(TextChannel channel, JDA jda, User user) {
		this.channel = channel;
		this.jda = jda;
		this.user = user;
		this.chn = channel;
		eb.setTitle("Registro de item");
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
					if (Main.getInfo().getGames().get(event.getGuild().getId()).getItems().stream().anyMatch(i -> i.getName().equals(event.getMessage().getContentRaw()))) {
						channel.sendMessage(":x: | Este nome já está em uso.").queue();
						return;
					}
					name = event.getMessage().getContentRaw();
					event.getChannel().sendMessage("Nome trocado para **" + event.getMessage().getContentRaw() + "**!").queue();
					complete[0] = true;
					render(msg);
					break;
				case 2:
					image = event.getMessage().getContentRaw();
					try {
						HttpURLConnection con = (HttpURLConnection) new URL(image).openConnection();
						con.setRequestProperty("User-Agent", "Mozilla/5.0");
						BufferedImage map = ImageIO.read(con.getInputStream());

						event.getChannel().sendMessage("Imagem trocada com sucesso!").queue();
						complete[1] = true;
						render(msg);
					} catch (IOException e) {
						event.getChannel().sendMessage(":x: | Imagem inválida, veja se pegou o link corretamente.").queue();
					}
					break;
				case 3:
					desc = event.getMessage().getContentRaw();
					event.getChannel().sendMessage("Descrição trocada com sucesso!").queue();
					complete[2] = true;
					render(msg);
					break;
				case 4:
					type = Equipment.byName(event.getMessage().getContentRaw());
					event.getChannel().sendMessage("Tipo trocado com sucesso!").queue();
					complete[3] = true;
					render(msg);
					break;
				case 5:
					try {
						price = Integer.parseInt(event.getMessage().getContentRaw());
						event.getChannel().sendMessage("Valor trocado com sucesso!").queue();
						complete[4] = true;
						render(msg);
						break;
					} catch (NumberFormatException e) {
						event.getChannel().sendMessage(":x: | O preço deve ser um número inteiro.").queue();
						break;
					}
				case 6:
					String[] args = event.getMessage().getContentRaw().split(";");
					switch (type) {
						case HEAD:
						case CHEST:
						case LEG:
						case FOOT:
						case ARM:
						case NECK:
							attrib1 = Integer.parseInt(args[0]);
							attrib2 = Integer.parseInt(args[1]);
							break;
						case BAG:
							attrib1 = Integer.parseInt(args[0]);
							break;
						case RING:
						case WEAPON:
							attrib1 = Integer.parseInt(args[0]);
							attrib2 = Integer.parseInt(args[1]);
							attrib3 = Integer.parseInt(args[2]);
							break;
					}

					complete[5] = true;
					render(msg);
					event.getChannel().sendMessage("Registro completo!\nConfirme o cadastro na mensagem inicial ou use os botões para alterar valores anteriores!").queue();
					break;
			}
		} catch (NumberFormatException e) {
			event.getChannel().sendMessage(":x: | Os atributos devem ser números inteiros, separados por ponto e vírgula \";\".").queue();
		}
		event.getMessage().delete().queue();
	}

	@Override
	public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
		if (event.getUser().isBot() || event.getUser() != user || event.getChannel() != chn) return;
		switch (event.getReactionEmote().getName()) {
			case CANCEL:
				channel.sendMessage("Registro abortado!").queue();
				jda.removeEventListener(this);
				msg.delete().queue();
				break;
			case ACCEPT:
				switch (type) {
					case HEAD:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Head(name, desc, image, price, attrib1, attrib2));
						break;
					case CHEST:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Chest(name, desc, image, price, attrib1, attrib2));
						break;
					case LEG:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Leg(name, desc, image, price, attrib1, attrib2));
						break;
					case FOOT:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Foot(name, desc, image, price, attrib1, attrib2));
						break;
					case ARM:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Arm(name, desc, image, price, attrib1, attrib2));
						break;
					case NECK:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Neck(name, desc, image, price, attrib1, attrib2));
						break;
					case BAG:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Bag(name, desc, image, price, attrib1));
						break;
					case RING:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Ring(name, desc, image, price, attrib1, attrib2, attrib3));
						break;
					case WEAPON:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Weapon(name, desc, image, price, attrib1, attrib2, attrib3));
						break;
					case MISC:
						Main.getInfo().getGames().get(event.getGuild().getId()).addItem(new Item.Misc(name, desc, image, price));
						break;
				}
				jda.removeEventListener(this);
				channel.sendMessage("Registrado com sucesso!").queue();
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
			eb.setTitle("Registro de item");
			switch (page) {
				case 0:
				case 1:
					eb.setDescription("Digite um nome");
					msg.addReaction(CANCEL).queue(s -> {
						if (complete[0]) msg.addReaction(NEXT).queue();
					});
					break;
				case 2:
					eb.setDescription("Escolha uma imagem");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[1]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 3:
					eb.setDescription("Digite uma descrição");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[2]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 4:
					eb.setDescription("Escolha um tipo");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[3]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 5:
					eb.setDescription("Escolha um preço");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[4]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 6:
					eb.setDescription("Distribua os atributos do item");
					switch (type) {
						case HEAD:
							eb.addField("Defesa: " + attrib1, "", true);
							eb.addField("Inteligência: " + attrib2, "", true);
							break;
						case CHEST:
							eb.addField("Defesa: " + attrib1, "", true);
							eb.addField("Resistência: " + attrib2, "", true);
							break;
						case LEG:
							eb.addField("Defesa: " + attrib1, "", true);
							eb.addField("Sorte: " + attrib2, "", true);
							break;
						case FOOT:
							eb.addField("Defesa: " + attrib1, "", true);
							eb.addField("Agilidade: " + attrib2, "", true);
							break;
						case ARM:
							eb.addField("Defesa: " + attrib1, "", true);
							eb.addField("Força: " + attrib2, "", true);
							break;
						case NECK:
							eb.addField("Sorte: " + attrib1, "", true);
							eb.addField("Percepção: " + attrib2, "", true);
							break;
						case BAG:
							eb.addField("Capacidade: " + attrib1, "", true);
							break;
						case RING:
							eb.addField("Inteligência: " + attrib1, "", true);
							eb.addField("Carisma: " + attrib2, "", true);
							eb.addField("Sorte: " + attrib3, "", true);
							break;
						case WEAPON:
							eb.addField("Força: " + attrib1, "", true);
							eb.addField("Inteligência: " + attrib2, "", true);
							eb.addField("Defesa: " + attrib3, "", true);
							break;
						case MISC:
							eb.addField("Não possui atributos", "", true);
							break;
					}
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[5]) msg.addReaction(ACCEPT).queue();
							}));
					break;
			}
			if (complete[5]) {
				msg.editMessage(user.getAsMention()).embed(eb.build()).queue();
			} else {
				msg.editMessage(eb.build()).queue();
			}
		} catch (InterruptedException ignore) {
		}
	}
}
