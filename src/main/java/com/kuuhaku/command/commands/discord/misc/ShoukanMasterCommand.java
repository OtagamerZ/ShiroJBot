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
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.BiContract;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "tutor",
		aliases = {"teacher", "mestre", "master"},
		usage = "req_mention",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class ShoukanMasterCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());

		if (mmr.getMaster().equals("none")) {
			channel.sendMessage("❌ | Você não pode mais escolher um tutor por já ter terminado sua MD-10.").queue();
			return;
		} else if (!mmr.getMaster().isBlank()) {
			channel.sendMessage("❌ | Você já possui um tutor.").queue();
			return;
		} else if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage("❌ | Você precisa mencionar um usuário para tornar-se seu tutor.").queue();
			return;
		}

		User u = message.getMentionedUsers().get(0);
		MatchMakingRating master = MatchMakingRatingDAO.getMMR(u.getId());
		if (master.getTier() == RankedTier.UNRANKED) {
			channel.sendMessage("❌ | O usuário escolhido precisa possuir ranking no Shoukan.").queue();
			return;
		}

		BiContract<Boolean, Boolean> contract = new BiContract<>((b1, b2) -> {
			mmr.setMaster(u.getId());
			MatchMakingRatingDAO.saveMMR(mmr);

			channel.sendMessage("✅ | Contrato efetuado com sucesso!").queue();
		});

		firstStep(contract, guild, author, u, channel);
	}

	private void firstStep(BiContract<Boolean, Boolean> contract, Guild guild, User author, User target, MessageChannel channel) {
		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prestes a definir " + target.getName() + " como seu tutor, ao alcançar o ranking de Aprendiz no Shoukan você receberá **5 sínteses gratuitas**. Deseja confirmar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (mb.getId().equals(author.getId())) {
								if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());

								s.delete().queue();
								contract.setSignatureA(true);
								secondStep(contract, guild, author, target, channel);
							}
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						msg -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}

	private void secondStep(BiContract<Boolean, Boolean> contract, Guild guild, User author, User target, MessageChannel channel) {
		String hash = Helper.generateHash(guild, target);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(target.getId(), true);
		channel.sendMessage(target.getAsMention() + ", " + author.getName() + " deseja tornar-se seu discípulo de Shoukan, você receberá **30.000 créditos** caso ele(a) alcance o ranking de Aprendiz. Deseja aceitar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (mb.getId().equals(target.getId())) {
								if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(target.getId());

								s.delete().queue();
								contract.setSignatureB(true);
							}
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(target.getId()),
						msg -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(target.getId());
						})
				);
	}
}
