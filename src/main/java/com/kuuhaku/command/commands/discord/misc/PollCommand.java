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
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;

import java.awt.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PollCommand implements Executable {

	public PollCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PollCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PollCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PollCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-no-question")).queue();
			return;
		}

		String text = String.join(" ", args);

		if (String.join(" ", args).length() < 10) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-too-short")).queue();
			return;
		} else if (text.length() > 2000 && !Helper.containsAll(text, ";", "[", "]")) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-too-long")).queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		JSONArray options = null;
		if (Helper.containsAll(text, ";", "[", "]")) {
			String[] s = text.split(";");

			try {
				options = new JSONArray(s[1]);
				text = s[0];
			} catch (JSONException ignore) {
			}
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":notepad_spiral: Enquete criada por " + member.getEffectiveName());
		eb.setDescription("Apenas 1 clique já contabiliza o voto (a quantidade de reações __**NÃO**__ ficará maior que 1).");
		eb.setThumbnail("https://www.kalkoken.org/apps/easypoll/resources/poll-logo.png");
		eb.setDescription(text);
		eb.setFooter("Clique nas reações abaixo para votar", null);
		eb.setColor(Color.decode("#2195f2"));

		Function<Message, Map<String, BiConsumer<Member, Message>>> opts = null;
		if (options != null && options.length() > 10) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-too-many-options")).queue();
			return;
		} else if (options != null) {
			JSONArray finalOptions = options;
			opts = m -> {
				Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
				for (int i = 0; i < finalOptions.length(); i++) {
					String emote = Helper.getRegionalIndicator(i);
					buttons.put(emote, (mb, msg) -> {
						if (Main.getInfo().getPolls().get(m.getId()).containsKey(mb.getId())) return;
						Main.getInfo().getPolls().get(m.getId()).put(mb.getId(), emote);
						eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + Main.getInfo().getPolls().get(m.getId()).size() + ")");
						m.editMessage(eb.build()).queue();
					});
				}
				buttons.put("❌", (mb, msg) -> {
					if (mb.getId().equals(author.getId())) msg.delete().queue();
					Main.getInfo().getPolls().remove(msg.getId());
				});
				return buttons;
			};
		}

		if (options != null) {
			for (int i = 0; i < options.length(); i++)
				eb.addField(Helper.getRegionalIndicator(i) + " | " + options.getString(i), Helper.VOID, true);

			for (int i = 0; i < 3 - eb.getFields().size() % 3; i++)
				eb.addBlankField(true);
		}

		Consumer<Message> sendSimple = m -> {
			Pages.buttonize(m, new LinkedHashMap<>() {{
				put("\uD83D\uDC4D", (mb, msg) -> {
					if (Main.getInfo().getPolls().get(m.getId()).containsKey(mb.getId())) return;
					Main.getInfo().getPolls().get(m.getId()).put(mb.getId(), "\uD83D\uDC4D");
					eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + Main.getInfo().getPolls().get(m.getId()).size() + ")");
					m.editMessage(eb.build()).queue();
				});
				put("\uD83D\uDC4E", (mb, msg) -> {
					if (Main.getInfo().getPolls().get(m.getId()).containsKey(mb.getId())) return;
					Main.getInfo().getPolls().get(m.getId()).put(mb.getId(), "\uD83D\uDC4E");
					eb.setFooter("Clique nas reações abaixo para votar (total de votos: " + Main.getInfo().getPolls().get(m.getId()).size() + ")");
					m.editMessage(eb.build()).queue();
				});
				put("❌", (mb, msg) -> {
					if (mb.getId().equals(author.getId())) {
						msg.delete().queue();
						Main.getInfo().getPolls().remove(msg.getId());
					}
				});
			}}, false, gc.getPollTime(), TimeUnit.SECONDS);
			Main.getInfo().getPolls().put(m.getId(), new HashMap<>());
			Main.getInfo().getScheduler().schedule(() -> showResult(m, member, eb), gc.getPollTime(), TimeUnit.SECONDS);
		};

		Function<Message, Map<String, BiConsumer<Member, Message>>> finalOpts = opts;
		Consumer<Message> sendOptions = m -> {
			assert finalOpts != null;
			Pages.buttonize(m, finalOpts.apply(m), false, gc.getPollTime(), TimeUnit.SECONDS);
			Main.getInfo().getPolls().put(m.getId(), new HashMap<>());
			Main.getInfo().getScheduler().schedule(() -> showResultOP(m, member, eb), gc.getPollTime(), TimeUnit.SECONDS);
		};

		if (gc.getCanalSUG() == null || gc.getCanalSUG().isBlank()) {
			gc.setCanalSUG("");
			GuildDAO.updateGuildSettings(gc);

			if (options != null) channel.sendMessage(eb.build()).queue(sendOptions);
			else channel.sendMessage(eb.build()).queue(sendSimple);
		} else {
			try {
				if (options != null)
					Objects.requireNonNull(guild.getTextChannelById(gc.getCanalSUG())).sendMessage(eb.build()).queue(sendOptions);
				else
					Objects.requireNonNull(guild.getTextChannelById(gc.getCanalSUG())).sendMessage(eb.build()).queue(sendSimple);
			} catch (Exception e) {
				try {
					if (gc.getCanalSUG() == null || gc.getCanalSUG().isBlank())
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed")).queue();
					else
						channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed-in-channel"), Objects.requireNonNull(guild.getTextChannelById(gc.getCanalSUG())).getAsMention())).queue();
					return;
				} catch (NullPointerException ex) {
					gc.setCanalSUG(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed")).queue();
				}
			}
		}

		channel.sendMessage("✅ | Enquete criada com sucesso, ela encerrará automaticamente em " + gc.getPollTime() + " segundos.").queue();
	}

	private static void showResult(Message msg, Member member, EmbedBuilder eb) {
		int pos = (int) Main.getInfo().getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals("\uD83D\uDC4D")).count();
		int neg = (int) Main.getInfo().getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals("\uD83D\uDC4E")).count();
		Main.getInfo().getPolls().remove(msg.getId());
		boolean NOVOTE = false;

		if (pos == 0 && neg == 0) {
			NOVOTE = true;
		}

		eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
		eb.setTitle("Enquete: (" + (NOVOTE ? "nenhum voto" : (pos + neg) + " votos") + ")");
		eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round(Helper.prcntToInt(pos, (pos + neg)), 1) + "%", true);
		eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round(Helper.prcntToInt(neg, (pos + neg)), 1) + "%", true);
		eb.setFooter(null);

		msg.editMessage(eb.build()).queue(null, Helper::doNothing);
		member.getUser().openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).queue());
		msg.clearReactions().queue(null, Helper::doNothing);
	}

	private static void showResultOP(Message msg, Member member, EmbedBuilder eb) {
		Map<String, Integer> votes = new HashMap<>();
		for (Map.Entry<String, String> entry : Main.getInfo()
				.getPolls()
				.get(msg.getId()).entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			votes.put(value, (int) Main.getInfo().getPolls().get(msg.getId()).entrySet().stream().filter(e -> e.getValue().equals(value)).count());
		}

		Main.getInfo().getPolls().remove(msg.getId());
		boolean NOVOTE = false;
		int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();

		if (totalVotes == 0) {
			NOVOTE = true;
		}

		eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
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
		msg.editMessage(eb.build()).queue(null, Helper::doNothing);
		member.getUser().openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).queue());
		msg.clearReactions().queue(null, Helper::doNothing);
	}
}
