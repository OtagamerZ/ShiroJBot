package com.kuuhaku.utils;

import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Signature;
import com.kuuhaku.utils.helpers.StringHelper;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.regex.Pattern;

public abstract class SignatureUtils {
	@Language("RegExp")
	private static final String ARGUMENT_PATTERN = "^<(?<name>[A-z]+):(?<type>[A-Z]+):(?<required>R)?>(?:\\[(?<options>.*)])?$";

	public static Map<String, String> parse(Class<? extends Executable> klass, String input) {
		Map<String, String> out = new LinkedHashMap<>();
		Signature annot = klass.getDeclaredAnnotation(Signature.class);
		if (annot == null) return out;

		String[] signatures = annot.value();

		for (String sig : signatures) {
			String str = input;
			String[] args = sig.split(" +");

			for (String arg : args) {
				Map<String, String> groups = StringHelper.extractNamedGroups(arg, ARGUMENT_PATTERN);
				String name = groups.get("name");
				boolean required = groups.containsKey("required");

				try {
					Signature.Type type = Signature.Type.valueOf(groups.get("type"));

					if (type == Signature.Type.TEXT) {
						out.put(name, str);
						str = "";
					} else {
						List<String> opts = Arrays.stream(groups.getOrDefault("options", "").split(","))
								.filter(s -> !s.isBlank())
								.map(String::toLowerCase)
								.toList();

						String s = str.split(" +")[0].trim();
						str = str.replaceFirst(Pattern.quote(s), "").trim();

						if (type.validate(s) && (opts.isEmpty() || opts.contains(s.toLowerCase(Locale.ROOT)))) {
							out.put(name, str);
						} else if (required) {
							out.clear();
							break;
						}
					}
				} catch (IllegalArgumentException e) {
					if (required) {
						out.clear();
						break;
					}
				}
			}

			if (!out.isEmpty()) return out;
		}

		return out;
	}
}
