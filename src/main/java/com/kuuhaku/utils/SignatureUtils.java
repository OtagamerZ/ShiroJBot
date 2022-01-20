package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Signature;
import org.intellij.lang.annotations.Language;

import javax.annotation.RegEx;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class SignatureUtils {
	@Language("RegExp")
	private static final String ARGUMENT_PATTERN = "^<(?<name>[A-z]+):(?<type>[A-Z]+)(?<required>:R)?>$";

	public static Map<String, String> parse(Class<? extends Executable> klass, String input) {
		Map<String, String> out = new LinkedHashMap<>();
		String[] signatures = Main.getCommandManager().getCommandSignature(klass);

		for (String sig : signatures) {
			String str = input;
			String[] args = sig.split("[ ]+");

			for (String arg : args) {
				Map<String, String> groups = Helper.extractNamedGroups(arg, ARGUMENT_PATTERN);
				String name = groups.get("name");
				boolean required = groups.containsKey("required");

				try {
					Signature.Type type = Signature.Type.valueOf(groups.get("type"));

					if (type == Signature.Type.TEXT) {
						out.put(name, str);
						str = "";
					} else {
						String s = str.split("[ ]+")[0].trim();
						str = str.replaceFirst(Pattern.quote(s), "").trim();

						if (type.validate(s)) {
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

			if (!out.isEmpty()) break;
		}

		return out;
	}
}
