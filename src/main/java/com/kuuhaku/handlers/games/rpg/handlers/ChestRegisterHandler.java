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
import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.handlers.games.rpg.entities.Chest;
import com.kuuhaku.handlers.games.rpg.entities.LootItem;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import com.kuuhaku.handlers.games.rpg.world.World;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChestRegisterHandler extends ListenerAdapter {
	private final TextChannel channel;
	private final JDA jda;
	private final User user;
	private final World world;
	private String name = "";
	private final List<LootItem> loot = new ArrayList<>();
	private int page = 0;
	private final EmbedBuilder eb = new EmbedBuilder();
	private Message msg;
	private final TextChannel chn;
	private final boolean[] complete = new boolean[]{false, false};

	private static final String PREVIOUS = "◀";
	private static final String CANCEL = "❎";
	private static final String NEXT = "▶";
	private static final String ACCEPT = "✅";

	public ChestRegisterHandler(TextChannel channel, JDA jda, User user, World world) {
		this.channel = channel;
		this.jda = jda;
		this.user = user;
		this.world = world;
		this.chn = channel;
		eb.setTitle("Registro de baú");
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
					if (Main.getInfo().getGames().get(event.getGuild().getId()).getChests().containsKey(event.getMessage().getContentRaw())) {
						channel.sendMessage(":x: | Este baú já existe.").queue();
						return;
					}
					name = event.getMessage().getContentRaw();
					event.getChannel().sendMessage("Nome trocado para **" + event.getMessage().getContentRaw() + "**!").queue();
					complete[0] = true;
					render(msg);
					break;
				case 2:
					String op = event.getMessage().getContentRaw().split(";")[0];
					String rarity = event.getMessage().getContentRaw().split(";")[1];
					Utils.checkRarity(op, rarity, loot, world, channel);

					eb.appendDescription("\n\n" + loot.stream().map(i -> "(" + i.getRarity().getName() + ") " + i.getItem().getName() + "\n").collect(Collectors.joining()));
					complete[1] = true;
					event.getChannel().sendMessage("Registro completo!\nConfirme o cadastro na mensagem inicial ou use os botões para alterar valores anteriores!").queue();
					render(msg);
					break;
			}
		} catch (UnknownItemException e) {
			event.getChannel().sendMessage(":x: | Item desconhecido.").queue();
		} catch (IndexOutOfBoundsException e) {
			event.getChannel().sendMessage(":x: | Formatação incorreta.").queue();
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
				eb.clearFields();
				eb.setDescription("Regitro completo");
				Main.getInfo().getGames().get(event.getGuild().getId()).addChest(new Chest(name, loot));
				jda.removeEventListener(this);
				msg.editMessage(eb.build()).queue();
				msg.clearReactions().queue();
				channel.sendMessage("Registrado com sucesso!").queue();
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
			eb.setTitle("Registro de baú");
			switch (page) {
				case 0:
				case 1:
					eb.setDescription("Digite um nome");
					msg.addReaction(CANCEL).queue(s -> {
						if (complete[0]) msg.addReaction(NEXT).queue();
					});
					break;
				case 2:
					eb.setDescription("Defina os espólios deste baú (coloque **+** antes do nome para adicionar e **-** para remover) e sua raridade (neste formato `NOME;RARIDADE`)");

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
		} catch (InterruptedException ignore) {
		}
	}
}
