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
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONArray;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Command(
		name = "enquete",
		aliases = {"poll"},
		usage = "req_question",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class PollCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_poll-no-question")).queue();
			return;
		}

		String text = String.join(" ", args);

		if (String.join(" ", args).length() < 10) {
			channel.sendMessage(I18n.getString("err_poll-too-short")).queue();
			return;
		} else if (text.length() > 2000 && !Helper.containsAll(text, ";", "[", "]")) {
			channel.sendMessage(I18n.getString("err_poll-too-long")).queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		JSONArray options = null;
		if (Helper.containsAll(text, ";", "[", "]")) {
			String[] s = text.split(";");

			try {
				options = new JSONArray(s[1]);
				text = s[0];
			} catch (IllegalStateException ignore) {
			}
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":notepad_spiral: Enquete criada por " + member.getUser().getName());
		eb.setDescription("Apenas 1 clique já contabiliza o voto (a quantidade de reações __**NÃO**__ ficará maior que 1).");
		eb.setThumbnail("https://www.kalkoken.org/apps/easypoll/resources/poll-logo.png");
		eb.setDescription(text);
		eb.setFooter("Clique nas reações abaixo para votar", null);
		eb.setColor(Color.decode("#2195f2"));

		Function<Message, Map<Emoji, ThrowingConsumer<ButtonWrapper>>> opts = null;
		if (options != null && options.size() > 10) {
			channel.sendMessage(I18n.getString("err_poll-too-many-options")).queue();
			return;
		} else if (options != null) {
			JSONArray finalOptions = options;
			opts = m -> {
				Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
				for (int i = 0; i < finalOptions.size(); i++) {
					String emote = Helper.getRegionalIndicator(i);
					buttons.put(Helper.parseEmoji(emote), wrapper -> {
						if (ShiroInfo.getPolls().get(m.getId()).containsKey(wrapper.getUser().getId())) return;
						ShiroInfo.getPolls().get(m.getId()).put(wrapper.getUser().getId(), emote);
						eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + ShiroInfo.getPolls().get(m.getId()).size() + ")");
						m.editMessageEmbeds(eb.build()).queue();
					});
				}
				buttons.put(Helper.parseEmoji("❌"), (wrapper) -> {
					if (wrapper.getUser().getId().equals(author.getId())) wrapper.getMessage().delete().queue();
					ShiroInfo.getPolls().remove(wrapper.getMessage().getId());
				});
				return buttons;
			};
		}

		if (options != null) {
			for (int i = 0; i < options.size(); i++)
				eb.addField(Helper.getRegionalIndicator(i) + " | " + options.getString(i), Helper.VOID, true);

			for (int i = 0; i < 3 - eb.getFields().size() % 3; i++)
				eb.addBlankField(true);
		}

		Consumer<Message> sendSimple = m -> {
			Pages.buttonize(m, new LinkedHashMap<>() {{
				put(Helper.parseEmoji("\uD83D\uDC4D"), wrapper -> {
					if (ShiroInfo.getPolls().get(m.getId()).containsKey(wrapper.getUser().getId())) return;
					ShiroInfo.getPolls().get(m.getId()).put(wrapper.getUser().getId(), "\uD83D\uDC4D");
					eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + ShiroInfo.getPolls().get(m.getId()).size() + ")");
					m.editMessageEmbeds(eb.build()).queue();
				});
				put(Helper.parseEmoji("\uD83D\uDC4E"), wrapper -> {
					if (ShiroInfo.getPolls().get(m.getId()).containsKey(wrapper.getUser().getId())) return;
					ShiroInfo.getPolls().get(m.getId()).put(wrapper.getUser().getId(), "\uD83D\uDC4E");
					eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + ShiroInfo.getPolls().get(m.getId()).size() + ")");
					m.editMessageEmbeds(eb.build()).queue();
				});
				put(Helper.parseEmoji("❌"), wrapper -> {
					if (wrapper.getUser().getId().equals(author.getId())) {
						wrapper.getMessage().delete().queue();
						ShiroInfo.getPolls().remove(wrapper.getMessage().getId());
					}
				});
			}}, ShiroInfo.USE_BUTTONS, false, (int) gc.getPollTime(), TimeUnit.MILLISECONDS);
			ShiroInfo.getPolls().put(m.getId(), new HashMap<>());
			Main.getInfo().getScheduler().schedule(() -> showResult(m, member, eb), (int) gc.getPollTime(), TimeUnit.MILLISECONDS);
		};

		Function<Message, Map<Emoji, ThrowingConsumer<ButtonWrapper>>> finalOpts = opts;
		Consumer<Message> sendOptions = m -> {
			assert finalOpts != null;
			Pages.buttonize(m, finalOpts.apply(m), ShiroInfo.USE_BUTTONS, false, (int) gc.getPollTime(), TimeUnit.MILLISECONDS);
			ShiroInfo.getPolls().put(m.getId(), new HashMap<>());
			Main.getInfo().getScheduler().schedule(() -> showResultOP(m, member, eb), (int) gc.getPollTime(), TimeUnit.MILLISECONDS);
		};

		if (gc.getSuggestionChannel() == null) {
			gc.setSuggestionChannel(null);
			GuildDAO.updateGuildSettings(gc);

			if (options != null) channel.sendMessageEmbeds(eb.build()).queue(sendOptions);
			else channel.sendMessageEmbeds(eb.build()).queue(sendSimple);
		} else {
			try {
				if (options != null)
					gc.getSuggestionChannel().sendMessageEmbeds(eb.build()).queue(sendOptions);
				else
					gc.getSuggestionChannel().sendMessageEmbeds(eb.build()).queue(sendSimple);
			} catch (Exception e) {
				try {
					if (gc.getSuggestionChannel() == null)
						channel.sendMessage(I18n.getString("err_send-embed")).queue();
					else
						channel.sendMessage(I18n.getString("err_send-embed-in-channel", gc.getSuggestionChannel().getAsMention())).queue();
					return;
				} catch (NullPointerException ex) {
					gc.setSuggestionChannel(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(I18n.getString("err_send-embed")).queue();
				}
			}
		}

		channel.sendMessage("✅ | Enquete criada com sucesso, ela encerrará automaticamente em " + Helper.toStringDuration(gc.getPollTime()) + ".").queue();
	}

	private static void showResult(Message msg, Member member, EmbedBuilder eb) {
		int pos = (int) ShiroInfo.getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals("\uD83D\uDC4D")).count();
		int neg = (int) ShiroInfo.getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals("\uD83D\uDC4E")).count();
		ShiroInfo.getPolls().remove(msg.getId());
		boolean NOVOTE = pos == 0 && neg == 0;

		eb.setAuthor("A enquete feita por " + member.getUser().getName() + " foi encerrada!");
		eb.setTitle("Enquete: (" + (NOVOTE ? "nenhum voto" : (pos + neg) + " votos") + ")");
		eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round(Helper.prcntToInt(pos, (pos + neg)), 1) + "%", true);
		eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round(Helper.prcntToInt(neg, (pos + neg)), 1) + "%", true);
		eb.setFooter(null);

		msg.editMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
		member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(eb.setAuthor("Sua enquete foi encerrada!").build()).queue());
		msg.clearReactions().queue(null, Helper::doNothing);
	}

	private static void showResultOP(Message msg, Member member, EmbedBuilder eb) {
		Map<String, Integer> votes = new HashMap<>();
		for (Map.Entry<String, String> entry : ShiroInfo
				.getPolls()
				.get(msg.getId()).entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			votes.put(value, (int) ShiroInfo.getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals(value)).count());
		}

		ShiroInfo.getPolls().remove(msg.getId());
		boolean NOVOTE = false;
		int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();

		if (totalVotes == 0) {
			NOVOTE = true;
		}

		eb.setAuthor("A enquete feita por " + member.getUser().getName() + " foi encerrada!");
		eb.setTitle("Enquete: (" + (NOVOTE ? "nenhum voto" : totalVotes + " votos") + ")");
		eb.setFooter(null);
		eb.addBlankField(false);

		boolean finalNOVOTE = NOVOTE;
		List<MessageEmbed.Field> fields = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : votes.entrySet()) {
			String k = entry.getKey();
			Integer v = entry.getValue();
			fields.add(new MessageEmbed.Field(k + " | " + (finalNOVOTE ? "0.0%" : Helper.round(Helper.prcntToInt(v, totalVotes), 1) + "%"), Helper.VOID, true));
		}

		fields.sort(Comparator.comparing(MessageEmbed.Field::getName));

		for (MessageEmbed.Field field : fields) {
			eb.addField(field);
		}
		msg.editMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
		member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(eb.setAuthor("Sua enquete foi encerrada!").build()).queue());
		msg.clearReactions().queue(null, Helper::doNothing);
	}
}
