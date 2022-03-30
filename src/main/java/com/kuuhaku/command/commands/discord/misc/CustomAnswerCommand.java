/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
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
import com.kuuhaku.utils.*;
import com.kuuhaku.utils.helpers.*;
import com.kuuhaku.utils.json.JSONArray;
import com.kuuhaku.utils.json.JSONObject;
import com.squareup.moshi.JsonDataException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Command(
		name = "fale",
		aliases = {"custom"},
		usage = "req_json",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class CustomAnswerCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!MiscHelper.hasPermission(member, PrivilegeLevel.MOD) && !GuildDAO.getGuildById(guild.getId()).isAnyTell()) {
			channel.sendMessage(I18n.getString("err_custom-answer-community-disabled")).queue();
			return;
		} else if (LogicHelper.equalsAny(argsAsText, "lista", "list")) {
			List<Page> pages = new ArrayList<>();

			List<List<CustomAnswer>> answers = CollectionHelper.chunkify(CustomAnswerDAO.getCAByGuild(guild.getId()), 10);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":pencil: Respostas deste servidor:");
			for (List<CustomAnswer> chunk : answers) {
				eb.clearFields();

				for (CustomAnswer ca : chunk) {
					eb.addField(
							"`" + ca.getId() + "` - (`" + (ca.isAnywhere() ? "QUALQUER" : "EXATO") + (ca.getChance() != 100 ? " | " + ca.getChance() + "%" : "") + "`) " + ca.getTrigger(),
							StringUtils.abbreviate(ca.getAnswer(), 100),
							false
					);
				}

				pages.add(new InteractPage(eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("Não há nenhuma resposta cadastrada neste servidor.").queue();
				return;
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, Constants.USE_BUTTONS, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
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

			if (!ca.getUsers().isEmpty())
				eb.appendDescription("\nApenas se for ativado por " + CollectionHelper.parseAndJoin(ca.getUsers(), MiscHelper::getUsername));

			if (!ca.getChannels().isEmpty())
				eb.appendDescription("\nApenas se for ativado em " + CollectionHelper.parseAndJoin(ca.getChannels(), MiscHelper::getUsername));

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		}

		try {
			JSONObject jo = new JSONObject(argsAsText);

			if (!jo.has("trigger") || !jo.has("answer")) {
				channel.sendMessage("❌ | Você precisa especificar ao menos o gatilho (`trigger`) e a resposta (`answer`).").queue();
				return;
			} else if (!MathHelper.between(jo.getString("trigger").length(), 2, 129)) {
				channel.sendMessage("❌ | O gatilho tem que ter entre 2 e 128 caracteres.").queue();
				return;
			} else if (!MathHelper.between(jo.getString("answer").length(), 2, 1025)) {
				channel.sendMessage("❌ | A resposta tem que ter entre 2 e 1024 caracteres.").queue();
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
				ca.setChance(MathHelper.clamp(jo.getInt("chance"), 1, 100));
				msg += ", com uma chance de " + ca.getChance() + "%%";
			}

			if (jo.has("users")) {
				Set<String> names = new HashSet<>();
				JSONArray ja = jo.getJSONArray("users");
				for (Object id : ja) {
					Member m = guild.getMemberById(String.valueOf(id));
					if (m == null) {
						channel.sendMessage("❌ | Não encontrei nenhum usuário com o ID `" + id + "`.").queue();
						return;
					}

					ca.getUsers().add(m.getId());
					names.add(m.getAsMention());
				}

				msg += ", apenas quando " + CollectionHelper.properlyJoin().apply(names) + " usar" + (names.size() == 1 ? "" : "em") + " o gatilho";
			}

			if (jo.has("channels")) {
				Set<String> names = new HashSet<>();
				JSONArray ja = jo.getJSONArray("channels");
				for (Object id : ja) {
					TextChannel tc = guild.getTextChannelById(String.valueOf(id));
					if (tc == null) {
						channel.sendMessage("❌ | Não encontrei nenhum canal com o ID `" + id + "`.").queue();
						return;
					}

					ca.getChannels().add(tc.getId());
					names.add(tc.getAsMention());
				}

				msg += ", apenas o gatilho for usado " + (names.size() == 1 ? "no canal" : "nos canais") + " " + CollectionHelper.properlyJoin().apply(names);
			}
			msg += ".";

			CustomAnswerDAO.addCustomAnswer(ca);
			Main.getInfo().registerCustomAnswer(ca);
			channel.sendMessage(msg.formatted(StringUtils.abbreviate(ca.getAnswer().replace("\n", " "), 100), ca.getTrigger().replace("\n", " "))).queue();
		} catch (JsonDataException | IllegalStateException e) {
			channel.sendMessage("❌ | Olha, esse JSON não me parece certo não.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Algum dos parâmetros que você informou não parece certo (ex: a chance deve ser um valor de 1 à 100 sem %).").queue();
		}
	}
}
