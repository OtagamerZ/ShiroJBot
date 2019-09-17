package com.kuuhaku.handlers.games.RPG;

import com.kuuhaku.handlers.games.RPG.Entities.Character;
import com.kuuhaku.handlers.games.RPG.Entities.Status;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

	public static void makeItem(Character c, String content, BufferedImage source, String path, int w, int h, int x, int y, int length) throws IOException, FontFormatException {
		Graphics2D g2d = source.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.PLAIN, 40));

		g2d.drawRoundRect(x + 10, y + 10, length < w + 20 ? w + 20 : length, h + 20, 64, 64);
		g2d.drawImage(makeIcon(path, w, h), x + 20, y + 20, null);
		g2d.drawString(content, x + 30 + w, y + h);

		g2d.dispose();
	}

	public static void makeInfoBox(Character c, BufferedImage source, int w, int h, int x, int y) throws IOException, FontFormatException {
		Graphics2D g2d = source.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.PLAIN, 40));

		int[] stats = c.getInventory().getStatModifiers();
		g2d.drawRoundRect(x + 10, y + 10, w, h, 64, 64);
		g2d.drawString("Modificadores", x + 10 + (int) (w / 2 - (g2d.getFontMetrics().getStringBounds("Modificadores", g2d).getWidth() / 2)), y + 70);
		g2d.drawString("ATK: " + (stats[1] > 0 ? "+" + stats[1] : (stats[1] < 0 ? "-" + stats[1] : stats[1])), x + 30, y + 140);
		g2d.drawString("DEF: " + (stats[0] > 0 ? "+" + stats[0] : (stats[0] < 0 ? "-" + stats[0] : stats[0])), x + 30, y + 200);

		g2d.drawString("PER: " + (stats[2] > 0 ? "+" + stats[2] : (stats[2] < 0 ? "-" + stats[2] : stats[2])), x + 30 + (w / 2), y + 140);
		g2d.drawString("RES: " + (stats[3] > 0 ? "+" + stats[3] : (stats[3] < 0 ? "-" + stats[3] : stats[3])), x + 30 + (w / 2), y + 200);
		g2d.drawString("CAR: " + (stats[4] > 0 ? "+" + stats[4] : (stats[4] < 0 ? "-" + stats[4] : stats[4])), x + 30 + (w / 2), y + 260);
		g2d.drawString("INT: " + (stats[5] > 0 ? "+" + stats[5] : (stats[5] < 0 ? "-" + stats[5] : stats[5])), x + 30 + (w / 2), y + 320);
		g2d.drawString("LCK: " + (stats[6] > 0 ? "+" + stats[6] : (stats[6] < 0 ? "-" + stats[6] : stats[6])), x + 30 + (w / 2), y + 380);

		g2d.dispose();
	}

	public static String rollDice(String arg, Status status) {
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
								if (status != null) dice = String.valueOf(status.getStrength());
								else dice = "1";
								break;
							case "p":
								if (status != null) dice = String.valueOf(status.getPerception());
								else dice = "1";
								break;
							case "e":
								if (status != null) dice = String.valueOf(status.getEndurance());
								else dice = "1";
								break;
							case "c":
								if (status != null) dice = String.valueOf(status.getCharisma());
								else dice = "1";
								break;
							case "i":
								if (status != null) dice = String.valueOf(status.getIntelligence());
								else dice = "1";
								break;
							case "a":
								if (status != null) dice = String.valueOf(status.getAgility());
								else dice = "1";
								break;
							case "l":
								if (status != null) dice = String.valueOf(status.getLuck());
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
					if (func.equals("sqrt")) x = Math.sqrt(x);
					else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
					else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
					else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
					else throw new RuntimeException("Unknown function: " + func);
				} else {
					throw new RuntimeException("Unexpected: " + (char) ch);
				}

				if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

				return x;
			}
		}.parse();
	}
}
