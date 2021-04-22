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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "abrir",
		aliases = {"open"},
		usage = "req_captcha",
		category = Category.FUN
)
public class CatchDropCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		TextChannel chn = gc.getDropChannel();

		if (chn != null && !channel.getId().equals(chn.getId())) {
			channel.sendMessage("❌ | O spawn de drops está configurado no canal " + chn.getAsMention() + ", você não pode coletá-los aqui.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-captcha")).queue();
			return;
		} else if (args[0].contains(Helper.ANTICOPY)) {
			channel.sendMessage("❌ | Espertinho né? Que tal tentarmos sem `Ctrl+C / Ctrl+V` para ficar justo?").queue();
			return;
		}

		Prize<?> p = Main.getInfo().getCurrentDrop().get(guild.getId());

		if (p == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-drop")).queue();
			return;
		} else if (!p.getRequirement().getValue().apply(author)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_requirements-not-fulfilled")).queue();
			return;
		} else if (!p.getRealCaptcha().equals(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-captcha")).queue();
			return;
		}

		Main.getInfo().getCurrentDrop().remove(guild.getId());
		p.award(author);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.addField("Conteúdo:", p.toString(author), true);

		channel.sendMessage("✅ | " + author.getAsMention() + " coletou o drop com sucesso!")
				.embed(eb.build())
				.queue();
	}
}
