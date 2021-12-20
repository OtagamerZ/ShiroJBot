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
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TradeDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Trade;
import com.kuuhaku.model.persistent.TradeOffer;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Command(
		name = "trocar",
		aliases = {"trade"},
		usage = "req_mention",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EMBED_LINKS
})
public class TradeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (true) return;
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		User tgt = message.getMentionedUsers().get(0);
		Trade t = TradeDAO.getTrade(author.getId());
		if (t != null) {
			String other = t.getLeft().getUid().equals(author.getId()) ? t.getRight().getUid() : t.getLeft().getUid();
			String name = Helper.getUsername(other);
			if (!tgt.getId().equals(other)) {
				channel.sendMessage("❌ | Você possui uma troca pendente com " + Helper.unmention(name) + ".").queue();
				return;
			}
		}

		channel.sendMessage(tgt.getAsMention() + ", " + author.getAsMention() + " deseja" + (t == null ? "" : " continuar a") + " negociar com você, deseja aceitar?").queue(s ->
				Pages.buttonize(s, Collections.singletonMap(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							if (!wrapper.getUser().getId().equals(tgt.getId())) return;
							s.delete().queue(null, Helper::doNothing);
							Trade trade = Helper.getOr(t, new Trade(
									new TradeOffer(author.getId()),
									new TradeOffer(tgt.getId())
							));

							EmbedBuilder eb = new ColorlessEmbedBuilder()
									.setTitle("Comércio entre " + author.getName() + " e " + tgt.getName())
									.setDescription("Para adicionar/remover uma oferta digite `+/- nome_da_carta [C]`, para adicionar/remover uma quantia de CR digite `+/- valor`.")
									.addField(author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
									.addField(tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
									.setFooter("%s: %s CR\n%s: %s CR".formatted(
											author.getName(),
											Helper.separate(trade.getLeft().getAccount().getBalance()),
											tgt.getName(),
											Helper.separate(trade.getRight().getAccount().getBalance())
									));

							sendTradeWindow(author, channel, guild, tgt, trade, eb);
						}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
						ms -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							Main.getInfo().getConfirmationPending().remove(tgt.getId());
						}
				)
		);
	}

	private void sendTradeWindow(User author, TextChannel channel, Guild guild, User tgt, Trade trade, EmbedBuilder eb) {
		channel.sendMessageEmbeds(eb.build()).queue(msg -> {
			SimpleMessageListener sml = new SimpleMessageListener() {
				@Override
				public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
					User usr = event.getAuthor();
					String content = event.getMessage().getContentRaw();

					if (Helper.equalsAny(usr.getId(), author.getId(), tgt.getId()) && (content.startsWith("+") || content.startsWith("-"))) {
						boolean add = content.startsWith("+");
						String[] rawOffer = content.replaceFirst("[+\\-]", "").trim().split(" ");
						if (rawOffer.length < 1) return;

						boolean foil = rawOffer.length > 1 && rawOffer[1].equalsIgnoreCase("C");
						String offer = rawOffer[0];

						TradeOffer to = trade.getOffer(usr.getId());
						if (to.hasAccepted()) {
							channel.sendMessage("❌ | Você já confirmou sua oferta.").queue();
							return;
						}

						if (StringUtils.isNumeric(offer)) {
							try {
								int c = Integer.parseInt(offer);
								if (c < 0) {
									channel.sendMessage("❌ | O valor deve ser maior ou igual a 0.").queue();
									return;
								}

								if (add) {
									if (to.getAccount().getBalance() < to.getValue() + c) {
										channel.sendMessage("❌ | Você não possui CR suficientes.").queue();
										return;
									}

									to.addValue(c);
								} else {
									if (to.getValue() - c < 0) {
										channel.sendMessage("❌ | Você não pode reduzir o CR oferecido para menos que 0.").queue();
										return;
									}

									to.removeValue(c);
								}
							} catch (NumberFormatException e) {
								channel.sendMessage("❌ | O valor máximo é " + Helper.separate(Integer.MAX_VALUE) + " CR!").queue();
								return;
							}
						} else {

						}

						for (TradeOffer of : trade.getOffers()) {
							of.setAccepted(false);
						}
						eb.clearFields()
								.addField(author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
								.addField(tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
								.setFooter("%s: %s CR\n%s: %s CR".formatted(
										author.getName(),
										Helper.separate(trade.getLeft().getAccount().getBalance()),
										tgt.getName(),
										Helper.separate(trade.getRight().getAccount().getBalance())
								));

						msg.editMessageEmbeds(eb.build()).queue(null, t -> {
							msg.delete().queue(null, Helper::doNothing);
							sendTradeWindow(author, channel, guild, tgt, trade, eb);
						});

						event.getMessage().delete().queue(null, Helper::doNothing);
					}
				}
			};

			Pages.buttonize(msg, Collections.singletonMap(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
						if (trade.getOffers().stream().anyMatch(o -> o.getUid().equals(wrapper.getUser().getId()))) {
							trade.getOffer(wrapper.getUser().getId()).setAccepted(true);

							if (trade.getOffers().stream().allMatch(TradeOffer::hasAccepted)) {

							}

							eb.clearFields()
									.addField((trade.getLeft().hasAccepted() ? "(CONFIRMADO) " : "") + author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
									.addField((trade.getRight().hasAccepted() ? "(CONFIRMADO) " : "") + tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
									.setFooter("%s: %s CR\n%s: %s CR".formatted(
											author.getName(),
											Helper.separate(trade.getLeft().getAccount().getBalance()),
											tgt.getName(),
											Helper.separate(trade.getRight().getAccount().getBalance())
									));

							msg.editMessageEmbeds(eb.build()).queue(null, t -> {
								msg.delete().queue(null, Helper::doNothing);
								sendTradeWindow(author, channel, guild, tgt, trade, eb);
							});
						}
					}), ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
					u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
					_ms -> {
						msg.editMessage("Transação cancelada.")
								.flatMap(m -> m.suppressEmbeds(true))
								.queue(null, Helper::doNothing);
						sml.close();
						Main.getInfo().getConfirmationPending().remove(author.getId());
						Main.getInfo().getConfirmationPending().remove(tgt.getId());
					}
			);

			ShiroInfo.getShiroEvents().addHandler(guild, sml);
		});
	}
}
