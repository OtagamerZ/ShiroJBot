/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.util;

import com.kuuhaku.exceptions.InvalidSignatureException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.FailedSignature;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public abstract class SignatureParser {
	private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^<(?<name>[a-z]\\w*):(?<type>[a-z]+)(?<required>:[r])?>(?:\\[(?<options>[^\\[\\]]+)+])?$", Pattern.CASE_INSENSITIVE);

	public static JSONObject parse(I18N locale, Executable command, String input) throws InvalidSignatureException {
		Signature annot = command.getClass().getDeclaredAnnotation(Signature.class);
		if (annot == null) return new JSONObject();

		return parse(locale, annot.value(), annot.patterns(), annot.allowEmpty(), input);
	}

	public static JSONObject parse(I18N locale, String[] signatures, SigPattern[] patterns, boolean allowEmpty, String input) throws InvalidSignatureException {
		JSONObject out = new JSONObject();
		List<FailedSignature> failed = new ArrayList<>();

		List<String> supplied = new ArrayList<>();
		for (String sig : signatures) {
			List<String> args = new ArrayList<>(List.of(input.split(" +")));
			String[] params = sig.split(" +");
			String[] failOpts = {};

			int i = 0;
			int matches = 0;
			boolean fail = false;
			for (String param : params) {
				i++;
				JSONObject groups = Utils.extractNamedGroups(param, ARGUMENT_PATTERN);
				if (groups.isEmpty()) {
					String s = args.removeFirst();
					s = StringUtils.stripAccents(s);

					if (s.equalsIgnoreCase(param)) {
						supplied.add(s);
						matches++;
					} else {
						fail = true;
						supplied.add(param);
					}
					continue;
				}

				String name = groups.getString("name");
				boolean required = groups.has("required");
				String wrap = required ? "[%s]" : "%s";

				if (args.isEmpty() && required) {
					fail = true;
					supplied.add(wrap.formatted("> " + locale.get("signature/" + name) + " <"));
					continue;
				}

				Signature.Type type = groups.getEnum(Signature.Type.class, "type");
				if (type == Signature.Type.TEXT) {
					if (!args.isEmpty()) {
						if (out.has(name)) {
							JSONArray arr;
							if (out.get(name) instanceof List<?> ls) {
								arr = new JSONArray(ls);
							} else {
								arr = new JSONArray();
								Object curr = out.get(name);
								arr.add(curr);
							}
							arr.add(String.join(" ", args));

							out.put(name, arr);
						} else {
							out.put(name, String.join(" ", args));
						}

						args.clear();
						matches++;
					} else if (required) {
						fail = true;
						supplied.add(wrap.formatted("> " + locale.get("signature/" + name) + " <"));
					}
				} else {
					String token = null;
					List<String> opts = List.of();

					if (!args.isEmpty()) {
						if (!fail) {
							String arg = args.getFirst();

							if (type == Signature.Type.CUSTOM) {
								@Language("RegExp") String opt = groups.getString("options", "");
								@Language("RegExp") String pattern = opt;
								if (patterns != null) {
									pattern = "^" + Arrays.stream(patterns).parallel()
											.filter(p -> p.id().equals(opt))
											.map(SigPattern::value)
											.findAny().orElse(opt);
								}

								if (Utils.match(arg, pattern)) {
									token = arg;
								}
							} else {
								opts = Arrays.stream(groups.getString("options", "").split(","))
										.filter(s -> !s.isBlank())
										.map(String::toLowerCase)
										.toList();

								if (Utils.match(arg, type.getPattern())) {
									token = arg;
									if (!opts.isEmpty() && !opts.contains(token.toLowerCase())) {
										token = null;
									}
								}
							}
						}

						if (token != null) {
							args.removeFirst();
							token = StringUtils.stripAccents(token);

							if (type.validate(token)) {
								switch (type) {
									case CHANNEL -> token = token.replaceAll("[<#>]", "");
									case USER, ROLE -> token = token.replaceAll("[<@!>]", "");
								}

								if (out.has(name)) {
									JSONArray arr;
									if (out.get(name) instanceof List<?> ls) {
										arr = new JSONArray(ls);
									} else {
										arr = new JSONArray();
										Object curr = out.get(name);
										arr.add(curr);
									}
									arr.add(token);

									out.put(name, arr);
								} else {
									out.put(name, token);
								}

								supplied.add(token);
								matches++;
							}
						}
					}

					if (token == null && required) {
						fail = true;

						if (opts.isEmpty()) {
							supplied.add(wrap.formatted("> " + locale.get("signature/" + name) + " <"));
						} else {
							supplied.add(wrap.formatted(String.join("|", opts)));
							failOpts = opts.stream().map(o -> "`" + o + "`").toArray(String[]::new);
						}
					}
				}
			}

			if (fail) {
				out.clear();
				failed.add(new FailedSignature(String.join(" ", supplied), failOpts, matches, params.length));
				supplied.clear();
			} else {
				return out;
			}
		}

		if (allowEmpty) return new JSONObject();
		else {
			int argLength = input.split(" +").length;
			FailedSignature first = failed.stream().max(
					Comparator.comparingInt(FailedSignature::matches)
							.thenComparing(fs -> fs.numArgs() - argLength, Comparator.reverseOrder())
			).orElseThrow();
			throw new InvalidSignatureException(first.line(), first.options());
		}
	}

	public static List<String> extract(I18N locale, Executable command) {
		Signature annot = command.getClass().getDeclaredAnnotation(Signature.class);
		if (annot == null) return List.of();

		return extract(locale, annot.value(), annot.allowEmpty());
	}

	public static List<String> extract(I18N locale, String[] signatures, boolean allowEmpty) {
		List<String> out = new ArrayList<>();
		if (signatures == null) return List.of("%1$s%2$s");

		if (allowEmpty) {
			out.add("%1$s%2$s");
		} else {
			for (String sig : signatures) {
				if (!Utils.regex(sig, ":r>").find()) {
					out.add("%1$s%2$s");
					break;
				}
			}
		}

		List<String> supplied = new ArrayList<>();
		for (String sig : signatures) {
			String[] args = sig.split(" +");

			supplied.add("%1$s%2$s");
			for (String arg : args) {
				JSONObject groups = Utils.extractNamedGroups(arg, ARGUMENT_PATTERN);
				if (groups.isEmpty()) {
					supplied.add(arg);
					continue;
				}

				String name = groups.getString("name");
				String type = groups.getString("type");
				String opt = groups.getString("options");
				boolean required = groups.has("required");
				String wrap = "%s";

				if (!opt.isBlank()) {
					List<String> opts = Arrays.stream(groups.getString("options", "").split(","))
							.filter(s -> !s.isBlank())
							.map(String::toLowerCase)
							.toList();

					wrap = String.join("|", opts);
				}

				if (type.equalsIgnoreCase(Signature.Type.TEXT.name()) && supplied.size() < args.length - 1) {
					wrap = "\"" + wrap + "\"";
				}

				if (required) {
					wrap = "[" + wrap + "]";
				}

				supplied.add(wrap.formatted(locale.get("signature/" + name)));
			}

			out.add(String.join(" ", supplied));
			supplied.clear();
		}

		return out;
	}
}
