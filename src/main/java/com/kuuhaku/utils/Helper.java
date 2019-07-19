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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.DuelData;
import com.kuuhaku.model.ReactionsList;
import com.kuuhaku.model.guildConfig;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

	public static final String VOID = "\u200B";

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (Main.getInfo().getNiiChan().contains(member.getUser().getId())) {
			return PrivilegeLevel.NIICHAN;
		} else if (Main.getInfo().getDevelopers().contains(member.getUser().getId())) {
			return PrivilegeLevel.DEV;
		} else if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
			return PrivilegeLevel.MOD;
		}
		return PrivilegeLevel.USER;
	}

	public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
		return getPrivilegeLevel(member).hasAuthority(privilegeLevel);
	}

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static String downloadWebPage(String webpage) throws Exception {
		URL url = new URL(webpage);
		BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openStream()));

		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = rdr.readLine()) != null)
			sb.append(line).append("\n");

		rdr.close();

		return sb.toString();
	}

	public static boolean findURL(String text) {
		final Pattern urlPattern = Pattern.compile(
				".*?(?:^|[\\W])((ht|f)tp(s?)://|www\\.)"
						+ "(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*"
						+ "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?)",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = text.replace("1", "i").replace("!", "i");
		text = text.replace("3", "e");
		text = text.replace("4", "a");
		text = text.replace("5", "s");
		text = text.replace("7", "t");
		text = text.replace("0", "o");
		text = text.replace(" ", "");

		final Matcher msg = urlPattern.matcher(text.toLowerCase());
		return msg.matches();
	}

	public static void sendPM(User user, String message) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue());
	}

	public static void purge(MessageChannel channel, int num) {
		MessageHistory history = new MessageHistory(channel);
		history.retrievePast(num).queue(channel::purgeMessages);
	}

	public static String getCustomEmoteMention(Guild guild, String name) {
		for (Emote em : guild.getEmotes()) {
			if (em.getName().equalsIgnoreCase(name))
				return em.getAsMention();
		}
		return null;
	}

	public static Emote getCustomEmote(Guild guild, String name) {
		for (Emote em : guild.getEmotes()) {
			if (em.getName().equalsIgnoreCase(name))
				return em;
		}
		return null;
	}

	public static void typeMessage(MessageChannel channel, String message) {
		channel.sendTyping().queue(tm -> channel.sendMessage(message).queueAfter(message.length() * 25 > 10000 ? 10000 : message.length(), TimeUnit.MILLISECONDS));
	}

	public static void sendReaction(String imageURL, MessageChannel channel, String message, boolean reacted) {
		try {
			if (ImageIO.read(getImage(imageURL)).getWidth() >= 400) {
				EmbedBuilder eb = new EmbedBuilder();
				eb.setImage(imageURL);
				if (reacted)
					channel.sendMessage(message).embed(eb.build()).queue(m -> m.addReaction("\u21aa").queue());
				else channel.sendMessage(message).embed(eb.build()).queue();
			} else {
				EmbedBuilder eb = new EmbedBuilder();
				eb.setImage(imageURL);
				if (reacted)
					channel.sendMessage(message + "\n:warning: | GIF com proporções irregulares, os desenvolvedores já foram informados.").embed(eb.build()).queue(m -> m.addReaction("\u21aa").queue());
				else
					channel.sendMessage(message + "\n:warning: | GIF com proporções irregulares, os desenvolvedores já foram informados.").embed(eb.build()).queue();
				log(ReactionsList.class, LogLevel.WARN, "GIF irregular: " + imageURL);
				Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("GIF irregular: " + imageURL).queue()));
			}
		} catch (Exception e) {
			log(Helper.class, LogLevel.ERROR, "Erro ao carregar a imagem: " + e.getStackTrace()[0]);
		}
	}

	private static float getDefFac(boolean defending, Beyblade b) {
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
		return (new Random().nextFloat() * 100) > ((speed * 100) / (stability * 2)) / modifier;
	}

	public static int rng(int maxValue) {
		return Math.abs(new Random().nextInt(maxValue));
	}

	public static Color colorThief(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage icon = ImageIO.read(con.getInputStream());

		return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
	}

	public static void battle(GuildMessageReceivedEvent event) {
		if (JDAEvents.dd.stream().noneMatch(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor())) {
			return;
		}
		@SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = JDAEvents.dd.stream().filter(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor()).findFirst().get();
		boolean player1Turn = duel.isP1turn();
		EmbedBuilder eb = new EmbedBuilder();

		if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				if (hit(duel.getB1().getSpeed(), duel.getB2().getStability(), duel.getM2())) {
					duel.setP1turn(false);
					duel.setD1(false);
					if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
					int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
					duel.getB2().setLife(duel.getB2().getLife() - damage);
					Helper.log(Helper.class, LogLevel.DEBUG, damage + " -> " + duel.getB2().getLife());
					event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue();
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
					int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
					duel.getB1().setLife(duel.getB1().getLife() - damage);
					Helper.log(Helper.class, LogLevel.DEBUG, damage + " -> " + duel.getB1().getLife());
					event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue();
					duel.clearM1();
				} else {
					duel.setP1turn(true);
					duel.addM1();
					event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " erra o ataque (" + Helper.round((duel.getB2().getSpeed() * 100) / (duel.getB1().getStability() * 2) / duel.getM1(), 1) + "), agora é a vez de " + duel.getB1().getName()).queue();
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
			if (duel.getB1().getS() != null) {
				if (!duel.isS1()) {
					if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
						duel.setP1turn(false);
						int chance = Helper.rng(100);
						duel.setD1(false);
						final Consumer<Void> Miss = tm -> event.getMessage().getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
						switch (duel.getB1().getSpecial()) {
							case 11:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB1().getName() + " desapareceu? Ah, lá está ela, com um movimento digno dos tigres ela executa o golpe especial " + duel.getB1().getS().getName() + "! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 12:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getSpeed() * 2 / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB1().getName() + " executou com perfeição em " + duel.getB1().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
									duel.setP1turn(true);
									event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 21:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB2().getStability() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB1().getName() + " lançou " + duel.getB1().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 22:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getStrength() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 150));
									event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB1().getName() + " provou que é possivel!!! " + duel.getB1().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 31:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB1().getS().setBear(true);
									duel.setD1(true);
									event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB1().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
									duel.setS1(true);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
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
						final Consumer<Void> Miss = tm -> event.getMessage().getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
						switch (duel.getB2().getSpecial()) {
							case 11:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB2().getName() + " desapareceu? Ah, lá está ele, com um movimento digno dos tigres ele executa o golpe especial " + duel.getB2().getS().getName() + "! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 12:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getSpeed() * 2 / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB2().getName() + " executou com perfeição em " + duel.getB2().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
									duel.setP1turn(false);
									event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 21:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB1().getStability() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB2().getName() + " lançou " + duel.getB2().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 22:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getStrength() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 150));
									event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB2().getName() + " provou que é possivel!!! " + duel.getB2().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 31:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB2().getS().setBear(true);
									duel.setD2(true);
									event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB2().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
									duel.setS2(true);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
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
				Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
				assert bl != null;
				bl.addLoses();
				MySQL.sendBeybladeToDB(bl);

				Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
				assert bb != null;
				bb.addWins();
				MySQL.sendBeybladeToDB(bb);
				JDAEvents.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
			} else {
				event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue();
				Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
				assert bl != null;
				bl.addLoses();
				MySQL.sendBeybladeToDB(bl);

				Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
				assert bb != null;
				bb.addWins();
				MySQL.sendBeybladeToDB(bb);
				JDAEvents.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
			}
		}
		if (duel.getB2().getLife() <= 0) {
			int pointWin = Helper.rng(Math.round(duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
			event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
			Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
			assert bl != null;
			bl.addLoses();
			MySQL.sendBeybladeToDB(bl);

			Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
			assert bb != null;
			bb.addWins();
			bb.addPoints(pointWin == 0 ? 1 : pointWin);
			MySQL.sendBeybladeToDB(bb);
			JDAEvents.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
		} else if (duel.getB1().getLife() <= 0) {
			int pointWin = Helper.rng(Math.round(duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
			event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
			Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
			assert bl != null;
			bl.addLoses();
			MySQL.sendBeybladeToDB(bl);

			Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
			assert bb != null;
			bb.addWins();
			bb.addPoints(pointWin == 0 ? 1 : pointWin);
			MySQL.sendBeybladeToDB(bb);
			JDAEvents.dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar") || event.getMessage().getContentRaw().equalsIgnoreCase("especial") || event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
			if (event.getMessage().getAuthor() == duel.getP1() && !player1Turn) {
				event.getMessage().getChannel().sendMessage("Ainda não é seu turno " + event.getMessage().getAuthor().getAsMention() + ", por favor aguarde seu oponente agir.").queue();
				return;
			} else if (event.getMessage().getAuthor() == duel.getP2() && player1Turn) {
				event.getMessage().getChannel().sendMessage("Ainda não é seu turno " + event.getMessage().getAuthor().getAsMention() + ", por favor aguarde seu oponente agir.").queue();
				return;
			}
			eb.setTitle("Dados do duelo:");
			eb.setColor(Color.decode(!player1Turn ? duel.getB1().getColor() : duel.getB2().getColor()));
			eb.setDescription("**" + duel.getB1().getName() + "** :vs: **" + duel.getB2().getName() + "**");
			eb.addField(duel.getB1().getName(), "Vida: " + duel.getB1().getLife() + "\n\nForça: " + duel.getB1().getStrength() + "\nVelocidade: " + duel.getB1().getSpeed() + "\nEstabilidade: " + duel.getB1().getStability() + "\nTipo: " + (duel.getB1().getS() == null ? "Não possui" : duel.getB1().getS().getType()), true);
			eb.addField(duel.getB2().getName(), "Vida: " + duel.getB2().getLife() + "\n\nForça: " + duel.getB2().getStrength() + "\nVelocidade: " + duel.getB2().getSpeed() + "\nEstabilidade: " + duel.getB2().getStability() + "\nTipo: " + (duel.getB2().getS() == null ? "Não possui" : duel.getB2().getS().getType()), true);
			event.getMessage().getChannel().sendMessage(eb.build()).queue();
			JDAEvents.dd.stream().filter(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor()).findFirst().ifPresent(m -> {
				m.getB1().setLife(duel.getB1().getLife());
				m.getB2().setLife(duel.getB2().getLife());
			});
		}
	}

	public static List<String> getGamble() {
		String[] icon = {":cheese:", ":izakaya_lantern:", ":moneybag:", ":diamond_shape_with_a_dot_inside:", ":rosette:"};
		List<String> result = new ArrayList<>();
		result.add(icon[rng(icon.length - 1)]);
		result.add(icon[rng(icon.length - 1)]);
		result.add(icon[rng(icon.length - 1)]);
		result.add(icon[rng(icon.length - 1)]);
		result.add(icon[rng(icon.length - 1)]);
		return result;
	}

	public static void spawnAd(MessageChannel channel) {
		if (Helper.rng(1000) > 990) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://discordbots.org/bot/572413282653306901").queue();
		}
	}

	public static void log(Class source, LogLevel level, String msg) {
		final Logger logger = LogManager.getLogger(source.getName());

		switch (level) {
			case DEBUG:
				logger.debug(msg.trim());
				break;
			case INFO:
				logger.info(msg.trim());
				break;
			case WARN:
				logger.warn(msg.trim());
				break;
			case ERROR:
				logger.error(msg.trim());
				break;
			case FATAL:
				logger.fatal(msg.trim());
				break;
		}
	}

	public static InputStream getImage(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0");
		return con.getInputStream();
	}

	public static Webhook getOrCreateWebhook(TextChannel chn) {
		try {
			final Webhook[] webhook = {null};
			chn.getWebhooks().queue(whs -> whs.stream().filter(w -> Objects.requireNonNull(w.getOwner()).getUser() == Main.getJibril().getSelfUser()).findFirst().ifPresent(webhook1 -> webhook[0] = webhook1));
			if (webhook[0] == null) return chn.createWebhook("Jibril").complete();
			else return webhook[0];
		} catch (InsufficientPermissionException e) {
			sendPM(chn.getGuild().getOwner().getUser(), ":x: | A Jibril não possui permissão para criar um webhook em seu servidor");
		}
		return null;
	}

	public static Color reverseColor(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
		hsv[2] = (hsv[2] + 180) % 360;

		return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
	}

	public static String makeEmoteFromMention(String[] source) {
		String[] chkdSrc = new String[source.length];
		for (int i = 0; i < source.length; i++) {
			if (source[i].startsWith("{") && source[i].endsWith("}")) chkdSrc[i] = source[i].replace("{", "<").replace("}", ">").replace("&", ":");
			else chkdSrc[i] = source[i];
		}
		return String.join(" ", chkdSrc).trim();
	}

	public static void logToChannel(Member m, boolean isCommand, Command c, String msg, Guild g) {
		guildConfig gc = SQLite.getGuildById(g.getId());
		try {
			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(msg);
			eb.addField("Referente:", m.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", c.getName(), true);
			eb.setFooter("Data: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), null);

			g.getTextChannelById(gc.getLogChannel()).sendMessage(eb.build()).queue();
		} catch (Exception e) {
			gc.setLogChannel("");
		}
	}
}
