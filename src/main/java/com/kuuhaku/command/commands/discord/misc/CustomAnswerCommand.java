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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.google.gson.JsonParseException;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CustomAnswerDAO;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "fale",
		aliases = {"custom"},
		usage = "req_json",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class CustomAnswerCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!Helper.hasPermission(member, PrivilegeLevel.MOD) && !GuildDAO.getGuildById(guild.getId()).isAnyTell()) {
			channel.sendMessage(I18n.getString("err_custom-answer-community-disabled")).queue();
			return;
		} else if (Helper.equalsAny(argsAsText, "lista", "list")) {
			List<Page> pages = new ArrayList<>();

			List<List<CustomAnswer>> answers = Helper.chunkify(CustomAnswerDAO.getCAByGuild(guild.getId()), 10);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":pencil: Respostas deste servidor:");
			for (List<CustomAnswer> chunk : answers) {
				eb.clearFields();

				for (CustomAnswer ca : chunk) {
					eb.addField(
							"`" + ca.getId() + "` - (`" + (ca.isAnywhere() ? "QUALQUER" : "EXATO") + (ca.getChance() != 100 ? (" | " + ca.getChance() + "%") : "") + (ca.getForUser() != null ? (" | " + Main.getInfo().getUserByID(ca.getForUser()).getName()) : "") + "`) " + ca.getTrigger(),
							StringUtils.abbreviate(ca.getAnswer(), 100),
							false
					);
				}

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("Não há nenhuma resposta cadastrada neste servidor.").queue();
				return;
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
			return;
		} else if (StringUtils.isNumeric(argsAsText)) {
			CustomAnswer ca = CustomAnswerDAO.getCAByIDAndGuild(Integer.parseInt(args[0]), guild.getId());

			if (ca == null) {
				channel.sendMessage(I18n.getString("err_custom-answer-not-found")).queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":speech_balloon: Resposta Nº " + ca.getId())
					.addField(":arrow_right: " + ca.getTrigger(), ":arrow_left: " + ca.getAnswer(), false);

			if (ca.isAnywhere())
				eb.setDescription("Se a mensagem contiver o gatilho");
			else
				eb.setDescription("Se a mensagem for exatamente o gatilho");

			if (ca.getChance() != 100)
				eb.appendDescription("\nCom " + ca.getChance() + "% de chance de eu responder");

			if (ca.getForUser() != null)
				eb.appendDescription("\nApenas se for ativado por " + Main.getInfo().getUserByID(ca.getForUser()));

			channel.sendMessage(eb.build()).queue();
			return;
		}

		try {
			JSONObject jo = new JSONObject(argsAsText);

			if (!jo.has("trigger") || !jo.has("answer")) {
				channel.sendMessage(I18n.getString("❌ | Você precisa especificar ao menos o gatilho (`trigger`) e a resposta (`answer`).")).queue();
				return;
			} else if (!Helper.between(jo.getString("trigger").length(), 2, 129)) {
				channel.sendMessage(I18n.getString("❌ | O gatilho tem que ter entre 2 e 128 caracteres.")).queue();
				return;
			} else if (!Helper.between(jo.getString("answer").length(), 2, 1025)) {
				channel.sendMessage(I18n.getString("❌ | A resposta tem que ter entre 2 e 1024 caracteres.")).queue();
				return;
			}

			String msg = "Agora irei responder `%s` quando alguém disser ";
			CustomAnswer ca = new CustomAnswer(guild.getId(), jo.getString("trigger"), jo.getString("answer"));
			if (jo.has("anywhere")) {
				ca.setAnywhere(jo.getBoolean("anywhere"));
				msg += "`%s` em qualquer lugar da mensagem";
			} else {
				msg += "exatamente `%s`";
			}

			if (jo.has("chance")) {
				ca.setChance(Helper.clamp(jo.getInt("chance"), 1, 100));
				msg += ", com uma chance de " + ca.getChance() + "%";
			}

			if (jo.has("forUser")) {
				Member m = guild.getMemberById(jo.getString("forUser"));
				if (m == null) {
					channel.sendMessage(I18n.getString("❌ | Não encontrei nenhum usuário com esse ID.")).queue();
					return;
				}

				ca.setForUser(m.getId());
				msg += ", apenas quando " + m.getAsMention() + " usar o gatilho";
			}
			msg += ".";

			CustomAnswerDAO.addCustomAnswer(ca);
			channel.sendMessage(msg.formatted(StringUtils.abbreviate(ca.getAnswer().replace("\n", " "), 100), ca.getTrigger().replace("\n", " "))).queue();
		} catch (JsonParseException | IllegalStateException e) {
			channel.sendMessage("❌ | Olha, esse JSON não me parece certo não.").queue();
		}
	}
}
