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
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.DuelData;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

	public static File createOhNoImage(String text) throws IOException {
		BufferedImage image = ImageIO.read(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("ohno.png")));

		BufferedImage resultImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D w = (Graphics2D) resultImg.getGraphics();
		w.drawImage(image, 0, 0, null);
		AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
		w.setComposite(alphaChannel);
		w.setColor(Color.BLACK);
		w.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));

		FontMetrics fontMetrics = w.getFontMetrics();
		Rectangle2D rect = fontMetrics.getStringBounds(text, w);
        
        /*int centerX = (image.getWidth() - (int) rect.getWidth()) / 2;
        int centerY = image.getHeight() / 2;*/

		//21 - 123456789012345678901
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (String str : text.split(" ")) {
			if (count + str.length() > 21) {
				sb.append("\n");
				count = 0;
				if (str.length() > 21) {
					String newStr = str;
					do {
						sb.append(newStr, 0, 20).append("\n");
						newStr = newStr.substring(21);
					} while (newStr.length() > 21);
					count = 0;
					continue;
				}
			}
			sb.append(str).append(" ");
			count += str.length() + 1;
		}
		text = sb.toString().trim();
		int lineN = 1;
		for (String line : text.split("\n")) {
			if (lineN > 4) break;
			w.drawString(line, 344 + 3, (int) (22 + (rect.getHeight() * lineN)));
			lineN++;
		}

		File result = new File(System.getProperty("user.dir") + "\\ohno-" + Instant.now().toEpochMilli() + ".png");
		ImageIO.write(resultImg, "png", result);
		w.dispose();

		return result;
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

	public static void sendReaction(MessageChannel channel, String message, InputStream is, boolean reacted) {
		if (reacted)
			channel.sendMessage(message).addFile(is, "reaction.gif").queue(m -> m.addReaction("\u21aa").queue());
		else channel.sendMessage(message).addFile(is, "reaction.gif").queue();
	}

	public static void cls() {
		try {
			final String os = System.getProperty("os.name");

			if (os.contains("Windows")) {
				Runtime.getRuntime().exec("cls");
			} else {
				Runtime.getRuntime().exec("clear");
			}
		} catch (final Exception e) {
			System.out.println("Erro ao limpar o console.");
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

	public static int rng(int maxValue) {
		return Math.abs(new Random().nextInt(maxValue));
	}

	public static Color colorThief(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage icon = ImageIO.read(con.getInputStream());

		return new Color(ColorThief.getColor(icon)[0], ColorThief.getColor(icon)[1], ColorThief.getColor(icon)[2]);
	}

	public static void battle(MessageReceivedEvent event) {
		if (JDAEvents.dd.stream().noneMatch(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor())) {
			return;
		}
		@SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = JDAEvents.dd.stream().filter(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor()).findFirst().get();
		boolean player1Turn = duel.isP1turn();
		EmbedBuilder eb = new EmbedBuilder();

		if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				duel.setP1turn(false);
				duel.setD1(false);
				if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
				int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
				duel.getB2().setLife(duel.getB2().getLife() - damage);
				System.out.println(damage + " -> " + duel.getB2().getLife());
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue());
			} else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
				duel.setP1turn(true);
				duel.setD2(false);
				if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
				int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
				duel.getB1().setLife(duel.getB1().getLife() - damage);
				System.out.println(damage + " -> " + duel.getB1().getLife());
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue());
			}
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
			if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
				duel.setP1turn(false);
				duel.setD1(true);
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB2().getName()).queue());
			} else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
				duel.setP1turn(true);
				duel.setD2(true);
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB1().getName()).queue());
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
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB1().getName() + " desapareceu? Ah, lá está ela, com um movimento digno dos tigres ela executa o golpe especial " + duel.getB1().getS().getName() + "! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 12:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getSpeed() * 2 / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB1().getName() + " executou com perfeição em " + duel.getB1().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
									duel.setP1turn(true);
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue());
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 21:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB2().getStability() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB1().getName() + " lançou " + duel.getB1().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
									duel.setS1(true);
									if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 22:
								if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
									duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getStrength() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 150));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB1().getName() + " provou que é possivel!!! " + duel.getB1().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
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
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB1().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
									duel.setS1(true);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
						}
					} else {
						event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB1().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue());
					}
				} else {
					event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ainda não possui um alinhamento!").queue());
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
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB2().getName() + " desapareceu? Ah, lá está ele, com um movimento digno dos tigres ele executa o golpe especial " + duel.getB2().getS().getName() + "! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 12:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getSpeed() * 2 / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB2().getName() + " executou com perfeição em " + duel.getB2().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
									duel.setP1turn(false);
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue());
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 21:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB1().getStability() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB2().getName() + " lançou " + duel.getB2().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
									duel.setS2(true);
									if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
							case 22:
								if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
									duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getStrength() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 150));
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB2().getName() + " provou que é possivel!!! " + duel.getB2().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
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
									event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB2().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
									duel.setS2(true);
								} else {
									event.getMessage().getChannel().sendTyping().queue(Miss);
								}
								break;
						}
					} else {
						event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB2().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue());
					}
				} else {
					event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ainda não possui um alinhamento!").queue());
				}
			}
		} else if (event.getMessage().getContentRaw().equalsIgnoreCase("desistir")) {
			if (event.getMessage().getAuthor() == duel.getP1()) {
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " desistiu. A vitória é de " + duel.getP2().getAsMention() + "!").queue());
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
				event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue());
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
			event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue());
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
			event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue());
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
			eb.setTitle("Dados do duelo:");
			eb.setDescription(duel.getB1().getName() + " :vs: " + duel.getB2().getName());
			eb.addField(duel.getB1().getName(), "Vida: " + duel.getB1().getLife() + "\n\nForça: " + duel.getB1().getStrength() + "\nVelocidade: " + duel.getB1().getSpeed() + "\nEstabilidade: " + duel.getB1().getStability()+ "\nEspecial: " + (duel.getB1().getS() == null ? "Não possui" : duel.getB1().getS().getName()), true);
			eb.addField(duel.getB2().getName(), "Vida: " + duel.getB2().getLife() + "\n\nForça: " + duel.getB2().getStrength() + "\nVelocidade: " + duel.getB2().getSpeed() + "\nEstabilidade: " + duel.getB2().getStability()+ "\nEspecial: " + (duel.getB2().getS() == null ? "Não possui" : duel.getB2().getS().getName()), true);
			event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(eb.build()).queue());
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
		if (Helper.rng(1000) > 950) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://discordbots.org/bot/572413282653306901").queue();
		}
	}
}
