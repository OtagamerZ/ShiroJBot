package com.kuuhaku.utils.helpers;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Emote;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.intellij.lang.annotations.Language;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StringHelper {
	public static String hash(byte[] bytes, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
			return "";
		}
	}

	public static String hash(String value, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
			return "";
		}
	}

	public static JSONObject findJson(String text) {
		String json = extract(text, "\\{.*}");

		if (json == null) return null;
		else return new JSONObject(json);
	}

	public static String noCopyPaste(String input) {
		return String.join(Constants.ANTICOPY, input.split(""));
	}

	public static Matcher regex(String text, @Language("RegExp") String regex) {
		return Pattern.compile(regex).matcher(text);
	}

	public static String extract(String text, @Language("RegExp") String regex) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group();
		else return null;
	}

	public static String extract(String text, @Language("RegExp") String regex, int group) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static List<String> extractGroups(String text, @Language("RegExp") String regex) {
		List<String> out = new ArrayList<>();
		Matcher m = Pattern.compile(regex).matcher(text);

		while (m.find()) {
			for (int i = 0; i < m.groupCount(); i++) {
				out.add(m.group(i + 1));
			}
		}

		return out.stream()
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static Map<String, String> extractNamedGroups(String text, @Language("RegExp") String regex) {
		List<String> names = extractGroups(regex, "\\(\\?<([a-zA-Z][A-z0-9]*)>");
		Map<String, String> out = new HashMap<>();
		Matcher m = Pattern.compile(regex).matcher(text);

		while (m.find()) {
			for (String name : names) {
				out.putIfAbsent(name, m.group(name));
			}
		}

		return out.entrySet().stream()
				.filter(e -> e.getValue() != null)
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public static String extract(String text, @Language("RegExp") String regex, String group) {
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find()) return m.group(group);
		else return null;
	}

	public static boolean hasEmote(String text) {
		for (String word : text.split(" ")) {
			if (word.startsWith(":") && word.endsWith(":")) {
				if (Main.getEmoteCache().containsKey(word)) return true;
			}
		}

		return false;
	}

	public static String getFancyNumber(int number) {
		String sNumber = String.valueOf(number);
		StringBuilder sb = new StringBuilder();
		for (char c : sNumber.toCharArray())
			sb.append(getNumericEmoji(Integer.parseInt(String.valueOf(c))));

		return sb.toString();
	}

	public static String bugText(String text) {
		return String.join(Constants.ANTICOPY, text.split(""));
	}

	public static String getRegionalIndicator(int i) {
		return new String(new char[]{"\uD83C\uDDE6".toCharArray()[0], (char) ("\uD83C\uDDE6".toCharArray()[1] + i)});
	}

	public static String getNumericEmoji(int i) {
		return i + "⃣";
	}

	public static String separate(Object value) {
		try {
			Number n = value instanceof Number nb ? nb : NumberUtils.createNumber(String.valueOf(value));
			DecimalFormat df = new DecimalFormat();
			df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
			df.setGroupingSize(3);

			return df.format(n);
		} catch (NumberFormatException e) {
			return String.valueOf(value);
		}
	}

	public static String getShortenedValue(long value, int forEach) {
		if (value == 0) return String.valueOf(value);
		int times = (int) Math.floor(MathHelper.log(value, forEach));
		String reduced = MathHelper.roundToString(value / Math.pow(forEach, times), 2);


		return reduced + StringUtils.repeat("k", times);
	}

	public static boolean isPureMention(String msg) {
		return msg.matches("<(@|@!)\\d+>");
	}

	public static String generateRandomHash(int length) {
		try {
			String method;

			if (length <= 0) return "";
			else if (length <= 32) method = "MD5";
			else if (length <= 40) method = "SHA-1";
			else if (length <= 64) method = "SHA-256";
			else if (length <= 128) method = "SHA-512";
			else return "";

			return Hex.encodeHexString(MessageDigest.getInstance(method).digest(SecureRandom.getSeed(length))).substring(0, length);
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	public static String toStringDuration(long millis) {
		long days = millis / Constants.MILLIS_IN_DAY;
		millis %= Constants.MILLIS_IN_DAY;
		long hours = millis / Constants.MILLIS_IN_HOUR;
		millis %= Constants.MILLIS_IN_HOUR;
		long minutes = millis / Constants.MILLIS_IN_MINUTE;
		millis %= Constants.MILLIS_IN_MINUTE;
		long seconds = millis / Constants.MILLIS_IN_SECOND;
		seconds %= Constants.MILLIS_IN_SECOND;

		return Stream.of(
				days > 0 ? days + " dia" + (days != 1 ? "s" : "") : "",
				hours > 0 ? hours + " hora" + (hours != 1 ? "s" : "") : "",
				minutes > 0 ? minutes + " minuto" + (minutes != 1 ? "s" : "") : "",
				seconds > 0 ? seconds + " segundo" + (seconds != 1 ? "s" : "") : ""
		).filter(s -> !s.isBlank()).collect(Collectors.collectingAndThen(Collectors.toList(), CollectionHelper.properlyJoin()));
	}

	public static long stringToDurationMillis(String str) {
		Map<String, String> args = extractNamedGroups(str, "(?:(?<DAYS>[0-9]+)d)?(?:(?<HOURS>[0-9]+)h)?(?:(?<MINUTES>[0-9]+)m)?(?:(?<SECONDS>[0-9]+)s)?");
		long out = 0;

		for (Map.Entry<String, String> arg : args.entrySet()) {
			TimeUnit unit = TimeUnit.valueOf(arg.getKey());
			out += unit.toMillis(Integer.parseInt(arg.getValue()));
		}

		return out;
	}

	public static long stringToLong(String in) {
		String hash = hash(in, "SHA-1");
		return new BigInteger(hash.getBytes(StandardCharsets.UTF_8)).longValue();
	}

	public static String sign(int value) {
		return value > 0 ? "+" + value : String.valueOf(value);
	}

	public static Emoji parseEmoji(String in) {
		if (StringUtils.isNumeric(in)) {
			Emote e = Main.getShiro().getEmoteById(in);
			if (e == null) return Emoji.fromMarkdown("❓");

			return Emoji.fromEmote(e);
		}

		return Emoji.fromMarkdown(in);
	}

	public static String didYouMean(String word, String[] array) {
		String match = "";
		int threshold = 999;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : array) {
			if (word.equalsIgnoreCase(w)) {
				return word;
			} else {
				int diff = checker.apply(word.toLowerCase(Locale.ROOT), w.toLowerCase(Locale.ROOT));
				if (diff < threshold) {
					match = w;
					threshold = diff;
				}
			}
		}

		return match;
	}

	public static String didYouMean(String word, Collection<String> array) {
		String match = "";
		int threshold = 999;
		LevenshteinDistance checker = new LevenshteinDistance();

		for (String w : array) {
			if (word.equalsIgnoreCase(w)) {
				return word;
			} else {
				int diff = checker.apply(word.toLowerCase(Locale.ROOT), w.toLowerCase(Locale.ROOT));
				if (diff < threshold) {
					match = w;
					threshold = diff;
				}
			}
		}

		return match;
	}
}
