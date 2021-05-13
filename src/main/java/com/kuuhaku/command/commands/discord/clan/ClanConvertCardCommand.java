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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cconverter",
		aliases = {"cconvert"},
		usage = "req_card",
		category = Category.CLAN
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class ClanConvertCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan cl = ClanDAO.getUserClan(author.getId());
		if (cl == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (cl.isLocked(author.getId(), ClanPermission.DECK)) {
			channel.sendMessage("❌ | Você não tem permissão para alterar o deck do clã.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Deck dk = kp.getDeck();

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa digitar o nome da carta kawaipon que quer converter para carta senshi.").queue();
			return;
		}

		CardType ct = CardDAO.identifyType(args[0]);
		Drawable tc = switch (ct) {
			case KAWAIPON -> CardDAO.getChampion(args[0]);
			case EVOGEAR -> CardDAO.getEquipment(args[0]);
			case FIELD -> CardDAO.getField(args[0]);
			case NONE -> null;
		};
		if (tc == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
			return;
		}

		switch (ct) {
			case KAWAIPON -> {
				KawaiponCard kc = new KawaiponCard(tc.getCard(), false);
				Champion c = (Champion) tc;
				if (!kp.getCards().contains(kc)) {
					channel.sendMessage("❌ | Você não possui essa carta.").queue();
					return;
				}

				if (cl.getDeck().checkChampion(c, channel)) return;

				c.setAcc(acc);
				EmbedBuilder eb = new ColorlessEmbedBuilder();
				eb.setTitle("Por favor confirme!");
				eb.setDescription("Sua carta kawaipon " + tc.getCard().getName() + " será convertida para carta senshi e será adicionada ao deck do seu clã, por favor clique no botão abaixo para confirmar a conversão.");
				eb.setImage("attachment://card.png");

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(c.drawCard(false), "kp_" + c.getId(), "png"), "card.png")
						.queue(s ->
								Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
											Main.getInfo().getConfirmationPending().remove(author.getId());
											kp.removeCard(kc);
											cl.getDeck().addChampion(c);
											KawaiponDAO.saveKawaipon(kp);
											cl.getTransactions().add(author.getAsTag() + " adicionou a carta " + tc.getCard().getName() + " ao deck");
											ClanDAO.saveClan(cl);
											s.delete().queue();
											channel.sendMessage("✅ | Conversão realizada com sucesso!").queue();
										}), true, 1, TimeUnit.MINUTES,
										u -> u.getId().equals(author.getId()),
										ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
								)
						);
			}
			case EVOGEAR -> {
				Equipment e = (Equipment) tc;
				if (dk.getEquipment(tc.getCard()) == null) {
					channel.sendMessage("❌ | Você não possui esse equipamento.").queue();
					return;
				}

				if (cl.getDeck().checkEquipment(e, channel)) return;

				e.setAcc(acc);
				EmbedBuilder eb = new ColorlessEmbedBuilder();
				eb.setTitle("Por favor confirme!");
				eb.setDescription("Sua carta evogear " + tc.getCard().getName() + " será transferida ao deck do seu clã, por favor clique no botão abaixo para confirmar.");
				eb.setImage("attachment://card.png");

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(e.drawCard(false), "kp_" + e.getCard().getId(), "png"), "card.png")
						.queue(s ->
								Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
											Main.getInfo().getConfirmationPending().remove(author.getId());
											dk.removeEquipment(e);
											cl.getDeck().addEquipment(e);
											KawaiponDAO.saveKawaipon(kp);
											cl.getTransactions().add(author.getAsTag() + " adicionou a carta " + tc.getCard().getName() + " ao deck");
											ClanDAO.saveClan(cl);
											s.delete().queue();
											channel.sendMessage("✅ | Transferência realizada com sucesso!").queue();
										}), true, 1, TimeUnit.MINUTES,
										u -> u.getId().equals(author.getId()),
										ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
								)
						);
			}
			case FIELD -> {
				Field f = (Field) tc;
				if (dk.getField(tc.getCard()) == null) {
					channel.sendMessage("❌ | Você não possui esse campo.").queue();
					return;
				}

				if (cl.getDeck().checkField(f, channel)) return;

				f.setAcc(acc);
				EmbedBuilder eb = new ColorlessEmbedBuilder();
				eb.setTitle("Por favor confirme!");
				eb.setDescription("Sua carta de campo " + tc.getCard().getName() + " será transferida ao deck do seu clã, por favor clique no botão abaixo para confirmar.");
				eb.setImage("attachment://card.png");

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(f.drawCard(false), "kp_" + f.getCard().getId(), "png"), "card.png")
						.queue(s ->
								Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
											Main.getInfo().getConfirmationPending().remove(author.getId());
											dk.removeField(f);
											cl.getDeck().addField(f);
											KawaiponDAO.saveKawaipon(kp);
											cl.getTransactions().add(author.getAsTag() + " adicionou a carta " + tc.getCard().getName() + " ao deck");
											ClanDAO.saveClan(cl);
											s.delete().queue();
											channel.sendMessage("✅ | Transferência realizada com sucesso!").queue();
										}), true, 1, TimeUnit.MINUTES,
										u -> u.getId().equals(author.getId()),
										ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
								)
						);
			}
		}
	}
}
