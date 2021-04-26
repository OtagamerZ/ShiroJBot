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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "limpardeck",
		aliases = {"ldeck", "cleardeck", "cdeck"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class ClearDeckCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		ShoukanDeck sd = new ShoukanDeck(AccountDAO.getAccount(author.getId()));

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setTitle("Por favor confirme!");
		eb.setDescription("Seu deck será limpo e todas as cartas nele serão colocadas na loja (oculto para os outros), por favor clique no botão abaixo para confirmar.");
		eb.setImage("attachment://deque.jpg");

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-deck")).queue(m -> {
			try {
				File f = Helper.writeAndGet(sd.view(kp));
				if (f == null) throw new IOException();
				channel.sendMessage(eb.build()).addFile(f)
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
									Main.getInfo().getConfirmationPending().remove(author.getId());

									Kawaipon fkp = KawaiponDAO.getKawaipon(author.getId());

									for (Champion c : fkp.getChampions()) {
										CardMarket cm = new CardMarket(
												author.getId(),
												new KawaiponCard(c.getCard(), false),
												9999999
										);
										CardMarketDAO.saveCard(cm);
									}
									fkp.getChampions().clear();

									for (Equipment e : fkp.getEquipments()) {
										EquipmentMarket em = new EquipmentMarket(
												author.getId(),
												e,
												9999999
										);
										EquipmentMarketDAO.saveCard(em);
									}
									fkp.getEquipments().clear();

									for (Field fd : fkp.getFields()) {
										FieldMarket fm = new FieldMarket(
												author.getId(),
												fd,
												9999999
										);
										FieldMarketDAO.saveCard(fm);
									}
									fkp.getFields().clear();

									KawaiponDAO.saveKawaipon(fkp);
									s.delete().queue();
									channel.sendMessage("✅ | Deck limpo com sucesso!").queue();
								}), true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(author.getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
				m.delete().queue();
			} catch (IOException e) {
				m.editMessage("❌ | Erro ao gerar o deck.").queue();
				Main.getInfo().getConfirmationPending().remove(author.getId());
			}
		});
	}
}
