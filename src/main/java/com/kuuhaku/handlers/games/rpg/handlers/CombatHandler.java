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

import com.kuuhaku.handlers.games.rpg.actors.Actor;
import com.kuuhaku.handlers.games.rpg.dialogs.CombatDialog;
import com.kuuhaku.handlers.games.rpg.exceptions.BadLuckException;
import com.kuuhaku.handlers.games.rpg.world.World;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

public class CombatHandler extends ListenerAdapter {
	private static final String ATTACK = "\u2694";
	private static final String DEFEND = "\uD83D\uDEE1";
	private static final String BAG = "\uD83D\uDCBC";
	private static final String FLEE = "\uD83C\uDFF3";
	private static final String BACK = "\u21A9";

	private final EmbedBuilder form = new EmbedBuilder();
	private final TextChannel channel;
	private final Actor.Player player;
	private final Actor.Monster monster;
	private final Map<Actor, Action> adbf = new Hashtable<>();
	private final World world;
	private final JDA api;

	private boolean playerTurn = true;
	private Message dialog;


	public CombatHandler(JDA api, World world, TextChannel channel, Actor.Player player, Actor.Monster monster) {
		api.addEventListener(this);
		this.world = world;
		this.api = api;
		this.player = player;
		this.monster = monster;
		this.channel = channel;
		form.setTitle("Resumo de combate");
		form.setDescription(getDialog());
		form.setThumbnail(playerTurn ? player.getCharacter().getImage() : monster.getMob().getImage());
		form.setFooter("Use as reações abaixo para interagir.", null);
		form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
		form.addField(monster.getMob().getName(), String.valueOf(monster.getMob().getStatus().getLife()), true);
		channel.sendMessage(form.build()).queue(c -> {
					this.dialog = c;
					regenReacts();
				}
		);

		adbf.put(player, new Action());
		adbf.put(monster, new Action());
	}

	private void changeTurn() {
		playerTurn = !playerTurn;
		if (playerTurn) {
			finishRound();
		} else {
			regenDialog();
		}
	}

	private void regenDialog() {
		if (adbf.get(player).isLookingBag()) {
			dialog.editMessage(player.getCharacter().openInventory()).queue(c -> regenReacts());
		} else {
			form.clear();
			form.setTitle("Resumo de combate");
			form.setDescription(getDialog());
			form.setThumbnail(playerTurn ? player.getCharacter().getImage() : monster.getMob().getImage());
			form.setFooter("Use as reações abaixo para interagir.", null);
			form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
			form.addField(monster.getMob().getName(), String.valueOf(monster.getMob().getStatus().getLife()), true);
			try {
				dialog.editMessage(form.build()).queue(c ->
						regenReacts()
				);
			} catch (NullPointerException e) {
				channel.sendMessage(form.build()).queue(c -> {
							this.dialog = c;
							regenReacts();
						}
				);
			}
		}
	}

	private void regenReacts() {
		if (adbf.get(player).isLookingBag()) {
			dialog.clearReactions().queue(r -> dialog.addReaction(BACK).queue());
		} else {
			if (!playerTurn) {
				dialog.clearReactions().queue(r -> {
					dialog.addReaction(ATTACK).queue();
					dialog.addReaction(DEFEND).queue();
					dialog.addReaction(FLEE).queue();
				});
			} else {
				dialog.clearReactions().queue(r -> {
					dialog.addReaction(ATTACK).queue();
					dialog.addReaction(DEFEND).queue();
					dialog.addReaction(BAG).queue();
					dialog.addReaction(FLEE).queue();
				});
			}
		}
	}

