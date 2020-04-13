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

package com.kuuhaku.handlers.games.rpg;

import com.kuuhaku.handlers.games.rpg.entities.Character;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.entities.LootItem;
import com.kuuhaku.handlers.games.rpg.enums.Rarity;
import com.kuuhaku.handlers.games.rpg.exceptions.BadLuckException;
import com.kuuhaku.handlers.games.rpg.world.World;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
	public static Integer[] coordToArray(char x, char y) throws IllegalArgumentException {
		final String alpha = "abcdefghijklmnopqrstuvwxyz";

		if (!ArrayUtils.contains(alpha.toUpperCase().toCharArray(), x) || !ArrayUtils.contains(alpha.toCharArray(), y)) {
			throw new IllegalArgumentException("Invalid coordinate");
		}

		return new Integer[]{ArrayUtils.indexOf(alpha.toUpperCase().toCharArray(), x), ArrayUtils.indexOf(alpha.toCharArray(), y)};
	}

	public static char[] arrayToCoord(Integer[] coord) throws ArrayIndexOutOfBoundsException {
		final String alpha = "abcdefghijklmnopqrstuvwxyz";

		return new char[]{alpha.toUpperCase().charAt(coord[0]), alpha.charAt(coord[1])};
	}

	public static int[] getAttribsFromMap(Map<String, String> map) {
		return map.values().stream().mapToInt(Integer::parseInt).toArray();
	}

	public static Integer[] toPinPos(Integer[] sourcePos) {
		return new Integer[]{sourcePos[0] * 64, sourcePos[1] * 64};
	}

	public static Integer[] toDotPos(Integer[] sourcePos) {
		return new Integer[]{sourcePos[0] * 64, sourcePos[1] * 64};
	}

	public static BufferedImage makeIcon(String path, int w, int h) throws IOException {
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.WHITE);

		g2d.drawOval(5, 5, w - 10, h - 10);
		g2d.drawImage(ImageIO.read(Objects.requireNonNull(Utils.class.getClassLoader().getResourceAsStream(path))).getScaledInstance(w - w / 3 + (w % 3 == 0 ? 0 : 1), h - h / 3 + (h % 3 == 0 ? 0 : 1), 0), w / 6, h / 6, null);

		g2d.dispose();

		return bi;
	}

	public static void makeItem(String content, BufferedImage source, String path, int w, int h, int x, int y, int length) throws IOException {
		Graphics2D g2d = getGraphics(source);

		g2d.drawRoundRect(x + 10, y + 10, Math.max(length, w + 20), h + 20, 64, 64);
		g2d.drawImage(makeIcon(path, w, h), x + 20, y + 20, null);
		g2d.drawString(content, x + 30 + w, y + h);

		g2d.dispose();
	}

	private static Graphics2D getGraphics(BufferedImage source) {
		Graphics2D g2d = source.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.PLAIN, 40));
		return g2d;
	}

	public static void makeInfoBox(Character c, BufferedImage source, int w, int h, int x, int y) {
		Graphics2D g2d = getGraphics(source);

		int[] stats = c.getInventory().getStatModifiers();
		g2d.drawRoundRect(x + 10, y + 10, w, h, 64, 64);
		g2d.drawString("Modificadores", x + 10 + (w / 2 - (g2d.getFontMetrics().stringWidth("Modificadores") / 2)), y + 70);
		g2d.drawString("ATK: " + (stats[1] > 0 ? "+" + stats[1] : (stats[1] < 0 ? "-" + stats[1] : stats[1])), x + 30, y + 140);
		g2d.drawString("DEF: " + (stats[0] > 0 ? "+" + stats[0] : (stats[0] < 0 ? "-" + stats[0] : stats[0])), x + 30, y + 200);

		g2d.drawString("PER: " + (stats[2] > 0 ? "+" + stats[2] : (stats[2] < 0 ? "-" + stats[2] : stats[2])), x + 30 + (w / 2), y + 140);
		g2d.drawString("RES: " + (stats[3] > 0 ? "+" + stats[3] : (stats[3] < 0 ? "-" + stats[3] : stats[3])), x + 30 + (w / 2), y + 200);
		g2d.drawString("CAR: " + (stats[4] > 0 ? "+" + stats[4] : (stats[4] < 0 ? "-" + stats[4] : stats[4])), x + 30 + (w / 2), y + 260);
		g2d.drawString("INT: " + (stats[5] > 0 ? "+" + stats[5] : (stats[5] < 0 ? "-" + stats[5] : stats[5])), x + 30 + (w / 2), y + 320);
		g2d.drawString("LCK: " + (stats[6] > 0 ? "+" + stats[6] : (stats[6] < 0 ? "-" + stats[6] : stats[6])), x + 30 + (w / 2), y + 380);

		g2d.dispose();
	}

	public static String rollDice(String arg, Character chr) throws NumberFormatException {
		String[] rawDices = arg.split(" ");
		List<String> dices = new ArrayList<>();
		List<String> steps = new ArrayList<>();
		List<String> descriptors = new ArrayList<>();

		for (String rawDice : rawDices) {
			if (rawDice.contains("d")) {
				int amount = Integer.parseInt(rawDice.split("d")[0]);
				for (int x = 0; x < amount; x++) {
					String dice;
					switch (rawDice.split("d")[1]) {
						case "s":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getStrength() + chr.getInventory().getStatModifiers()[1]);
							else dice = "1";
							break;
						case "p":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getPerception() + chr.getInventory().getStatModifiers()[2]);
							else dice = "1";
							break;
						case "e":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getEndurance() + chr.getInventory().getStatModifiers()[3]);
							else dice = "1";
							break;
						case "c":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getCharisma() + chr.getInventory().getStatModifiers()[4]);
							else dice = "1";
							break;
						case "i":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getIntelligence() + chr.getInventory().getStatModifiers()[5]);
							else dice = "1";
							break;
						case "a":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getAgility() + chr.getInventory().getStatModifiers()[6]);
							else dice = "1";
							break;
						case "l":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getLuck() + chr.getInventory().getStatModifiers()[7]);
							else dice = "1";
							break;
						case "r":
							if (chr.getStatus() != null)
								dice = String.valueOf(chr.getStatus().getDefense() + chr.getInventory().getStatModifiers()[0]);
							else dice = "1";
							break;
						default:
							dice = rawDice.split("d")[1];
					}
					dices.add(dice);
					descriptors.add(rawDice);
				}
			}
		}

		int[] results = new int[dices.size()];

		for (int i = 0; i < dices.size(); i++) {
			StringBuilder numBuffer = new StringBuilder();
			StringBuilder opBuffer = new StringBuilder();
			boolean inOP = false;
			for (char c : dices.get(i).toCharArray()) {
				if (StringUtils.isNumeric(String.valueOf(c)) && !inOP) {
					numBuffer.append(c);
				} else {
					inOP = true;
					opBuffer.append(c);
				}
			}
			int rolled = (int) Math.round(Math.random() * Integer.parseInt(numBuffer.toString()));
			results[i] = (int) eval(rolled + opBuffer.toString());
			steps.add(rolled + opBuffer.toString());
		}

		for (int i = 0; i < steps.size(); i++) {
			steps.set(i, (i + 1) + "º (" + descriptors.get(i) + ") -> " + steps.get(i));
		}
		return Arrays.stream(results).sum() + "```" + steps.stream().map(s -> s + "\n").collect(Collectors.joining()) + "```";
	}

	private static double eval(final String str) {
		return new Object() {
			int pos = -1, ch;

			void nextChar() {
				ch = (++pos < str.length()) ? str.charAt(pos) : -1;
			}

			boolean eat(int charToEat) {
				while (ch == ' ') nextChar();
				if (ch == charToEat) {
					nextChar();
					return true;
				}
				return false;
			}

			double parse() {
				nextChar();
				double x = parseExpression();
				if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
				return x;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor
			// factor = `+` factor | `-` factor | `(` expression `)`
			//        | number | functionName factor | factor `^` factor

			double parseExpression() {
				double x = parseTerm();
				for (; ; ) {
					if (eat('+')) x += parseTerm(); // addition
					else if (eat('-')) x -= parseTerm(); // subtraction
					else return x;
				}
			}

			double parseTerm() {
				double x = parseFactor();
				for (; ; ) {
					if (eat('*')) x *= parseFactor(); // multiplication
					else if (eat('/')) x /= parseFactor(); // division
					else return x;
				}
			}

			double parseFactor() {
				if (eat('+')) return parseFactor(); // unary plus
				if (eat('-')) return -parseFactor(); // unary minus

				double x;
				int startPos = this.pos;
				if (eat('(')) { // parentheses
					x = parseExpression();
					eat(')');
				} else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
					while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
					x = Double.parseDouble(str.substring(startPos, this.pos));
				} else if (ch >= 'a' && ch <= 'z') { // functions
					while (ch >= 'a' && ch <= 'z') nextChar();
					String func = str.substring(startPos, this.pos);
					x = parseFactor();
					switch (func) {
						case "sqrt":
							x = Math.sqrt(x);
							break;
						case "sin":
							x = Math.sin(Math.toRadians(x));
							break;
						case "cos":
							x = Math.cos(Math.toRadians(x));
							break;
						case "tan":
							x = Math.tan(Math.toRadians(x));
							break;
						default:
							throw new RuntimeException("Unknown function: " + func);
					}
				} else {
					throw new RuntimeException("Unexpected: " + (char) ch);
				}

				if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

				return x;
			}
		}.parse();
	}

	public static Item getItem(int luck, List<LootItem> lootTable) {
		List<Item> filteredList = lootTable.stream().filter(i -> i.getRarity().equals(Rarity.roll(luck))).map(LootItem::getItem).collect(Collectors.toList());
		if (filteredList.size() == 0) throw new BadLuckException();
		return filteredList.get((int) Math.round(Math.random() * filteredList.size() - 1));
	}

	public static void checkRarity(String op, String rarity, List<LootItem> loot, World world, TextChannel channel) {
		if (op.contains("+")) {
			op = op.replace("+", "").trim();
			try {
				loot.add(new LootItem(world.getItem(op), Rarity.byName(rarity)));
			} catch (IllegalArgumentException e) {
				channel.sendMessage(":x: | Raridade inválida. Os tipos de raridade são:\n" +
						"\n`Comum`" +
						"\n`Incomum`" +
						"\n`Raro`" +
						"\n`Épico`" +
						"\n`Lendário`").queue();
			}
		} else if (op.contains("-")) {
			op = op.replace("+", "").trim();
			String finalOp = op;
			loot.removeIf(i -> i.getItem().getName().equalsIgnoreCase(finalOp));
		}
	}

	public static boolean noPlayerAlert(String[] args, Message message, MessageChannel channel) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(":x: | Você precisa especificar o jogador").queue();
			return true;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | O segundo argumento precisa ser o nome do item ou ouro").queue();
			return true;
		} else if (args.length < 3 && (args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			channel.sendMessage(":x: | Você precisa especificar a quantidade de ouro").queue();
			return true;
		}
		return false;
	}

	public static String encodeToBase64(BufferedImage bi) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(bi, "png", baos);
			byte[] b = baos.toByteArray();

			return Base64.getEncoder().encodeToString(b);
		} catch (IOException e) {
			Helper.logger(Utils.class).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public static BufferedImage decodeBase64(String b64) {
		byte[] b = Base64.getDecoder().decode(b64);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(b)) {
			return ImageIO.read(bais);
		} catch (Exception e) {
			Helper.logger(Utils.class).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
