/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.games;

import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.DuelData;
import com.kuuhaku.model.Special;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.util.Random;

public class Beyblade{
	public static void play(GuildMessageReceivedEvent event) {
		@SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = ShiroInfo.dd.stream().filter(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor()).findFirst().get();
		boolean player1Turn = duel.isP1turn();
		EmbedBuilder eb = new EmbedBuilder();

		if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				if (hit(duel.getB1().getSpeed(), duel.getB2().getStability(), duel.getM2())) {
					duel.setP1turn(false);
					duel.setD1(false);
					if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
					int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
					duel.getB2().setLife(duel.getB2().getLife() - damage);
					Helper.log(Helper.class, LogLevel.DEBUG, damage + " -> " + duel.getB2().getLife());
					duel.clearM2();
				} else {
					duel.setP1turn(false);
					duel.addM2();
					event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " erra o ataque (" + Helper.round((duel.getB1().getSpeed() * 100) / (duel.getB2().getStability() * 2) / duel.getM2(), 1) + "% de chance), agora é a vez de " + duel.getB2().getName()).queue();
				}
			} else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
				if (hit(duel.getB2().getSpeed(), duel.getB1().getStability(), duel.getM1())) {
					duel.setP1turn(true);
					duel.setD2(false);
					if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
					int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
					duel.getB1().setLife(duel.getB1().getLife() - damage);
					Helper.log(Helper.class, LogLevel.DEBUG, damage + " -> " + duel.getB1().getLife());
					duel.clearM1();
				} else {
					duel.setP1turn(true);
					duel.addM1();
					event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " erra o ataque (" + Helper.round((duel.getB2().getSpeed() * 100) / (duel.getB1().getStability() * 2) / duel.getM1(), 1) + "% de chance), agora é a vez de " + duel.getB1().getName()).queue();
				}
			}
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				duel.setP1turn(false);
				duel.setD1(true);
				event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB2().getName()).queue();
			} else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
				duel.setP1turn(true);
				duel.setD2(true);
				event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB1().getName()).queue();
			}
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("especial")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				if (duel.getB1().getS() != null) {
					if (!duel.isS1()) {
						duel.setP1turn(false);
						int chance = Helper.rng(100);
						duel.setD1(false);
						if (Special.trySpecial(chance, getDefFac(duel.isD2(), duel.getB2()), duel.getB1(), duel.getB2(), event)) {
							duel.setS1(true);
							switch (duel.getB1().getSpecial()) {
								case 12: duel.setP1turn(true);
								case 31: duel.setD1(true);
							}
						}
					} else {
						event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB1().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
					}
				} else {
					event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ainda não possui um alinhamento!").queue();
				}
			} else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
				if (duel.getB2().getS() != null) {
					if (!duel.isS2()) {
						duel.setP1turn(true);
						int chance = Helper.rng(100);
						duel.setD2(false);
						if (Special.trySpecial(chance, getDefFac(duel.isD2(), duel.getB2()), duel.getB2(), duel.getB1(), event)) {
							duel.setS2(true);
							switch (duel.getB1().getSpecial()) {
								case 12: duel.setP1turn(false);
								case 31: duel.setD2(true);
							}
						}
					} else {
						event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB2().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
					}
				} else {
					event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ainda não possui um alinhamento!").queue();
				}
			}
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("desistir")) {
			if (event.getMessage().getAuthor() == duel.getP1()) {
				event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " desistiu. A vitória é de " + duel.getP2().getAsMention() + "!").queue();
				com.kuuhaku.model.Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
				assert bl != null;
				bl.addLoses();
				MySQL.sendBeybladeToDB(bl);

				com.kuuhaku.model.Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
				assert bb != null;
				bb.addWins();
				MySQL.sendBeybladeToDB(bb);
				ShiroInfo.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
			} else {
				event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue();
				com.kuuhaku.model.Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
				assert bl != null;
				bl.addLoses();
				MySQL.sendBeybladeToDB(bl);

				com.kuuhaku.model.Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
				assert bb != null;
				bb.addWins();
				MySQL.sendBeybladeToDB(bb);
				ShiroInfo.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
			}
		}
		if (duel.getB2().getLife() <= 0) {
			int pointWin = Helper.rng(Math.round((duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability()) * duel.getB2().getKDA()));
			event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
			com.kuuhaku.model.Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
			assert bl != null;
			bl.addLoses();
			MySQL.sendBeybladeToDB(bl);

			com.kuuhaku.model.Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
			assert bb != null;
			bb.addWins();
			bb.addPoints(pointWin == 0 ? 5 : pointWin);
			MySQL.sendBeybladeToDB(bb);
			ShiroInfo.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
		} else if (duel.getB1().getLife() <= 0) {
			int pointWin = Helper.rng(Math.round((duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability()) * duel.getB1().getKDA()));
			event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
			com.kuuhaku.model.Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
			assert bl != null;
			bl.addLoses();
			MySQL.sendBeybladeToDB(bl);

			com.kuuhaku.model.Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
			assert bb != null;
			bb.addWins();
			bb.addPoints(pointWin == 0 ? 5 : pointWin);
			MySQL.sendBeybladeToDB(bb);
			ShiroInfo.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar") || event.getMessage().getContentRaw().equalsIgnoreCase("especial") || event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
			if (event.getMessage().getAuthor() == duel.getP1() && !player1Turn) {
				event.getMessage().getChannel().sendMessage("Ainda não é seu turno " + event.getMessage().getAuthor().getAsMention() + ", por favor aguarde seu oponente agir.").queue();
				return;
			} else if (event.getMessage().getAuthor() == duel.getP2() && player1Turn) {
				event.getMessage().getChannel().sendMessage("Ainda não é seu turno " + event.getMessage().getAuthor().getAsMention() + ", por favor aguarde seu oponente agir.").queue();
				return;
			}
			eb.setTitle("Dados do duelo:");
			eb.setColor(Color.decode(player1Turn ? duel.getB1().getColor() : duel.getB2().getColor()));
			eb.setDescription("**" + duel.getB1().getName() + "** :vs: **" + duel.getB2().getName() + "**");
			eb.addField(duel.getB1().getName(), "Vida: " + duel.getB1().getLife() + "\n\nForça: " + duel.getB1().getStrength() + "\nVelocidade: " + duel.getB1().getSpeed() + "\nEstabilidade: " + duel.getB1().getStability() + "\nTipo: " + (duel.getB1().getS() == null ? "Não possui" : duel.getB1().getS().getType()), true);
			eb.addField(duel.getB2().getName(), "Vida: " + duel.getB2().getLife() + "\n\nForça: " + duel.getB2().getStrength() + "\nVelocidade: " + duel.getB2().getSpeed() + "\nEstabilidade: " + duel.getB2().getStability() + "\nTipo: " + (duel.getB2().getS() == null ? "Não possui" : duel.getB2().getS().getType()), true);
			event.getMessage().getChannel().sendMessage((!player1Turn ? duel.getB1().getName() : duel.getB2().getName()) + " age, agora é a vez de " + (player1Turn ? duel.getB1().getName() : duel.getB2().getName())).embed(eb.build()).queue();
			ShiroInfo.dd.stream().filter(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor()).findFirst().ifPresent(m -> {
				m.getB1().setLife(duel.getB1().getLife());
				m.getB2().setLife(duel.getB2().getLife());
			});
		}
	}

	private static float getDefFac(boolean defending, com.kuuhaku.model.Beyblade b) {
		if (defending) {
			if (b.getS() == null) {
				return b.getStability();
			} else {
				if (b.getS().isBear()) {
					return 2.0f + (b.getStability() / 2);
				} else {
					return b.getStability();
				}
			}
		} else {
			return 1.0f;
		}
	}

	private static boolean hit(float speed, float stability, int modifier) {
		return (new Random().nextFloat() * 100) < ((speed * 50) / (stability * 2)) / modifier;
	}
}
