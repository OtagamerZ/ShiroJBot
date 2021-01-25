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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class URankCommand extends Command {

	private static final String STR_LEVEL = "str_level";
	private static final String STR_CREDIT = "str_credit";
	private static final String STR_CARD = "str_card";
	private static final String SRT_USER_RANKING_TITLE = "str_user-ranking-title";
	private static final String STR_GLOBAL = "str_global";
	private static final String STR_LOCAL = "str_local";

	public URankCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public URankCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public URankCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public URankCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			ArrayList<Page> pages = new ArrayList<>();

			if (args.length > 0 && args[0].equalsIgnoreCase("global"))
				getLevelRanking(pages, guild, true);
			else if (args.length > 0 && Helper.equalsAny(args[0], "credit", "creditos", "créditos"))
				getCreditRanking(pages);
			else if (args.length > 0 && Helper.equalsAny(args[0], "card", "kawaipon", "cartas"))
				getCardRanking(pages);
			else
				getLevelRanking(pages, guild, false);

			m.delete().queue();
			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
		});
	}

	private void getLevelRanking(List<Page> pages, Guild guild, boolean global) {
		List<com.kuuhaku.model.persistent.Member> mbs;
		if (global) {
			mbs = MemberDAO.getMemberRank(null, true);
		} else {
			mbs = MemberDAO.getMemberRank(guild.getId(), false);
		}

		mbs.removeIf(mb -> checkUser(mb).isBlank());

		String champ = "1 - %s %s %s".formatted(
				global ? checkGuild(mbs.get(0)) : "",
				checkUser(mbs.get(0)),
				MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_LEVEL), mbs.get(0).getLevel())
		);
		List<com.kuuhaku.model.persistent.Member> sub9 = mbs.subList(1, Math.min(mbs.size(), 10));
		StringBuilder sub9Formatted = new StringBuilder();
		for (int i = 0; i < sub9.size(); i++) {
			sub9Formatted.append("%s - %s %s %s\n".formatted(
					i + 2,
					global ? checkGuild(sub9.get(i)) : "",
					checkUser(sub9.get(i)),
					MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_LEVEL), sub9.get(i).getLevel())
			));
		}

		StringBuilder next10 = new StringBuilder();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		makeEmbed(global, pages, sub9Formatted, eb, champ);

		for (int x = 1; x < Math.ceil(mbs.size() / 10f); x++) {
			eb.clear();
			next10.setLength(0);
			for (int i = 10 * x; i < mbs.size() && i < (10 * x) + 10; i++) {
				next10.append("%s - %s %s %s\n".formatted(
						i + 1,
						global ? checkGuild(mbs.get(i)) : "",
						checkUser(mbs.get(i)),
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_LEVEL), mbs.get(i).getLevel())
				));
			}

			makeEmbed(global, pages, next10, eb, Helper.VOID);
		}
	}

	private void getCreditRanking(List<Page> pages) {
		List<Account> accs = AccountDAO.getAccountRank();
		accs.removeIf(acc -> checkUser(acc).isBlank());

		String champ = "1 - %s %s créditos".formatted(
				checkUser(accs.get(0)),
				MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CREDIT), Helper.separate(accs.get(0).getBalance()))
		);
		List<Account> sub9 = accs.subList(1, Math.min(accs.size(), 10));
		StringBuilder sub9Formatted = new StringBuilder();
		for (int i = 0; i < sub9.size(); i++) {
			sub9Formatted.append("%s - %s %s créditos\n".formatted(
					i + 2,
					checkUser(sub9.get(i)),
					MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CREDIT), Helper.separate(sub9.get(i).getBalance()))
			));
		}

		StringBuilder next10 = new StringBuilder();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		makeEmbed(true, pages, sub9Formatted, eb, champ);

		for (int x = 1; x < Math.ceil(accs.size() / 10f); x++) {
			eb.clear();
			next10.setLength(0);
			for (int i = 10 * x; i < accs.size() && i < (10 * x) + 10; i++) {
				next10.append("%s - %s %s\n".formatted(
						i + 1,
						checkUser(accs.get(i)),
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CREDIT), accs.get(i).getBalance())
				));
			}

			makeEmbed(true, pages, next10, eb, Helper.VOID);
		}
	}

	private void getCardRanking(List<Page> pages) {
		List<Object[]> kps = KawaiponDAO.getCardRank();
		kps.removeIf(kp -> checkUser(kp).isBlank());

		String champ = "1 - %s %s".formatted(
				checkUser(kps.get(0)),
				MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CARD), kps.get(0)[1])
		);
		List<Object[]> sub9 = kps.subList(1, Math.min(kps.size(), 10));
		StringBuilder sub9Formatted = new StringBuilder();
		for (int i = 0; i < sub9.size(); i++) {
			sub9Formatted.append("%s - %s %s\n".formatted(
					i + 2,
					checkUser(sub9.get(i)),
					MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CARD), sub9.get(i)[1])
			));
		}

		StringBuilder next10 = new StringBuilder();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		makeEmbed(true, pages, sub9Formatted, eb, champ);

		for (int x = 1; x < Math.ceil(kps.size() / 10f); x++) {
			eb.clear();
			next10.setLength(0);
			for (int i = 10 * x; i < kps.size() && i < (10 * x) + 10; i++) {
				next10.append("%s - %s %s\n".formatted(
						i + 1,
						checkUser(kps.get(i)),
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_CARD), kps.get(i)[1])
				));
			}

			makeEmbed(true, pages, next10, eb, Helper.VOID);
		}
	}

	private void makeEmbed(boolean global, List<Page> pages, StringBuilder next10, EmbedBuilder eb, String aVoid) {
		eb.setTitle(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(SRT_USER_RANKING_TITLE), global ? ShiroInfo.getLocale(I18n.PT).getString(STR_GLOBAL) : ShiroInfo.getLocale(I18n.PT).getString(STR_LOCAL)));
		eb.addField(aVoid, next10.toString(), false);
		eb.setThumbnail("http://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");

		pages.add(new Page(PageType.EMBED, eb.build()));
	}

	private static String checkUser(com.kuuhaku.model.persistent.Member m) {
		try {
			return Main.getInfo().getUserByID(m.getMid()).getName();
		} catch (Exception e) {
			return "";
		}
	}

	private static String checkUser(Object[] kp) {
		try {
			return Main.getInfo().getUserByID(String.valueOf(kp[0])).getName();
		} catch (Exception e) {
			return "";
		}
	}

	private static String checkUser(Account acc) {
		try {
			return Main.getInfo().getUserByID(acc.getUserId()).getName();
		} catch (Exception e) {
			return "";
		}
	}

	private static String checkGuild(com.kuuhaku.model.persistent.Member m) {
		try {
			return "(" + Main.getInfo().getGuildByID(m.getId().replace(m.getMid(), "")).getName() + ")";
		} catch (Exception e) {
			return "";
		}
	}
}
