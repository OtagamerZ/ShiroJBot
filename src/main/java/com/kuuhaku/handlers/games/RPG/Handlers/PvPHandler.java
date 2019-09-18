package com.kuuhaku.handlers.games.RPG.Handlers;

import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Dialogs.CombatDialog;
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

public class PvPHandler extends ListenerAdapter {
	private static final String ATTACK = "\u2694";
	private static final String DEFEND = "\uD83D\uDEE1";
	private static final String BAG = "\uD83D\uDCBC";
	private static final String FLEE = "\uD83C\uDFF3";
	private static final String BACK = "\u21A9";

	private final EmbedBuilder form = new EmbedBuilder();
	private final TextChannel channel;
	private final Actor.Player player;
	private final Actor.Player player2;
	private final Map<Actor, Action> adbf = new Hashtable<>();
	private final JDA api;

	private boolean playerTurn = true;
	private Message dialog;


	public PvPHandler(JDA api, TextChannel channel, Actor.Player player, Actor.Player player2) {
		api.addEventListener(this);
		this.api = api;
		this.player = player;
		this.player2 = player2;
		this.channel = channel;
		form.setTitle("Resumo de combate");
		form.setDescription(getDialog());
		form.setThumbnail(playerTurn ? player.getCharacter().getImage() : player2.getCharacter().getImage());
		form.setFooter("Use as reações abaixo para interagir.", null);
		form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
		form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
		channel.sendMessage(form.build()).queue(c -> {
					this.dialog = c;
					regenReacts();
				}
		);

		adbf.put(player, new Action());
		adbf.put(player2, new Action());
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
		} else if (adbf.get(player2).isLookingBag()) {
			dialog.editMessage(player2.getCharacter().openInventory()).queue(c -> regenReacts());
		} else {
			form.clear();
			form.setTitle("Resumo de combate");
			form.setDescription(getDialog());
			form.setThumbnail(playerTurn ? player.getCharacter().getImage() : player2.getCharacter().getImage());
			form.setFooter("Use as reações abaixo para interagir.", null);
			form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
			form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
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
		} else if (adbf.get(player2).isLookingBag()) {
			dialog.clearReactions().queue(r -> dialog.addReaction(BACK).queue());
		} else {
			if (!playerTurn) {
				dialog.clearReactions().queue(r -> {
					dialog.addReaction(ATTACK).queue();
					dialog.addReaction(DEFEND).queue();
					dialog.addReaction(BAG).queue();
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
		return CombatDialog.dialog().replace("%actor%", playerTurn ? player.getCharacter().getName() : player2.getCharacter().getName());
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().isBot()) return;
		if (player2.getId() == event.getUser().getIdLong() && !playerTurn) {
			switch (event.getReactionEmote().getName()) {
				case ATTACK:
					adbf.get(player2).setAttacking();
					changeTurn();
					break;
				case DEFEND:
					adbf.get(player2).setDefending();
					changeTurn();
					break;
				case BAG:
					adbf.get(player2).setLookingBag(true);
					regenDialog();
					break;
				case FLEE:
					adbf.get(player2).setFleeing();
					changeTurn();
					break;
				case BACK:
					adbf.get(player2).setLookingBag(false);
					regenDialog();
					break;
			}
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

		if (!adbf.get(player2).isDefending() && (player.getCharacter().getStatus().getAgility() + 0.1f > player2.getCharacter().getStatus().getAgility() || adbf.get(player).isDefending())) {
			try {
				form.setTitle("Resultados da rodada");
				if (adbf.get(player).isAttacking()) {
					form.setDescription(player.getCharacter().getName() + " ataca!");
					player2.getCharacter().getStatus().damage(player.getCharacter().getStatus().getStrength(), adbf.get(player2).isDefending());
					form.clearFields();
					form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
					form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
				} else if (adbf.get(player).isDefending()) {
					form.setDescription(player.getCharacter().getName() + " defende!");
				} else if (adbf.get(player).isFleeing()) {
					form.setDescription(player.getCharacter().getName() + " está tentando fugir!");
				}

				form.setThumbnail(player.getCharacter().getImage());
				dialog.editMessage(form.build()).queue();
				Thread.sleep(1750);
				if (checkResult()) {
					return;
				}

				if (adbf.get(player2).isAttacking()) {
					form.setDescription(player2.getCharacter().getName() + " ataca!");
					player.getCharacter().getStatus().damage(player2.getCharacter().getStatus().getStrength(), adbf.get(player).isDefending());
					form.clearFields();
					form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
					form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
				} else if (adbf.get(player2).isDefending()) {
					form.setDescription(player2.getCharacter().getName() + " defende!");
				} else if (adbf.get(player2).isFleeing()) {
					form.setDescription(player2.getCharacter().getName() + " está tentando fugir!");
				}

				form.setThumbnail(player2.getCharacter().getImage());
				dialog.editMessage(form.build()).queue();
				Thread.sleep(1750);
				if (checkResult()) {
					return;
				}

				regenDialog();
				adbf.put(player, new Action());
				adbf.put(player2, new Action());
			} catch (InterruptedException ignore) {
			}
		} else if (!adbf.get(player).isDefending() && (player.getCharacter().getStatus().getAgility() + 0.1f < player2.getCharacter().getStatus().getAgility() || adbf.get(player2).isDefending())) {
			try {
				form.setTitle("Resultados da rodada");
				if (adbf.get(player2).isAttacking()) {
					form.setDescription(player2.getCharacter().getName() + " ataca!");
					player.getCharacter().getStatus().damage(player2.getCharacter().getStatus().getStrength(), adbf.get(player).isDefending());
					form.clearFields();
					form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
					form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
				} else if (adbf.get(player2).isDefending()) {
					form.setDescription(player2.getCharacter().getName() + " defende!");
				} else if (adbf.get(player2).isFleeing()) {
					form.setDescription(player2.getCharacter().getName() + " está tentando fugir!");
				}

				form.setThumbnail(player2.getCharacter().getImage());
				dialog.editMessage(form.build()).queue();
				Thread.sleep(1750);
				if (checkResult()) {
					return;
				}

				if (adbf.get(player).isAttacking()) {
					form.setDescription(player.getCharacter().getName() + " ataca!");
					player2.getCharacter().getStatus().damage(player.getCharacter().getStatus().getStrength(), adbf.get(player2).isDefending());
					form.clearFields();
					form.addField(player.getCharacter().getName(), String.valueOf(player.getCharacter().getStatus().getLife()), true);
					form.addField(player2.getCharacter().getName(), String.valueOf(player2.getCharacter().getStatus().getLife()), true);
				} else if (adbf.get(player).isDefending()) {
					form.setDescription(player.getCharacter().getName() + " defende!");
				} else if (adbf.get(player).isFleeing()) {
					form.setDescription(player.getCharacter().getName() + " está tentando fugir!");
				}

				form.setThumbnail(player.getCharacter().getImage());
				dialog.editMessage(form.build()).queue();
				Thread.sleep(1750);
				if (checkResult()) {
					return;
				}

				regenDialog();
				adbf.put(player, new Action());
				adbf.put(player2, new Action());
			} catch (InterruptedException ignore) {
			}
		}
	}

	private boolean checkResult() {
		if (!player.getCharacter().getStatus().isAlive()) {
			form.setTitle("Fim de combate: " + player.getCharacter().getName() + " foi derrotado!");
			form.setDescription("O fim de sua jornada chegou, porém um clérigo poderá retorná-lo à vida!");
			form.setThumbnail(player.getCharacter().getImage());
			form.setColor(Color.green);
			dialog.editMessage(form.build()).queue();
			player.getCharacter().getStatus().addXp(player2.getCharacter().getStatus().getXp() / 2);
			player2.getCharacter().getStatus().addXp(-player2.getCharacter().getStatus().getXp() / 2);
			api.removeEventListener(this);
			return true;
		} else if (!player2.getCharacter().getStatus().isAlive()) {
			form.setTitle("Fim de combate: " + player2.getCharacter().getName() + " foi derrotado!");
			form.setDescription("O fim de sua jornada chegou, porém um clérigo poderá retorná-lo à vida!");
			form.setThumbnail(player2.getCharacter().getImage());
			form.setColor(Color.green);
			dialog.editMessage(form.build()).queue();
			player2.getCharacter().getStatus().addXp(player.getCharacter().getStatus().getXp() / 2);
			player.getCharacter().getStatus().addXp(-player.getCharacter().getStatus().getXp() / 2);
			api.removeEventListener(this);
			return true;
		}

		if (adbf.get(player).isFleeing()) {
			boolean success = player.getCharacter().getStatus().getAgility() >= player2.getCharacter().getStatus().getAgility() && new Random().nextInt(player.getCharacter().getStatus().getAgility() * 100) > player2.getCharacter().getStatus().getAgility() * 50;
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
		} else if (adbf.get(player2).isFleeing()) {
			boolean success = player2.getCharacter().getStatus().getAgility() >= player.getCharacter().getStatus().getAgility() && new Random().nextInt(player2.getCharacter().getStatus().getAgility() * 100) > player.getCharacter().getStatus().getAgility() * 50;
			if (success) {
				form.setTitle("Fim de combate: " + player2.getCharacter().getName() + " fugiu!");
				form.setDescription("Como os sabios dizem: \"Perder a batalha não significa perder a guerra!\".");
				form.setThumbnail(player2.getCharacter().getImage());
				form.setColor(Color.yellow);
				dialog.editMessage(form.build()).queue();
				api.removeEventListener(this);
				return true;
			} else {
				try {
					form.setTitle(player2.getCharacter().getName() + " não conseguiu fugir!");
					form.setDescription("");
					form.setThumbnail(player2.getCharacter().getImage());
					form.setColor(null);
					dialog.editMessage(form.build()).queue();
					Thread.sleep(1750);
				} catch (InterruptedException ignore) {
				}
			}
		}

		return false;
	}
}