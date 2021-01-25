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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public class ShoukanMasterCommand extends Command {

	public ShoukanMasterCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ShoukanMasterCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ShoukanMasterCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ShoukanMasterCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());

		if (mmr.getMaster().equals("none")) {
			channel.sendMessage("❌ | Você não pode mais definir um tutor por já ter jogado 1 partida.").queue();
			return;
		} else if (!mmr.getMaster().isBlank()) {
			channel.sendMessage("❌ | Você já possui um tutor.").queue();
			return;
		} else if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage("❌ | Você precisa mencionar um usuário para tornar-se seu tutor.").queue();
			return;
		}

		User u = message.getMentionedUsers().get(0);
		String hash1 = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash1);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prestes a definir " + u.getName() + " como seu tutor, ao alcançar seu primeiro ranking de Shoukan você receberá 5 sínteses gratúitas. Deseja confirmar?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (mb.getId().equals(author.getId())) {
								if (!ShiroInfo.getHashes().remove(hash1)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());

								String hash2 = Helper.generateHash(guild, u);
								ShiroInfo.getHashes().add(hash2);
								Main.getInfo().getConfirmationPending().put(u.getId(), true);
								s.delete().flatMap(d ->
										channel.sendMessage(u.getAsMention() + ", " + author.getName() + " deseja tornar-se seu discípulo de Shoukan, você receberá 25.000 créditos caso ele(a) alcance o ranking de Aprendiz IV. Deseja aceitar?")
								).queue(s2 -> Pages.buttonize(s2, Map.of(Helper.ACCEPT, (mb2, ms2) -> {
											if (mb.getId().equals(u.getId())) {
												if (!ShiroInfo.getHashes().remove(hash1)) return;
												Main.getInfo().getConfirmationPending().invalidate(u.getId());

												mmr.setMaster(u.getId());
												MatchMakingRatingDAO.saveMMR(mmr);

												s.delete().flatMap(d -> channel.sendMessage("✅ | Contrato feito com sucesso!")).queue();
											}
										}), true, 1, TimeUnit.MINUTES,
										usr -> u.getId().equals(u.getId()),
										msg -> {
											ShiroInfo.getHashes().remove(hash2);
											Main.getInfo().getConfirmationPending().invalidate(u.getId());
										})
								);
							}
						}), true, 1, TimeUnit.MINUTES,
						usr -> u.getId().equals(author.getId()),
						msg -> {
							ShiroInfo.getHashes().remove(hash1);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
