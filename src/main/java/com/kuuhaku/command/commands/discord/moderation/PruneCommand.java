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

package com.kuuhaku.command.commands.discord.moderation;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.*;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Command(
		name = "apagar",
		aliases = {"prune", "clean", "limpar"},
		usage = "req_qtd-all",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
public class PruneCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (ShiroInfo.getPruneQueue().contains(guild.getId())) {
			channel.sendMessage("❌ | Ainda estou deletando as mensagens de antes, tenha calma!").queue();
			return;
		}

		MessageHistory hist = channel.getHistory();
		Predicate<Message> cond = Objects::nonNull;
		Function<Integer, String> out;

		if (args.length == 0) {
			hist.retrievePast(100).complete();
			cond = cond.and(m -> m.getAuthor().isBot());

			out = size -> {
				if (size != 1)
					return "✅ | " + size + " mensagens de bots limpas.";
				else
					return "✅ | " + size + " mensagem de bot limpa.";
			};
		} else if (StringUtils.isNumeric(args[0]) && args[0].length() >= 10) {
			hist.retrievePast(100).complete();
			cond = cond.and(m -> m.getAuthor().getId().equals(args[0]));

			out = size -> {
				if (size != 1)
					return "✅ | " + size + " mensagens de <@" + args[0] + "> limpas.";
				else
					return "✅ | " + size + " mensagem de <@" + args[0] + "> limpa.";
			};
		} else if (StringUtils.isNumeric(args[0])) {
			int amount = Integer.parseInt(args[0]);
			if (!MathHelper.between(amount, 1, 1001)) {
				channel.sendMessage("❌ | Só é possível apagar entre 1 e 1000 mensagens de uma vez.").queue();
				return;
			}

			int overflow = amount % 100;
			for (int i = 0; i < (amount - overflow) / 100; i++) {
				hist.retrievePast(100).complete();
			}

			if (overflow > 0)
				hist.retrievePast(overflow).complete();

			out = size -> {
				if (size != 1)
					return "✅ | " + size + " mensagens limpas.";
				else
					return "✅ | " + size + " mensagem limpa.";
			};
		} else if (message.getMentionedUsers().size() > 0) {
			hist.retrievePast(100).complete();
			User target = message.getMentionedUsers().get(0);

			cond = cond.and(m -> m.getAuthor().getId().equals(target.getId()));

			out = size -> {
				if (size != 1)
					return "✅ | " + size + " mensagens de " + target.getAsMention() + " limpas.";
				else
					return "✅ | " + size + " mensagem de " + target.getAsMention() + " limpa.";
			};
		} else if (LogicHelper.equalsAny(args[0], "user", "usuarios")) {
			hist.retrievePast(100).complete();
			cond = cond.and(m -> !m.getAuthor().isBot());

			out = size -> {
				if (size != 1)
					return "✅ | " + size + " mensagens de usuários limpas.";
				else
					return "✅ | " + size + " mensagem de usuário limpa.";
			};
		} else if (LogicHelper.equalsAny(args[0], "all", "tudo")) {
			channel.retrievePinnedMessages().queue(p -> {
				Main.getInfo().getConfirmationPending().put(author.getId(), true);

				if (p.size() > 0)
					channel.sendMessage("Há " + p.size() + " mensage" + (p.size() == 1 ? "m fixada " : "ns fixadas ") + "neste canal, tem certeza que deseja limpá-lo?")
							.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										channel.createCopy()
												.setPosition(channel.getPosition())
												.queue(c -> {
													try {
														channel.delete().queue();
														c.sendMessage("✅ | Canal limpo com sucesso!").queue(null, MiscHelper::doNothing);
													} catch (InsufficientPermissionException e) {
														channel.sendMessage(I18n.getString("err_prune-permission-required")).queue(null, MiscHelper::doNothing);
													}
												});
									}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
									u -> u.getId().equals(author.getId()),
									ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
							));
				else
					channel.sendMessage("O canal será recriado, tem certeza que deseja limpá-lo?")
							.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										channel.createCopy()
												.setPosition(channel.getPosition())
												.queue(c -> {
													try {
														channel.delete().queue();
														c.sendMessage("✅ | Canal limpo com sucesso!").queue(null, MiscHelper::doNothing);
													} catch (InsufficientPermissionException e) {
														channel.sendMessage(I18n.getString("err_prune-permission-required")).queue(null, MiscHelper::doNothing);
													}
												});
									}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
									u -> u.getId().equals(author.getId()),
									ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
							));
			});

			return;
		} else {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
			return;
		}

		List<Message> msgs = hist.getRetrievedHistory().stream()
				.filter(cond)
				.filter(m -> m.getTimeCreated().isAfter(OffsetDateTime.now().minusWeeks(2)))
				.collect(Collectors.toList());

		int pinned = msgs.size();
		msgs.removeIf(Message::isPinned);
		pinned -= msgs.size();

		String msg = out.apply(msgs.size());
		if (pinned > 1)
			msg += " (" + pinned + " mensagens ignoradas)";
		else if (pinned == 1)
			msg += " (" + pinned + " mensagem ignorada)";

		ShiroInfo.getPruneQueue().add(guild.getId());
		try {
			if (msgs.size() > 2) {
				List<List<Message>> chunks = CollectionHelper.chunkify(msgs, 100);
				for (List<Message> chunk : chunks) {
					channel.deleteMessages(chunk).complete();

					Thread.sleep(1500);
				}

				channel.sendMessage(msg).queue();
			} else if (msgs.size() == 1) {
				msgs.get(0).delete().complete();

				channel.sendMessage(msg).queue();
			} else {
				channel.sendMessage("Nenhuma mensagem deletada.").submit().get();
			}
		} catch (ExecutionException | InterruptedException ignore) {
		}
		ShiroInfo.getPruneQueue().remove(guild.getId());
	}
}
