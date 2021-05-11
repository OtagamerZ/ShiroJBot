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

package com.kuuhaku.command.commands.discord.moderation;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "apagar",
		aliases = {"prune", "clean", "limpar"},
		usage = "req_qtd-all",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_MANAGE})
public class PruneCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		message.delete()
				.flatMap(s -> channel.getHistory().retrievePast(100))
				.queue(msgs -> {
					String msg = "✅ | ";

					int pinned = msgs.size();
					msgs.removeIf(Message::isPinned);
					pinned -= msgs.size();

					if (args.length == 0) {
						msgs.removeIf(m -> !m.getAuthor().isBot());

						if (msgs.size() != 1)
							msg += msgs.size() + " mensagens de bots limpas.";
						else
							msg += msgs.size() + " mensagem de bot limpa.";
					} else if (StringUtils.isNumeric(args[0]) && args[0].length() >= 10) {
						msgs.removeIf(m -> !m.getAuthor().getId().equals(args[0]));

						if (msgs.size() != 1)
							msg += msgs.size() + " mensagens de <@" + args[0] + "> limpas.";
						else
							msg += msgs.size() + " mensagem de <@" + args[0] + "> limpa.";
					} else if (StringUtils.isNumeric(args[0])) {
						int amount = Integer.parseInt(args[0]);
						if (!Helper.between(amount, 1, 101)) {
							channel.sendMessage("❌ | Só é possível apagar entre 1 e 100 mensagens de uma vez").queue();
							return;
						}

						msgs = msgs.subList(0, Math.min(amount, msgs.size()));

						if (msgs.size() != 1)
							msg += msgs.size() + " mensagens de limpas.";
						else
							msg += msgs.size() + " mensagem de limpa.";
					} else if (message.getMentionedUsers().size() > 0) {
						User target = message.getMentionedUsers().get(0);

						msgs.removeIf(m -> !m.getAuthor().getId().equals(target.getId()));

						if (msgs.size() != 1)
							msg += msgs.size() + " mensagens de " + target.getAsMention() + " limpas.";
						else
							msg += msgs.size() + " mensagem de " + target.getAsMention() + " limpa.";
					} else if (Helper.equalsAny(args[0], "user", "usuarios")) {
						msgs.removeIf(m -> m.getAuthor().isBot());

						if (msgs.size() != 1)
							msg += msgs.size() + " mensagens de usuarios limpas.";
						else
							msg += msgs.size() + " mensagem de usuario limpa.";
					} else if (Helper.equalsAny(args[0], "all", "tudo")) {
						channel.retrievePinnedMessages().queue(p -> {
							Main.getInfo().getConfirmationPending().put(author.getId(), true);

							if (p.size() > 0)
								channel.sendMessage("Há " + p.size() + " mensage" + (p.size() == 1 ? "m fixada " : "ns fixadas ") + "neste canal, tem certeza que deseja limpá-lo?")
										.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
													Main.getInfo().getConfirmationPending().remove(author.getId());
													channel.createCopy()
															.setPosition(channel.getPosition())
															.queue(c -> {
																try {
																	channel.delete().queue();
																	c.sendMessage("✅ | Canal limpo com sucesso!").queue(null, Helper::doNothing);
																} catch (InsufficientPermissionException e) {
																	channel.sendMessage(I18n.getString("err_prune-permission-required")).queue(null, Helper::doNothing);
																}
															});
												}), true, 1, TimeUnit.MINUTES,
												u -> u.getId().equals(author.getId()),
												ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
										));
							else
								channel.sendMessage("O canal será recriado, tem certeza que deseja limpá-lo?")
										.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
													Main.getInfo().getConfirmationPending().remove(author.getId());
													channel.createCopy()
															.setPosition(channel.getPosition())
															.queue(c -> {
																try {
																	channel.delete().queue();
																	c.sendMessage("✅ | Canal limpo com sucesso!").queue(null, Helper::doNothing);
																} catch (InsufficientPermissionException e) {
																	channel.sendMessage(I18n.getString("err_prune-permission-required")).queue(null, Helper::doNothing);
																}
															});
												}), true, 1, TimeUnit.MINUTES,
												u -> u.getId().equals(author.getId()),
												ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
										));
						});
						return;
					} else {
						channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
						return;
					}

					if (pinned > 1)
						msg += " (" + pinned + " mensagens ignoradas)";
					else if (pinned == 1)
						msg += " (" + pinned + " mensagem ignorada)";

					channel.purgeMessages(msgs);
					channel.sendMessage(msg).queue();
				});
	}
}
