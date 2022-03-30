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
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Debuff;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.BountyDifficulty;
import com.kuuhaku.model.enums.Danger;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.BountyQuest;
import com.kuuhaku.model.records.BountyInfo;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.kuuhaku.model.enums.BountyDifficulty.*;

@Command(
		name = "muraldemissoes",
		aliases = {"bountyboard", "bb"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class BountyBoardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.isUnavailable()) {
			channel.sendMessage("❌ | Este herói está em uma missão.").queue();
			return;
		}

		Calendar cal = Calendar.getInstance();
		long seed = StringHelper.stringToLong(author.getId() + h.getId() + cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR)) + h.getSeed();

		List<BountyQuest> pool = BountyQuest.queryAll(BountyQuest.class, "SELECT b FROM BountyQuest b WHERE b.difficulty IN :allowed", Set.of(VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD));
		pool = CollectionHelper.getRandomN(pool, 3, 1, seed);

		List<BountyQuest> training = BountyQuest.queryAll(BountyQuest.class, "SELECT b FROM BountyQuest b WHERE b.difficulty = 'NONE'");
		pool.add(CollectionHelper.getRandomEntry(training));

		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		for (int i = 0; i < pool.size(); i++) {
			BountyQuest q = pool.get(i);
			BountyInfo info = q.getInfo(h, seed);

			EmbedBuilder eb = new EmbedBuilder()
					.setTitle("Informações da missão \"" + q + "\"")
					.setDescription(q.getDescription());

			XStringBuilder sb = new XStringBuilder();
			Integer[] stats = info.reqStats().getStats();
			String[] capt = {"STR: ", "RES: ", "AGI: ", "WIS: ", "CON: "};
			for (int j = 0; j < stats.length; j++) {
				Integer stat = stats[j];

				if (stat > 0) {
					sb.appendNewLine(capt[j] + stat);
				}
			}

			if (!sb.isBlank()) {
				eb.addField("Atributos recomendados", sb.toString(), true);
			}

			sb.clear();
			for (Map.Entry<Reward, Integer> entry : info.rewards().entrySet()) {
				Reward rew = entry.getKey();
				int val = entry.getValue();
				if (val == 0) continue;

				sb.appendNewLine(rew).append(" | ").append(rew.apply(null, val));
			}

			if (!sb.isBlank()) {
				eb.addField("Recompensas", sb.toString(), true);
			}

			sb.clear();
			Set<Danger> dangers = q.getDangers();
			for (Danger danger : dangers) {
				sb.appendNewLine(danger.toString());
			}

			Set<Debuff> debuffs = q.getDebuffs();
			if (!debuffs.isEmpty()) {
				sb.appendNewLine("**Maldições:**");
				for (Debuff debuff : debuffs) {
					sb.appendNewLine("- " + debuff.getName());
				}
			}

			if (!sb.isBlank()) {
				eb.addField("Possíveis perigos", sb.toString(), true);
			}

			buttons.put(StringHelper.parseEmoji(StringHelper.getFancyNumber(i + 1)), wrapper -> {
				if (h.getEnergy() < 1) {
					channel.sendMessage("❌ | Seu herói está cansado (sem energia suficiente).").queue();
					return;
				}

				Main.getInfo().getConfirmationPending().put(h.getUid(), true);
				channel.sendMessage("Deseja aceitar a missão \"" + q + "\"? (Duração: " + StringHelper.toStringDuration(TimeUnit.MILLISECONDS.convert(info.time(), TimeUnit.MINUTES)) + ")")
						.setEmbeds(eb.build())
						.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), w -> {
									Main.getInfo().getConfirmationPending().remove(author.getId());

									h.setQuest(q, seed);
									KawaiponDAO.saveHero(h);

									s.delete()
											.flatMap(d -> wrapper.getMessage().delete())
											.flatMap(d -> channel.sendMessage("✅ | Herói enviado com sucesso!"))
											.queue();
								}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(h.getUid()),
								m -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			});
		}

		channel.sendMessageEmbeds(getEmbed(h, pool, seed)).queue(s ->
				Pages.buttonize(s, buttons, Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private MessageEmbed getEmbed(Hero h, List<BountyQuest> pool, long seed) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Missões disponíveis")
				.setDescription("Para ganhar XP (e outras coisas) você pode enviar seu herói em uma missão, consumindo 1 EP. Mas cuidado, existem riscos na jornada, e dependendo da dificuldade da missão pode resultar até na morte. Rotaciona a cada dia.");

		for (int i = 0; i < pool.size(); i++) {
			BountyQuest q = pool.get(i);
			BountyInfo info = q.getInfo(h, seed);

			int diff = q.getDifficulty().getValue();
			double modDiff = MathHelper.prcnt(diff - info.diff(), diff);

			eb.addField(StringHelper.getFancyNumber(i + 1) + " | " + q
					, "%s\n\nDificuldade: %s (Sucesso: %s%%) | Duração: %s".formatted(
							q.getDescription(),
							BountyDifficulty.valueOf(10 - 10 * modDiff),
							info.diff() == 0 ? "100" : MathHelper.roundToString(100 * modDiff, 1),
							StringHelper.toStringDuration(TimeUnit.MILLISECONDS.convert(info.time(), TimeUnit.MINUTES))
					), false
			);
		}

		return eb.build();
	}
}
