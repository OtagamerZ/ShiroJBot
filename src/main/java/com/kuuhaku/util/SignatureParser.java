package com.kuuhaku.util;

import com.kuuhaku.exceptions.InvalidSignatureException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.FailedSignature;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class SignatureParser {
	private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^<(?<name>[A-Za-z]\\w*):(?<type>[A-Za-z]+)(?<required>:[Rr])?>(?:\\[(?<options>[\\w\\-;,]+)+])?$");

	public static JSONObject parse(I18N locale, Executable exec, String input) throws InvalidSignatureException {
		JSONObject out = new JSONObject();
		List<FailedSignature> failed = new ArrayList<>();
		Signature annot = exec.getClass().getDeclaredAnnotation(Signature.class);
		if (annot == null) return out;

		String[] signatures = annot.value();

		boolean fail;
		List<String> supplied = new ArrayList<>();
		for (String sig : signatures) {
			fail = false;
			String str = input;
			String[] args = sig.split(" +");
			String[] failOpts = new String[0];

			int i = 0;
			int matches = 0;
			for (String arg : args) {
				i++;
				JSONObject groups = Utils.extractNamedGroups(arg, ARGUMENT_PATTERN);
				String name = groups.getString("name");
				boolean required = groups.containsKey("required");
				String wrap = required ? "[%s]" : "%s";

				Signature.Type type = Signature.Type.valueOf(groups.getString("type").toUpperCase(Locale.ROOT));

				if (type == Signature.Type.TEXT) {
					if (str.isBlank() && required) {
						fail = true;
						supplied.add(wrap.formatted(Utils.underline(locale.get("signature/" + name))));
						continue;
					}

					if (i == args.length) {
						if (out.has(name)) {
							JSONArray arr;
							if (out.get(name) instanceof List<?> ls) {
								arr = new JSONArray(ls);
							} else {
								arr = new JSONArray();
								Object curr = out.get(name);
								arr.add(curr);
							}
							arr.add(str.replaceFirst("\"(.*)\"", "$1"));

							out.put(name, arr);
						} else {
							out.put(name, str.replaceFirst("\"(.*)\"", "$1"));
						}

						str = "";
					} else {
						if (out.has(name)) {
							JSONArray arr;
							if (out.get(name) instanceof List<?> ls) {
								arr = new JSONArray(ls);
							} else {
								arr = new JSONArray();
								Object curr = out.get(name);
								arr.add(curr);
							}
							arr.add(Utils.extract(str, type.getPattern(), "text"));

							out.put(name, arr);
						} else {
							out.put(name, Utils.extract(str, type.getPattern(), "text"));
						}

						str = str.replaceFirst(type.getRegex(), "").trim();
					}

					matches++;
				} else {
					List<String> opts = Arrays.stream(groups.getString("options", "").split(","))
							.filter(s -> !s.isBlank())
							.map(String::toLowerCase)
							.toList();

					String s = str.split("\\s+")[0].trim();
					str = str.replaceFirst(Pattern.quote(s), "").trim();
					s = StringUtils.stripAccents(s);

					if (type.validate(s) && (opts.isEmpty() || opts.contains(s.toLowerCase(Locale.ROOT)))) {
						switch (type) {
							case CHANNEL -> s = s.replaceAll("[<#>]", "");
							case USER, ROLE -> s = s.replaceAll("[<@!>]", "");
						}

						if (!fail) {
							if (out.has(name)) {
								JSONArray arr;
								if (out.get(name) instanceof List<?> ls) {
									arr = new JSONArray(ls);
								} else {
									arr = new JSONArray();
									Object curr = out.get(name);
									arr.add(curr);
								}
								arr.add(s);

								out.put(name, arr);
							} else {
								out.put(name, s);
							}
						}
						supplied.add(s);
						matches++;
					} else if (required) {
						fail = true;
						if (opts.isEmpty()) {
							supplied.add(wrap.formatted(Utils.underline(locale.get("signature/" + name))));
						} else {
							supplied.add(wrap.formatted(opts.stream().map(Utils::underline).collect(Collectors.joining("|"))));
							failOpts = opts.stream().map(o -> "`" + o + "`").toArray(String[]::new);
						}
					}
				}
			}

			if (fail) {
				out.clear();
				failed.add(new FailedSignature(String.join(" ", supplied), failOpts, matches, args.length));
				supplied.clear();
			} else return out;
		}

		if (annot.allowEmpty()) return new JSONObject();
		else {
			int argLength = input.split(" +").length;
			FailedSignature first = failed.stream().max(
					Comparator.comparingInt(FailedSignature::matches)
							.thenComparing(fs -> fs.numArgs() - argLength, Comparator.reverseOrder())
			).orElseThrow();
			throw new InvalidSignatureException(first.line(), first.options());
		}
	}

	public static List<String> extract(I18N locale, Executable exec) {
		List<String> out = new ArrayList<>();
		Signature annot = exec.getClass().getDeclaredAnnotation(Signature.class);
		if (annot == null) return out;

		String[] signatures = annot.value();

		List<String> supplied = new ArrayList<>();
		for (String sig : signatures) {
			String[] args = sig.split(" +");

			supplied.add("%1$s%2$s");
			for (String arg : args) {
				JSONObject groups = Utils.extractNamedGroups(arg, ARGUMENT_PATTERN);
				String name = groups.getString("name");
				boolean required = groups.containsKey("required");
				String wrap = required ? "[%s]" : "%s";

				supplied.add(wrap.formatted(locale.get("signature/" + name)));
			}

			out.add(String.join(" ", supplied));
			supplied.clear();
		}

		return out;
	}
}
