/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;

public class BotInfoCommand extends Command {

	private static final String STR_BOT_INFO_SERVERS = "str_bot-info-servers";

	public BotInfoCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BotInfoCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BotInfoCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BotInfoCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-title"));
		eb.setThumbnail(Main.getInfo().getAPI().getSelfUser().getAvatarUrl());
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-1"), Main.getInfo().getUserByID(Main.getInfo().getNiiChan()).getAsTag(), true);
		StringBuilder sb = new StringBuilder();
		Main.getInfo().getDevelopers().forEach(d -> sb.append(Main.getInfo().getUserByID(d).getAsTag()).append(", "));
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-2"), sb.toString(), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-3"), Main.getInfo().getSelfUser().getTimeCreated().format(DateTimeFormatter.ofPattern(ShiroInfo.getLocale(I18n.PT).getString("date_format"))), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-4"), MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_BOT_INFO_SERVERS), TagDAO.getPartnerAmount()), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-5"), MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString(STR_BOT_INFO_SERVERS), Main.getInfo().getAPI().getGuilds().size()), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-6"), MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-registered-users"), Main.getInfo().getAPI().getUsers().size(), MemberDAO.getAllMembers().size()), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-7"), Main.getInfo().getVersion(), true);
		eb.addField(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-8"), "https://top.gg/bot/572413282653306901", true);
		eb.setImage(ShiroInfo.getLocale(I18n.PT).getString("str_bot-info-field-9"));

		channel.sendMessage(eb.build()).queue();
	}
}
