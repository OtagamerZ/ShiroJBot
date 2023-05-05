package com.kuuhaku.util;

import com.kuuhaku.exceptions.InvalidSignatureException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.FailedSignature;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class SignatureParser {
    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^<(?<name>[A-Za-z]\\w*):(?<type>[A-Za-z]+)(?<required>:[Rr])?>(?:\\[(?<options>[^\\[\\]]+)+])?$");

    public static JSONObject parse(I18N locale, Executable command, String input) throws InvalidSignatureException {
        Signature annot = command.getClass().getDeclaredAnnotation(Signature.class);
        if (annot == null) return new JSONObject();

        return parse(locale, annot.value(), annot.patterns(), annot.allowEmpty(), input);
    }

    public static JSONObject parse(I18N locale, String[] signatures, SigPattern[] patterns, boolean allowEmpty, String input) throws InvalidSignatureException {
        JSONObject out = new JSONObject();
        List<FailedSignature> failed = new ArrayList<>();

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
                boolean required = groups.has("required");
                String wrap = required ? "[%s]" : "%s";

                Signature.Type type = groups.getEnum(Signature.Type.class, "type");

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
                            arr.add(str);

                            out.put(name, arr);
                        } else {
                            out.put(name, str);
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
                            arr.add(Utils.extract(str, type.getPattern(), 1));

                            out.put(name, arr);
                        } else {
                            out.put(name, Utils.extract(str, type.getPattern(), 1));
                        }

                        str = str.replaceFirst(type.getRegex(), "").trim();
                    }

                    matches++;
                } else if (type == Signature.Type.CUSTOM) {
                    @Language("RegExp") String opt = groups.getString("options", "");
                    @Language("RegExp") String pattern = opt;
                    if (patterns != null) {
                        pattern = Arrays.stream(patterns)
                                .filter(p -> p.id().equals(opt))
                                .map(SigPattern::value)
                                .findFirst().orElse(opt);
                    }

                    Matcher match = Utils.regex(str, pattern);
                    if (match.find()) {
                        String s = match.group();
                        str = str.replaceFirst(Pattern.quote(s), "").trim();
                        s = StringUtils.stripAccents(s);

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
                    } else {
                        fail = true;
                        supplied.add(wrap.formatted(Utils.underline(locale.get("signature/" + name))));
                    }
                } else {
                    List<String> opts = Arrays.stream(groups.getString("options", "").split(","))
                            .filter(s -> !s.isBlank())
                            .map(String::toLowerCase)
                            .toList();

                    String s = str.split("\\s+")[0].trim();
                    str = str.replaceFirst(Pattern.quote(s), "").trim();
                    s = StringUtils.stripAccents(s);

                    if (type.validate(s) && (opts.isEmpty() || opts.contains(s.toLowerCase()))) {
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
                String name = groups.getString("name");
                String type = groups.getString("type");
                boolean required = groups.has("required");
                String wrap = "%s";

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
