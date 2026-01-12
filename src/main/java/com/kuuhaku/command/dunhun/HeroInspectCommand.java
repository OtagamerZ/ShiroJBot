/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.dunhun;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.EmojiId;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ItemUseException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.model.records.dunhun.GearStats;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Command(
		name = "hero",
		path = "inspect",
		category = Category.STAFF
)
@Syntax("<gear:number:r>")
public class HeroInspectCommand implements Executable {
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

		Gear g = DAO.find(Gear.class, args.getInt("gear"));
		if (g == null) {
			event.channel().sendMessage(locale.get("error/gear_not_found")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setThumbnail("attachment://thumb.png");

		List<GlobalDrop> mats = DAO.queryAll(GlobalDrop.class, """
				SELECT g
				FROM GlobalDrop g
				WHERE g.maxRarity IS NOT NULL
				  AND g.effect IS NOT NULL
				""");
		mats.sort(Comparator
				.<GlobalDrop>comparingInt(gd -> gd.getMaxRarity().ordinal())
				.thenComparing(GlobalDrop::getId)
		);

		AtomicReference<Message> msg = new AtomicReference<>();

		updateEmbed(locale, data.profile().getAccount(), g, eb, event, msg, mats);
	}

	private static void updateEmbed(I18N locale, Account acc, Gear g, EmbedBuilder eb, MessageData.Guild event, AtomicReference<Message> msg, List<GlobalDrop> mats) {
		GearType type = g.getBasetype().getStats().gearType();
		if (g.getRarityClass().ordinal() >= RarityClass.RARE.ordinal()) {
			eb.setTitle(g.getName(locale) + ", " + g.getBasetype().getInfo(locale).getName());
		} else {
			eb.setTitle(g.getName(locale));
		}

		if (g.getUnique() != null) {
			eb.setFooter(g.getUnique().getInfo(locale).getDescription());
		}

		eb.setDescription("");

		JSONArray tags = g.getTags();
		if (!tags.isEmpty()) {
			List<String> tgs = tags.stream()
					.map(t -> LocalizedString.get(locale, "tag/" + t, ""))
					.filter(t -> !t.isEmpty())
					.toList();

			eb.appendDescription("-# " + String.join(", ", tgs) + "\n\n");
		}

		boolean hasStats = false;
		GearStats stats = g.getBasetype().getStats();
		if (g.getDmg() != 0) {
			eb.appendDescription(locale.get("str/bonus_attack", g.getDmg()) + "\n");
			hasStats = true;
		}
		if (g.getDfs() != 0) {
			eb.appendDescription(locale.get("str/bonus_defense", g.getDfs()) + "\n");
			hasStats = true;
		}
		if (g.getCritical() != 0) {
			eb.appendDescription(locale.get("str/bonus_critical", Utils.roundToString(g.getCritical(), 2)) + "\n");
			hasStats = true;
		}

		if (hasStats) {
			eb.appendDescription("\n");
		}

		Attributes reqs = stats.requirements().attributes();
		if (reqs.str() + reqs.dex() + reqs.wis() + reqs.vit() > 0) {
			eb.appendDescription("-# " + locale.get("str/required_attributes") + "\n");
		}

		List<String> attrs = new ArrayList<>();
		if (g.getReqLevel() > 0) attrs.add(locale.get("str/level", g.getReqLevel()));

		for (AttrType t : AttrType.values()) {
			if (t.ordinal() >= AttrType.LVL.ordinal()) break;

			if (reqs.get(t) > 0) attrs.add(t + ": " + reqs.get(t) + " ");
		}

		if (!attrs.isEmpty()) {
			eb.appendDescription(String.join(" | ", attrs) + "\n\n");
		}

		GearAffix imp = g.getImplicit();
		if (imp != null) {
			eb.appendDescription("-# " + locale.get("str/implicit") + "\n");
			eb.appendDescription(imp.getDescription(locale, true) + "\n");
			if (!g.getAffixes().isEmpty()) {
				eb.appendDescription("────────────────\n");
			}
		}

		List<GearAffix> affs = g.getAffixes().stream()
				.sorted(Comparator
						.<GearAffix, Boolean>comparing(ga -> ga.getAffix().getType() == AffixType.SUFFIX, Boolean::compareTo)
						.thenComparing(ga -> ga.getAffix().getId())
				)
				.toList();

		XStringBuilder sb = new XStringBuilder();
		for (GearAffix ga : affs) {
			sb.clear();
			sb.append("-# " + locale.get("str/" + ga.getAffix().getType()));

			if (ga.getAffix().getType() != AffixType.UNIQUE) {
				sb.append(" - " + locale.get("str/tier", ga.getAffix().getTier()));
			}

			String name = ga.getName(locale);
			if (name != null) {
				sb.append(" - " + name);
			}

			JSONArray tgs = ga.getAffix().getTags();
			if (!tgs.isEmpty()) {
				sb.append(" - " + tgs.stream()
						.map(t -> LocalizedString.get(locale, "tag/" + t, ""))
						.collect(Collectors.joining(", "))
				);
			}

			eb.appendDescription(sb + "\n");
			eb.appendDescription(ga.getDescription(locale, true) + "\n\n");
		}

		FileUpload img = null;
		if (Utils.parseEmoji(type.getIcon()) instanceof CustomEmoji e) {
			int[] color = Graph.unpackRGB((switch (g.getRarityClass()) {
				case NORMAL -> Color.WHITE;
				case MAGIC -> new Color(0x4BA5FF);
				case RARE -> Color.ORANGE;
				case UNIQUE -> new Color(0xC64C00);
			}).getRGB());

			ThreadLocal<int[]> out = ThreadLocal.withInitial(() -> new int[4]);
			BufferedImage icon = com.kuuhaku.util.IO.getImage(e.getImageUrl());
			Graph.forEachPixel(icon, (x, y, rgb) -> {
				double bright = (rgb & 0xFF) / 255d;
				int[] aux = out.get();

				aux[0] = (rgb >> 24) & 0xFF;
				for (int i = 1; i < color.length; i++) {
					aux[i] = (int) (color[i] * bright);
				}

				return Graph.packRGB(aux);
			});

			img = FileUpload.fromData(IO.getBytes(icon, "png"), "thumb.png");
		}

		if (acc.getUid().equals(g.getOwner().getAccount().getUid())) {
			Runnable update = () -> updateEmbed(locale, acc, g.refresh(), eb, event, msg, mats);
			ButtonizeHelper helper = getButtons(locale, acc, g, mats, update);

			RestAction<Message> act;
			if (msg.get() == null) {
				MessageCreateAction ma = event.channel().sendMessageEmbeds(eb.build());
				if (img != null) {
					ma.setFiles(img);
				}

				act = helper != null ? helper.apply(ma) : ma;
			} else {
				MessageEditAction ma = msg.get().editMessageEmbeds(eb.build());
				if (img != null) {
					ma.setFiles(img);
				}

				act = helper != null ? helper.apply(ma) : ma;
			}

			act.queue(s -> {
				msg.set(s);
				if (helper != null) {
					Pages.buttonize(s, helper);
				}
			});
		} else {
			MessageCreateAction ma = event.channel().sendMessageEmbeds(eb.build());
			if (img != null) {
				ma.setFiles(img);
			}

			ma.queue();
		}
	}

	private static ButtonizeHelper getButtons(I18N locale, Account acc, Gear g, List<GlobalDrop> mats, Runnable update) {
		RarityClass rarity = g.getRarityClass();

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(acc.getUid()))
				.setCancellable(false);

		EmbedBuilder table = new ColorlessEmbedBuilder();

		boolean valid = false;
		for (GlobalDrop mat : mats) {
			if (mat.getMaxRarity().ordinal() < rarity.ordinal()) continue;

			int affs = g.getAffixes().size();
			if (affs < mat.getMinMods() || affs >= mat.getMaxMods()) continue;

			UserItem item = mat.getItem();
			table.appendDescription("-# " + item.getIcon() + " " + item.getDescription(locale) + "\n");

			helper.addAction(
					new EmojiId(Utils.parseEmoji(item.getIcon()), String.valueOf(acc.getItemCount(item.getId()))),
					w -> {
						try {
							if (!acc.consumeItem(item)) {
								w.getChannel().sendMessage(locale.get("error/item_not_enough")).queue();
								return;
							}

							mat.apply(locale, w.getChannel(), acc, g);
							if (g.isDestroyed()) {
								w.getMessage().delete().queue(null, Utils::doNothing);
							} else {
								update.run();
							}
						} catch (ItemUseException e) {
							acc.addItem(item, 1);
							String out = locale.get(e.getMessage(), e.getArgs());
							if (out.isBlank() || out.equalsIgnoreCase(e.getMessage())) {
								out = LocalizedString.get(locale, e.getMessage(), "").formatted(e.getArgs());
							}

							w.getChannel().sendMessage(Utils.getOr(out, e.getMessage())).queue();
						}
					}
			);

			valid = true;
		}

		helper.addAction(locale.get("str/help"), w -> {
			Objects.requireNonNull(w.getHook())
					.setEphemeral(true)
					.sendMessageEmbeds(table.build())
					.queue();
		});

		return valid ? helper : null;
	}
}