	private String getDialog() {
		return CombatDialog.dialog().replace("%actor%", playerTurn ? player.getCharacter().getName() : monster.getMob().getName());
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().isBot()) return;
		if (event.getUser() == world.getMaster() && !playerTurn) {
			switch (event.getReactionEmote().getName()) {
				case ATTACK:
					adbf.get(monster).setAttacking();
					break;
				case DEFEND:
					adbf.get(monster).setDefending();
					break;
				case FLEE:
					adbf.get(monster).setFleeing();
					break;
			}
			changeTurn();
		} else if (player.getId() == event.getUser().getIdLong() && playerTurn) {
			switch (event.getReactionEmote().getName()) {
				case ATTACK:
					adbf.get(player).setAttacking();
					changeTurn();
					break;
				case DEFEND:
					adbf.get(player).setDefending();
					changeTurn();
					break;
				case BAG:
					adbf.get(player).setLookingBag(true);
					regenDialog();
					break;
				case FLEE:
					adbf.get(player).setFleeing();
					changeTurn();
					break;
				case BACK:
					adbf.get(player).setLookingBag(false);
					regenDialog();
					break;
			}
		}
	}

	private void finishRound() {
		dialog.clearReactions().queue();

		if (!adbf.get(monster).isDefending() && (player.getCharacter().getStatus().getAgility() + 0.1f > monster.getMob().getStatus().getAgility() || adbf.get(player).isDefending())) {
			try {
				form.setTitle("Resultados da rodada");
				if (checkTurn()) return;

				if (checkOpponentTurn()) return;

				regenDialog();
			} catch (InterruptedException ignore) {
			}
		} else if (!adbf.get(player).isDefending() && (player.getCharacter().getStatus().getAgility() + 0.1f < monster.getMob().getStatus().getAgility() || adbf.get(monster).isDefending())) {
			try {
				form.setTitle("Resultados da rodada");
				if (checkOpponentTurn()) return;

				if (checkTurn()) return;

				regenDialog();
			} catch (InterruptedException ignore) {
			}
		}
	}

	private boolean checkOpponentTurn() throws InterruptedException {
		if (adbf.get(monster).isAttacking()) {
			form.setDescription(monster.getMob().getName() + " ataca!");
			player.getCharacter().getStatus().damage(monster.getMob().getStatus().getStrength(), adbf.get(player).isDefending());
			form.clearFields();
			form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
			form.addField(monster.getMob().getName(), String.valueOf(monster.getMob().getStatus().getLife()), true);
		} else if (adbf.get(monster).isDefending()) {
			form.setDescription(monster.getMob().getName() + " defende!");
		} else if (adbf.get(monster).isFleeing()) {
			form.setDescription(monster.getMob().getName() + " está tentando fugir!");
		}

		form.setThumbnail(player.getCharacter().getImage());
		dialog.editMessage(form.build()).queue();
		Thread.sleep(1750);
		return checkResult();
	}

	private boolean checkTurn() throws InterruptedException {
		if (adbf.get(player).isAttacking()) {
			form.setDescription(player.getCharacter().getName() + " ataca!");
			monster.getMob().getStatus().damage(player.getCharacter().getStatus().getStrength(), adbf.get(monster).isDefending());
			form.clearFields();
			form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
			form.addField(monster.getMob().getName(), String.valueOf(monster.getMob().getStatus().getLife()), true);
		} else if (adbf.get(player).isDefending()) {
			form.setDescription(player.getCharacter().getName() + " defende!");
		} else if (adbf.get(player).isFleeing()) {
			form.setDescription(player.getCharacter().getName() + " está tentando fugir!");
		}

		form.setThumbnail(monster.getMob().getImage());
		dialog.editMessage(form.build()).queue();
		Thread.sleep(1750);
		return checkResult();
	}

	private boolean checkResult() {
		if (!player.getCharacter().getStatus().isAlive()) {
			form.setTitle("Fim de combate: " + player.getCharacter().getName() + " foi derrotado!");
			form.setDescription("O fim de sua jornada chegou, porém um clérigo poderá retorná-lo à vida!");
			form.setThumbnail("https://www.shareicon.net/download/2015/09/26/107761_dead.ico");
			form.setColor(Color.red);
			dialog.editMessage(form.build()).queue();
			player.getCharacter().getStatus().addXp(-player.getCharacter().getStatus().getXp() / 2);
			api.removeEventListener(this);
			return true;
		} else if (!monster.getMob().getStatus().isAlive()) {
			form.setTitle("Fim de combate: " + monster.getMob().getName() + " venceu!");
			String result;
			try {
				result = "Você recebeu " + monster.getMob().dropLoot(player.getCharacter().getStatus().getLuck()).getName();
			} catch (BadLuckException e) {
				result = "Que azar! Você não ganhou nenhum item!";
			}
			form.setDescription(result);
			form.setThumbnail(player.getCharacter().getImage());
			form.setColor(Color.green);
			dialog.editMessage(form.build()).queue();
			player.getCharacter().getStatus().addXp(monster.getMob().getXp());
			api.removeEventListener(this);
			return true;
		}

		if (adbf.get(player).isFleeing()) {
			boolean success = player.getCharacter().getStatus().getAgility() >= monster.getMob().getStatus().getAgility() && new Random().nextInt(player.getCharacter().getStatus().getAgility() * 100) > monster.getMob().getStatus().getAgility() * 50;
			if (success) {
				form.setTitle("Fim de combate: " + player.getCharacter().getName() + " fugiu!");
				form.setDescription("Como os sabios dizem: \"Perder a batalha não significa perder a guerra!\".");
				form.setThumbnail(player.getCharacter().getImage());
				form.setColor(Color.yellow);
				dialog.editMessage(form.build()).queue();
				api.removeEventListener(this);
				return true;
			} else {
				try {
					form.setTitle(player.getCharacter().getName() + " não conseguiu fugir!");
					form.setDescription("");
					form.setThumbnail(player.getCharacter().getImage());
					form.setColor(null);
					dialog.editMessage(form.build()).queue();
					Thread.sleep(1750);
				} catch (InterruptedException ignore) {
				}
			}
		} else if (adbf.get(monster).isFleeing()) {
			boolean success = monster.getMob().getStatus().getAgility() >= player.getCharacter().getStatus().getAgility() && new Random().nextInt(monster.getMob().getStatus().getAgility() * 100) > player.getCharacter().getStatus().getAgility() * 50;
			if (success) {
				form.setTitle("Fim de combate: " + monster.getMob().getName() + " fugiu!");
				form.setDescription("A cada segundo que passa, o mal torna-se mais forte. Hei de voltar para derrotá-lo!");
				form.setThumbnail(monster.getMob().getImage());
				form.setColor(Color.yellow);
				dialog.editMessage(form.build()).queue();
				api.removeEventListener(this);
				return true;
			} else {
				try {
					form.setTitle(monster.getMob().getName() + " não conseguiu fugir!");
					form.setDescription("");
					form.setThumbnail(monster.getMob().getImage());
					form.setColor(null);
					dialog.editMessage(form.build()).queue();
					Thread.sleep(1750);
				} catch (InterruptedException ignore) {
				}
			}
		}

		adbf.put(player, new Action());
		adbf.put(monster, new Action());
		return false;
	}
}