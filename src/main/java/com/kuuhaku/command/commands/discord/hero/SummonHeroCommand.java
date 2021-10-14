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

package com.kuuhaku.command.commands.discord.hero;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang.WordUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Command(
		name = "invocarheroi",
		aliases = {"summonhero", "isekai"},
		usage = "req_race-name-image",
		category = Category.MISC
)
public class SummonHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Hero h = CardDAO.getHero(author.getId());

		if (h != null && h.getLevel() < 10) {
			channel.sendMessage("❌ | Você só pode invocar outro herói após ele alcançar o nível 10.").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar uma raça e um nome para seu novo herói.").queue();
			return;
		} else if (message.getAttachments().isEmpty()) {
			channel.sendMessage("❌ | Você precisa enviar uma imagem.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		if (h == null && acc.getGems() < 5) {
			channel.sendMessage("❌ | Você não possui gemas suficientes para completar o feitiço de invocação.").queue();
			return;
		}

		Race r = Race.getByName(args[0]);
		if (r == null) {
			channel.sendMessage("❌ | Raça inválida.").queue();
			return;
		}

		String name = WordUtils.capitalizeFully(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
		if (name.isBlank()) {
			channel.sendMessage("❌ | Você precisa digitar um nome.").queue();
			return;
		} else if (name.length() > 25) {
			channel.sendMessage("❌ | Nome muito longo, o tamanho máximo é 25 caractéres.").queue();
			return;
		}

		BufferedImage bi = null;
		try {
			Message.Attachment a = message.getAttachments().get(0);
			if (!a.isImage()) {
				channel.sendMessage("❌ | Você precisa enviar uma imagem.").queue();
				return;
			}

			bi = ImageIO.read(a.retrieveInputStream().get());
		} catch (InterruptedException | ExecutionException | IOException e) {
			channel.sendMessage("❌ | Imagem inválida.").queue();
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.addField("Atributos iniciais de " + r.toString().toLowerCase(Locale.ROOT) + ":", """
						STR: %s
						RES: %s
						AGI: %s
						WIS: %s
						CON: %s
						""".formatted((Object[]) r.getStartingStats()), false);

		BufferedImage image = bi;
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		if (h != null) {
			channel.sendMessage("Você está prestes a invocar " + name + ", campeão da raça " + r.toString().toLowerCase(Locale.ROOT) + " e enviar " + h.getName() + " de volta ao seu mundo de origem, deseja confirmar?")
					.setEmbeds(eb.build())
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
								CardDAO.saveHero(new Hero(author, name, r, image));

								int gems = h.getLevel() - 10;
								acc.addGem(gems);
								AccountDAO.saveAccount(acc);

								Main.getInfo().getConfirmationPending().remove(author.getId());
								s.delete().flatMap(d -> channel.sendMessage("✅ | Herói invocado com sucesso, você recebeu " + gems + " de " + h.getName() + " antes de retornar!")).queue();
							}), true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
		} else {
			channel.sendMessage("Você está prestes a invocar " + name + ", campeão da raça " + r.toString().toLowerCase(Locale.ROOT) + " por 5 gemas, deseja confirmar?")
					.setEmbeds(eb.build())
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
								CardDAO.saveHero(new Hero(author, name, r, image));

								acc.removeGem(5);
								AccountDAO.saveAccount(acc);

								Main.getInfo().getConfirmationPending().remove(author.getId());
								s.delete().flatMap(d -> channel.sendMessage("✅ | Herói invocado com sucesso!")).queue();
							}), true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
		}
	}
}
