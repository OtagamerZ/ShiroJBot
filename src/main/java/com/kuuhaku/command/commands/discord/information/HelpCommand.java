/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.managers.Argument;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HelpCommand extends Command {

	private static final String STR_COMMAND_LIST_TITLE = "str_command-list-title";
	private static final String STR_COMMAND_LIST_DESCRIPTION = "str_command-list-description";

	public HelpCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public HelpCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public HelpCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public HelpCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		Map<String, Page> pages = new LinkedHashMap<>();

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		if (Helper.hasPermission(guild.getSelfMember(), Permission.MESSAGE_MANAGE, (TextChannel) channel) && args.length == 0) {
			eb.setTitle(ShiroInfo.getLocale(I18n.PT).getString(STR_COMMAND_LIST_TITLE));
			eb.setDescription(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_COMMAND_LIST_DESCRIPTION), prefix, Arrays.stream(Category.values()).filter(c -> c.isEnabled(gc, guild, author)).count(), Main.getCommandManager().getCommands().values().stream().filter(c -> c.getCategory().isEnabled(gc, guild, author)).count()));
			for (Category cat : Category.values()) {
				if (cat.isEnabled(gc, guild, author))
					eb.addField(cat.getEmote() + " | " + cat.getName(), Helper.VOID, true);
			}

			eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_tips"), Helper.VOID, true);

			eb.setColor(Color.PINK);
			eb.setFooter(Main.getInfo().getFullName(), null);
			eb.setThumbnail(Objects.requireNonNull(Main.getShiroShards().getEmoteById(Helper.HOME)).getImageUrl());

			pages.put(Helper.HOME, new Page(PageType.EMBED, eb.build()));

			for (Category cat : Category.values()) {
				EmbedBuilder ceb = new ColorlessEmbedBuilder();
				ceb.setTitle(cat.getName());
				ceb.setFooter(Main.getInfo().getFullName(), null);
				ceb.setThumbnail(Objects.requireNonNull(Main.getShiroShards().getEmoteById(cat.getEmoteId())).getImageUrl());

				ceb.setDescription(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_prefix"), prefix, cat.getCmds().size()));

				if (!cat.isEnabled(gc, guild, author))
					continue;
				if (cat.getCmds().size() == 0) {
					ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_empty-category"), cat.getDescription()), false);
					continue;
				}

				StringBuilder cmds = new StringBuilder();

				for (Argument cmd : cat.getCmds()) {
					cmds.append("`").append(cmd.getName()).append("`  ");
				}

				ceb.addField(Helper.VOID, cat.getDescription() + "\n" + cmds.toString().trim(), false);
				ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_command-list-single-help-tip"), prefix), false);
				pages.put(cat.getEmoteId(), new Page(PageType.EMBED, ceb.build()));
			}

			EmbedBuilder ceb = new ColorlessEmbedBuilder();
			ceb.setTitle(ShiroInfo.getLocale(I18n.PT).getString("str_tips-and-tricks"));
			ceb.setFooter(Main.getInfo().getFullName(), null);
			ceb.setThumbnail(Objects.requireNonNull(Objects.requireNonNull(Main.getShiroShards().getEmoteById("684039810079522846")).getImageUrl()));

			ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_quick-emote-tip"), prefix), false);
			ceb.addField(Helper.VOID, ShiroInfo.getLocale(I18n.PT).getString("str_pagination-tip"), false);
			ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_waifu-tip"), prefix), false);
			ceb.addField(Helper.VOID, ShiroInfo.getLocale(I18n.PT).getString("str_exceed-tip"), false);
			ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_kawaipon-tip"), prefix), false);
			ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_edit-message-tip"), prefix), false);
			ceb.addField(Helper.VOID, MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_loan-tip"), prefix), false);

			pages.put("684039810079522846", new Page(PageType.EMBED, ceb.build()));

			channel.sendMessage(eb.build()).queue(s -> Pages.categorize(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId())), Helper::doNothing);
			return;
		} else if (args.length == 0) {
			eb.setTitle(ShiroInfo.getLocale(I18n.PT).getString(STR_COMMAND_LIST_TITLE));
			eb.setDescription(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_COMMAND_LIST_DESCRIPTION), prefix, Arrays.stream(Category.values()).filter(c -> c.isEnabled(gc, guild, author)).count(), Main.getCommandManager().getCommands().values().stream().filter(c -> c.getCategory().isEnabled(gc, guild, author)).count()));
			eb.appendDescription(ShiroInfo.getLocale(I18n.PT).getString("str_command-list-alert"));
			StringBuilder sb = new StringBuilder();
			for (Category cat : Category.values()) {
				if (cat.isEnabled(gc, guild, author)) {
					sb.setLength(0);
					for (Argument c : cat.getCmds()) {
						sb.append("`").append(c.getName()).append("`  ");
					}
					eb.addField(cat.getEmote() + " | " + cat.getName(), sb.toString(), true);
				}
			}

			eb.setFooter(Main.getInfo().getFullName(), null);
			eb.setThumbnail(Objects.requireNonNull(Main.getShiroShards().getEmoteById(Helper.HOME)).getImageUrl());

			channel.sendMessage(eb.build()).queue();
			return;
		}

		eb.clear();
		eb.setFooter(Main.getInfo().getFullName(), null);
		eb.setThumbnail("https://images.vexels.com/media/users/3/152594/isolated/preview/d00d116b2c073ccf7f9fec677fec78e3---cone-de-ponto-de-interroga----o-quadrado-roxo-by-vexels.png");

		String cmdName = args[0];

		Argument cmd = null;

		for (Argument cmmd : Main.getCommandManager().getCommands().values()) {
			boolean found = false;

			if (cmmd.getName().equalsIgnoreCase(cmdName) && cmmd.getCategory().isEnabled(gc, guild, author))
				found = true;
			else if (cmmd.getName().equalsIgnoreCase(cmdName) && !cmmd.getCategory().isEnabled(gc, guild, author)) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_module-disabled")).queue();
				return;
			}

			for (String alias : cmmd.getAliases()) {
				if (alias.equalsIgnoreCase(cmdName) && cmmd.getCategory().isEnabled(gc, guild, author)) {
					found = true;
					break;
				} else if (alias.equalsIgnoreCase(cmdName) && !cmmd.getCategory().isEnabled(gc, guild, author)) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_module-disabled")).queue();
					return;
				}
			}

			if (found) {
				cmd = cmmd;
				break;
			}
		}

		if (cmd == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_command-not-found")).queue();
			return;
		}

		eb.setTitle(cmd.getName() + (cmd.getUsage() != null ? " " + cmd.getUsage() : ""));

		StringBuilder aliases = new StringBuilder(ShiroInfo.getLocale(I18n.PT).getString("str_aliases")).append(" ");

		for (String al : cmd.getAliases()) {
			aliases.append("`").append(al).append("`  ");
		}

		eb.setDescription(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_command-list-category"), cmd.getDescription(), cmd.getAliases().length != 0 ? aliases.toString().trim() + "\n" : "", cmd.getCategory().getName()));

		channel.sendMessage(eb.build()).queue();

	}
}
