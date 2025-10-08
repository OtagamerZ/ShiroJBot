/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.fun;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Command(
		name = "dunhun",
		category = Category.STAFF
)
@Syntax(
		allowEmpty = true,
		patterns = @SigPattern(id = "users", value = "(<@!?(\\d+)>(?=\\s|$))+"),
		value = {
				"<dungeon:word:r> <users:custom:r>[users]",
				"<dungeon:word:r>"
		}
)
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class DunhunCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero(locale);
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		if (!args.has("dungeon")) {
			List<Dungeon> dgs = DAO.queryAll(Dungeon.class, "SELECT d FROM Dungeon d ORDER BY d.areaLevel, d.id");
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/dungeons"))
					.setImage("attachment://image.png");

			List<Page> pages = new ArrayList<>();
			for (Dungeon dg : dgs) {
				String name = dg.getInfo(locale).getName() + " (`" + dg.getId() + "`)";
				if (h.hasCompleted(dg)) {
					name += " - " + Constants.ACCEPT;
				}

				eb.clearFields()
						.setTitle(name);

				if (h.canEnter(dg)) {
					eb.setDescription(dg.getInfo(locale).getDescription());
				} else {
					List<String> rem = h.remainingDungeonsFor(dg).stream()
							.map(id -> DAO.find(Dungeon.class, id))
							.map(dun -> dun.getInfo(locale).getName())
							.toList();

					eb.setDescription(locale.get("str/dungeon_requirement", Utils.properlyJoin(locale, rem)));
				}

				if (!dg.getMonsterPool().isEmpty()) {
					List<String> mobs = DAO.queryAllNative(String.class, "SELECT regexp_replace(name, '\\[.+]', '') FROM monster_info WHERE locale = ?1 AND id IN ?2",
							locale.name(), dg.getMonsterPool()
					);
					eb.addField(locale.get("str/monster_pool"), Utils.properlyJoin(locale, mobs), true);
				} else {
					eb.addField(locale.get("str/monster_pool"), locale.get("str/unknown"), true);
				}

				if (dg.getAreaLevel() > 0) {
					eb.addField(locale.get("str/area_level"), String.valueOf(dg.getAreaLevel()), true);
				} else {
					eb.addField(locale.get("str/area_level"), locale.get("str/unknown"), true);
				}

				pages.add(InteractPage.of(eb.build()));
			}

			AtomicInteger i = new AtomicInteger();
			ButtonizeHelper helper = new ButtonizeHelper(true)
					.setTimeout(1, TimeUnit.MINUTES)
					.setCanInteract(event.user()::equals)
					.addAction(Utils.parseEmoji("◀️"), w -> {
						if (i.get() > 0) {
							setDungeonEmbed(dgs, pages, w, i.decrementAndGet());
						}
					})
					.addAction(Utils.parseEmoji("▶️"), w -> {
						if (i.get() < pages.size() - 1) {
							setDungeonEmbed(dgs, pages, w, i.incrementAndGet());
						}
					});

			MessageCreateAction act = Utils.sendPage(event.channel(), pages.getFirst());

			File img = new File(Constants.CARDS_ROOT + "../dungeons/" + dgs.getFirst().getId() + ".png");
			if (img.exists()) {
				act.setFiles(FileUpload.fromData(img, "image.png"));
			}

			helper.apply(act).queue(s -> Pages.buttonize(s, helper));
			return;
		}

		if (GameInstance.PLAYERS.containsKey(event.user().getId())) {
			event.channel().sendMessage(locale.get("error/in_game_self")).queue();
			return;
		}

		List<User> others = event.message().getMentions().getUsers();
		if (others.contains(event.user())) {
			event.channel().sendMessage(locale.get("error/cannot_play_with_self")).queue();
			return;
		} else if (others.size() > 3) {
			event.channel().sendMessage(locale.get("error/many_players", 4)).queue();
			return;
		}

		for (User other : others) {
			if (GameInstance.PLAYERS.containsKey(other.getId())) {
				event.channel().sendMessage(locale.get("error/in_game_target", other.getEffectiveName())).queue();
				return;
			}
		}

		Dungeon dungeon = DAO.find(Dungeon.class, args.getString("dungeon").toUpperCase());
		if (dungeon == null) {
			String sug = Utils.didYouMean(args.getString("dungeon"), "SELECT id AS value FROM dungeon");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_dungeon_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_dungeon", sug)).queue();
			}
			return;
		}

		Set<User> pending = new HashSet<>(others);
		try {
			if (others.isEmpty()) {
				try {
					Dunhun dun = new Dunhun(locale, dungeon, event.user());
					dun.start(event.guild(), event.channel())
							.whenComplete((v, e) -> {
								if (e instanceof GameReport rep && rep.getCode() == GameReport.INITIALIZATION_ERROR) {
									Constants.LOGGER.error(e, e);
									event.channel().sendMessage(locale.get("error/error", e)).queue();
								}
							});
				} catch (GameReport e) {
					switch (e.getCode()) {
						case GameReport.NO_HERO -> event.channel().sendMessage(locale.get("error/no_hero")).queue();
						case GameReport.OVERBURDENED ->
								event.channel().sendMessage(locale.get("error/overburdened", e.getContent())).queue();
						case GameReport.UNDERLEVELLED ->
								event.channel().sendMessage(locale.get("error/underlevelled", e.getContent())).queue();
						case GameReport.INVALID_DUEL ->
								event.channel().sendMessage(locale.get("error/invalid_duel")).queue();
					}
				}

				return;
			}

			Utils.confirm(locale.get("question/dunhun",
							Utils.properlyJoin(locale, others.stream().map(User::getAsMention).toList()),
							event.user().getAsMention(),
							dungeon.getInfo(locale).getName()
					), event.channel(), w -> {
						if (pending.remove(w.getUser())) {
							event.channel().sendMessage(locale.get("str/match_accept", w.getUser().getEffectiveName())).queue();

							if (!pending.isEmpty()) return false;
						} else {
							return false;
						}

						try {
							Dunhun dun = new Dunhun(locale, dungeon,
									Stream.concat(Stream.of(event.user()), others.stream())
											.map(User::getId)
											.toArray(String[]::new)
							);
							dun.start(event.guild(), event.channel())
									.whenComplete((v, e) -> {
										if (e instanceof GameReport rep && rep.getCode() == GameReport.INITIALIZATION_ERROR) {
											Constants.LOGGER.error(e, e);
											event.channel().sendMessage(locale.get("error/error", e)).queue();
										}
									});
						} catch (GameReport e) {
							switch (e.getCode()) {
								case GameReport.NO_HERO -> {
									if (e.getContent().equals(event.user().getId())) {
										event.channel().sendMessage(locale.get("error/no_hero")).queue();
									} else {
										event.channel().sendMessage(locale.get("error/no_hero_target", "<@" + e.getContent() + ">")).queue();
									}
								}
								case GameReport.OVERBURDENED ->
										event.channel().sendMessage(locale.get("error/overburdened", e.getContent())).queue();
								case GameReport.UNDERLEVELLED ->
										event.channel().sendMessage(locale.get("error/underlevelled", e.getContent())).queue();
							}
						}

						return true;
					}, others.toArray(User[]::new)
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}

	private void setDungeonEmbed(List<Dungeon> dgs, List<Page> pages, ButtonWrapper w, int it) {
		MessageEditAction act = w.getMessage()
				.editMessageEmbeds(Utils.getEmbeds(pages.get(it)));

		File img = new File(Constants.CARDS_ROOT + "../dungeons/" + dgs.get(it).getId() + ".png");
		if (img.exists()) {
			act.setFiles(FileUpload.fromData(img, "image.png"));
		}

		act.queue();
	}
}
