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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.FileHelper;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Command(
		name = "carta",
		aliases = {"card", "see", "olhar"},
		usage = "req_card-type",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES})
public class SeeCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma carta.").queue();
			return;
		}

		Account acc = Account.find(Account.class, author.getId());
		Kawaipon kp = Kawaipon.find(Kawaipon.class, author.getId());
		boolean shoukan = args.length > 1 && args[1].equalsIgnoreCase("S");

		if (shoukan) {
			Champion ch = Champion.getChampion(args[0]);
			Evogear eq = Evogear.getEvogear(args[0]);
			Field f = Field.getField(args[0]);

			if (ch == null && eq == null && f == null) {
				channel.sendMessage("❌ | Esse campeão, equipamento ou campo não existe, você não quis dizer `" + StringHelper.didYouMean(args[0], Stream.of(Champion.getChampions(), Evogear.getEvogears(), Field.getFields()).flatMap(Collection::stream).map(d -> d.getCard().getId()).toList()) + "`?").queue();
				return;
			}

			Drawable d = ch == null ? eq == null ? f : eq : ch;
			d.setAcc(acc);

			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle((ch == null ? ":shield:" : ":crossed_swords:") + " | " + d.getCard().getName());
			if (d instanceof Champion c) {
				eb.addField("Classe:", c.getCategory() == null ? "Nenhuma" : c.getCategory().getName(), true);
			} else if (d instanceof Evogear e && !e.getCharms().isEmpty()) {
				List<Charm> charms = e.getCharms();
				for (Charm c : charms) {
					eb.addField("Amuleto: " + c.getName(), c.getDescription(e.getTier()), true);
				}
			}
			eb.setImage("attachment://kawaipon.png");

			channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(d.drawCard(false), "s_" + d.getCard().getId(), "png"), "kawaipon.png").queue();
		} else {
			Card tc = CardDAO.getCard(args[0], true);
			if (tc == null) {
				channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + StringHelper.didYouMean(args[0], Card.getCards().stream().map(Card::getId).toList()) + "`?").queue();
				return;
			}

			boolean foil = args.length > 1 && tc.getRarity() != KawaiponRarity.ULTIMATE && args[1].equalsIgnoreCase("C");
			KawaiponCard card = new KawaiponCard(tc, foil);

			Set<KawaiponCard> cards = kp.getCards();
			List<AddedAnime> animes = AddedAnime.queryAll(AddedAnime.class, "SELECT a FROM AddedAnime a WHERE a.hidden = FALSE");
			for (AddedAnime anime : animes) {
				if (acc.getCompletion(anime).any()) {
					cards.add(new KawaiponCard(Card.find(Card.class, anime.getName()), false));
				}
			}

			Champion c = Champion.getChampion(card.getCard().getId());
			EmbedBuilder eb = new EmbedBuilder()
					.setTitle((foil ? ":star2:" : ":flower_playing_cards:") + " | " + card.getName())
					.setColor(RarityColors.find(RarityColors.class, tc.getRarity()).getPrimary())
					.addField("Obtida:", cards.contains(card) ? "Sim" : "Não", true)
					.addField("Elegível:", c != null && !c.isFusion() ? (CollectionHelper.getOr(c.getRawEffect(), "").contains("//TODO") ? "Ainda não" : "Sim") : "Não", true)
					.addField("Raridade:", tc.getRarity().toString(), true)
					.addField("Tipo:", tc.getRarity() == KawaiponRarity.ULTIMATE ? "Única" : (card.isFoil() ? "Cromada" : "Normal"), true)
					.addField("Anime:", tc.getAnime().toString(), true)
					.setImage("attachment://kawaipon." + (cards.contains(card) ? "png" : "jpg"));

			BufferedImage bi = FileHelper.getResourceAsImage(this.getClass(), "kawaipon/missing.jpg");

			if (cards.contains(card)) {
				if (tc.getRarity() == KawaiponRarity.ULTIMATE) {
					channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(tc.drawUltimate(author.getId()), "kp_" + tc.getId(), "png"), "kawaipon.png").queue();
				} else {
					channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(tc.drawCard(foil), "kp_" + tc.getId(), "png"), "kawaipon.png").queue();
				}
			} else {
				channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(bi, "unknown", "jpg"), "kawaipon.jpg").queue();
			}
		}
	}
}