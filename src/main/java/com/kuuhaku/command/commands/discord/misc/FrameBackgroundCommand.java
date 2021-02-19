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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "fundododeck",
		aliases = {"deckbg"},
		usage = "req_ultimate",
		category = Category.MISC
)
public class FrameBackgroundCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa especificar um anime com coleção completa para definir como imagem de fundo de seu deck Shoukan.").queue();
			return;
		}

		AddedAnime anime = CardDAO.verifyAnime(args[0].toUpperCase());
		if (anime == null) {
			channel.sendMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getValidAnime().stream().map(AddedAnime::getName).toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
			return;
		}

		boolean canUse = CardDAO.totalCards(anime.getName()) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime) && !k.isFoil()).count();
		if (!canUse) {
			channel.sendMessage("❌ | Você só pode usar como fundo animes que você já tenha completado a coleção.").queue();
			return;
		}

		acc.setUltimate(anime.getName());
		AccountDAO.saveAccount(acc);
		channel.sendMessage("✅ | Ultimate definida com sucesso!").queue();
	}
}
