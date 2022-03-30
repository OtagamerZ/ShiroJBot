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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.DrawableDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.exceptions.ValidationException;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.LogicHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.image.BufferedImage;

@Command(
		name = "deck",
		usage = "req_daily-meta-p",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("shoukan")
@SlashCommand(name = "deck", args = {
		"{\"name\": \"tipo\", \"description\": \"Tipo de deck a ser exibido (daily/meta)\", \"type\": \"STRING\", \"required\": false}",
		"{\"name\": \"privado\", \"description\": \"Exibe o deck nas mensagens privadas\", \"type\": \"BOOLEAN\", \"required\": false}"
})
public class ShoukanDeckCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		boolean showPrivate = LogicHelper.equalsAny("p", args);

		channel.sendMessage(I18n.getString("str_generating-deck")).queue(m -> {
			if (LogicHelper.containsAny(args, "daily", "diario")) {
				Deck dk = MiscHelper.getDailyDeck();

				ShoukanDeck kb = new ShoukanDeck(Account.find(Account.class, author.getId()));
				BufferedImage cards = kb.view(dk);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle(":date: | Deck diário")
						.setDescription("O deck diário será o mesmo para todos os jogadores até amanhã, e permite que usuários que não possuam 30 cartas Senshi joguem.")
						.setImage("attachment://deck.jpg");

				if (showPrivate) {
					author.openPrivateChannel()
							.flatMap(c -> c.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(cards, "deck", "jpg")))
							.flatMap(c -> channel.sendMessage("Deck enviado nas suas mensagens privadas."))
							.flatMap(c -> m.delete())
							.queue(null, MiscHelper::doNothing);
				} else {
					channel.sendMessageEmbeds(eb.build())
							.addFile(ImageHelper.writeAndGet(cards, "deck", "jpg"))
							.flatMap(c -> m.delete())
							.queue();
				}
			} else if (LogicHelper.containsAny(args, "meta")) {
				Deck dk = DrawableDAO.getMetaDeck();

				ShoukanDeck kb = new ShoukanDeck(Account.find(Account.class, author.getId()));
				BufferedImage cards = kb.view(dk);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle(":date: | Deck meta")
						.setDescription("O deck meta reflete as cartas mais utilizadas pela comunidade (não necessáriamente sendo a melhor combinação possível).")
						.setImage("attachment://deck.jpg");

				if (showPrivate) {
					author.openPrivateChannel()
							.flatMap(c -> c.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(cards, "deck", "jpg")))
							.flatMap(c -> channel.sendMessage("Deck enviado nas suas mensagens privadas."))
							.flatMap(c -> m.delete())
							.queue(null, MiscHelper::doNothing);
				} else {
					channel.sendMessageEmbeds(eb.build())
							.addFile(ImageHelper.writeAndGet(cards, "deck", "jpg"))
							.flatMap(c -> m.delete())
							.queue();
				}
			} else {
				Deck dk = KawaiponDAO.getDeck(author.getId());

				ShoukanDeck kb = new ShoukanDeck(Account.find(Account.class, author.getId()));
				BufferedImage cards = kb.view(dk);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle(":beginner: | Deck de " + author.getName() + (dk.isNovice() ? " (INICIANTE)" : ""))
						.addField(":crossed_swords: | Cartas Senshi:", dk.getChampions().size() + " de 36", true)
						.addField(":shield: | Peso evogear:", dk.getEvoWeight() + " de 24", true)
						.setImage("attachment://deck.jpg");

				if (dk.isNovice()) {
					eb.setFooter("""
							O deck de iniciante dura 1 mês após completar o tutorial, nele você pode adicionar qualquer campeão que quiser mesmo que não tenha (não afeta as cartas obtidas) para ir testando as combinações.
							Ao expirar, equipamentos e campos serão transferidos para seu armazém automaticamente.
							""");
				}

				if (showPrivate) {
					author.openPrivateChannel()
							.flatMap(c -> c.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(cards, "deck", "jpg")))
							.flatMap(c -> channel.sendMessage("Deck enviado nas suas mensagens privadas."))
							.flatMap(c -> m.delete())
							.queue(null, MiscHelper::doNothing);
				} else {
					channel.sendMessageEmbeds(eb.build())
							.addFile(ImageHelper.writeAndGet(cards, "deck", "jpg"))
							.flatMap(c -> m.delete())
							.queue();
				}
			}
		});
	}

	@Override
	public String toCommand(SlashCommandEvent evt) {
		OptionMapping type = evt.getOption("tipo");
		OptionMapping prv = evt.getOption("privado");

		String tp = type == null ? "" : type.getAsString();
		if (!LogicHelper.equalsAny(tp, "daily", "meta"))
			throw new ValidationException("❌ | O tipo deve ser `daily` ou `meta`.");

		return tp + (prv == null ? "" : (prv.getAsBoolean() ? "p" : ""));
	}
}