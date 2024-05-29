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

package com.kuuhaku.command.info;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.CategorizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.StringTree;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.PreparedCommand;
import com.kuuhaku.util.SignatureParser;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(
		name = "help",
		category = Category.INFO
)
@Signature("<command:word>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_EXT_EMOJI
})
public class HelpCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		String cmd = args.getString("command");
		if (cmd.isBlank()) {
			showHomePage(bot, locale, event);
			return;
		}

		String[] parts = cmd.split("\\.");
		JSONObject serverAliases = data.config().getSettings().getAliases();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];

			if (serverAliases.has(part)) {
				parts[i] = serverAliases.getString(part);
			}
		}

		JSONObject userAliases = acc.getSettings().getAliases();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];

			if (userAliases.has(part)) {
				parts[i] = userAliases.getString(part);
			}
		}

		String command = String.join(".", parts);
		PreparedCommand pc = Main.getCommandManager().getCommand(command);
		if (pc == null) {
			event.channel().sendMessage(locale.get("error/command_not_found")).queue();
			return;
		}

		Set<String> aliases = new LinkedHashSet<>();
		Stream.of(serverAliases.entrySet(), userAliases.entrySet())
				.flatMap(Set::stream)
				.filter(e -> e.getValue().equals(command))
				.map(Map.Entry::getKey)
				.forEach(aliases::add);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/command", pc.name()))
				.addField(locale.get("str/category"), pc.category().getName(locale), true)
				.setFooter(Constants.BOT_NAME + " " + Constants.BOT_VERSION.call());

		if (!aliases.isEmpty()) {
			eb.addField("Alias",
					aliases.stream()
							.map(s -> "`" + data.config().getPrefix() + s + "`")
							.collect(Collectors.joining("\n")),
					true
			);
		}

		List<String> sigs = SignatureParser.extract(locale, pc.command());
		if (!sigs.isEmpty()) {
			eb.addField(
					locale.get("str/command_signatures"),
					"```css\n" + String.join("\n", sigs).formatted(data.config().getPrefix(), pc.name()) + "\n```",
					false
			);
		}

		Set<PreparedCommand> subCmds = Main.getCommandManager().getSubCommands(pc.name().split("\\.")[0]);
		if (!subCmds.isEmpty()) {
			StringTree tree = new StringTree();

			for (PreparedCommand sub : subCmds) {
				String[] path = sub.name().split("(?=\\.)");
				path[0] = data.config().getPrefix() + path[0];

				tree.addElement(path[path.length - 1], path);
			}

			eb.addField(locale.get("str/subcommands"), "```" + tree + "```", false);
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}

	private void showHomePage(JDA bot, I18N locale, MessageData.Guild event) {
		List<Category> categories = new ArrayList<>();
		for (Category cat : Category.values()) {
			if (cat.check(event.member())) {
				categories.add(cat);
			}
		}

		EmbedBuilder index = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/all_commands"))
				.appendDescription(locale.get("str/category_counter", categories.size()) + "\n")
				.appendDescription(locale.get("str/command_counter", categories.stream().map(Category::getCommands).mapToInt(Set::size).sum()))
				.setFooter(Constants.BOT_NAME + " " + Constants.BOT_VERSION.call());

		Map<Emoji, Page> pages = new LinkedHashMap<>();
		for (Category cat : categories) {
			CustomEmoji emt = cat.getEmote();
			if (emt == null) continue;

			index.addField(emt.getAsMention() + " " + cat.getName(locale), cat.getDescription(locale), true);
		}

		CustomEmoji home = bot.getEmojiById("674261700366827539");
		if (home != null) {
			index.setThumbnail(home.getImageUrl());
			pages.put(Utils.parseEmoji(home.getId()), InteractPage.of(index.build()));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setFooter(Constants.BOT_NAME + " " + Constants.BOT_VERSION.call());

		for (Category cat : categories) {
			CustomEmoji emt = cat.getEmote();
			if (emt == null) continue;

			eb.clear()
					.setTitle(cat.getName(locale))
					.setThumbnail(emt.getImageUrl())
					.appendDescription(cat.getDescription(locale) + "\n\n")
					.appendDescription(locale.get("str/command_counter", cat.getCommands().size()))
					.setFooter(Constants.BOT_NAME + " " + Constants.BOT_VERSION.call());

			pages.put(Utils.parseEmoji(emt.getId()), Utils.generatePage(eb, cat.getCommands(), 10, cmd -> {
				if (cmd.name().contains(".")) return null;

				int subs = cmd.getSubCommands().size();
				if (subs > 0) {
					return "`" + cmd.name() + "` **(+" + subs + ")**";
				} else {
					return "`" + cmd.name() + "`";
				}
			}));
		}

		CategorizeHelper helper = new CategorizeHelper(pages, true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals);

		assert home != null;
		helper.apply(event.channel().sendMessageEmbeds(index.build()));
	}
}
