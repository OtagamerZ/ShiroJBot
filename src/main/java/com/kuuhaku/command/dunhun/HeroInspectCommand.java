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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.GearType;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Deck;
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
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
		// Obtém o deck do perfil do jogador
		Deck deck = getDeck(data, event, locale);
		if (deck == null) return;

		// Obtém o herói associado ao deck
		Hero hero = getHero(deck, event, data, locale);
		if (hero == null) return;

		// Obtém o gear
		Gear gear = getGear(args, event, locale);
		if (gear == null) return;

		// Carrega as informações do gear em relação ao herói
		gear.load(locale, hero);

		// Constrói o embed com as informações do gear
		EmbedBuilder embed = buildGearEmbed(gear, locale, hero);

		// Cria e envia a mensagem com o embed e imagem
		MessageCreateAction action = createMessageWithEmbed(event, gear, embed);
    	action.queue();
	}
	// Recupera o deck do perfil do jogador
	private Deck getDeck(EventData data, MessageData.Guild event, I18N locale) {
		Deck deck = data.profile().getAccount().getDeck();
		if (deck == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return null;
		}
		return deck;
	}
	
	// Recupera o herói do deck
	private Hero getHero(Deck deck, MessageData.Guild event, EventData data, I18N locale) {
		Hero hero = deck.getHero();
		if (hero == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return null;
		}
		return hero;
	}
	
	// Recupera o gear com base no argumento fornecido
	private Gear getGear(JSONObject args, MessageData.Guild event, I18N locale) {
		Gear gear = DAO.find(Gear.class, args.getInt("gear"));
		if (gear == null) {
			event.channel().sendMessage(locale.get("error/gear_not_found")).queue();
		}
		return gear;
	}

	// Constrói o embed com as informações detalhadas do equipamento
	private EmbedBuilder buildGearEmbed(Gear gear, I18N locale, Hero hero) {
		EmbedBuilder embed = new ColorlessEmbedBuilder()
				.setThumbnail("attachment://thumb.png");

		// Define o título do embed com base na raridade do gear
		if (gear.getRarityClass() == RarityClass.RARE) {
			embed.setTitle(gear.getName(locale) + ", " + gear.getBasetype().getInfo(locale).getName());
		} else {
			embed.setTitle(gear.getName(locale));
		}

		if (gear.getUnique() != null) {
			embed.setFooter(gear.getUnique().getInfo(locale).getDescription());
		}

		// Constrói a seção de tags e atributos do gear
		buildTagsAndAttributes(gear, embed, locale);

		// Constrói a seção de afixos
		buildGearAffixes(gear, embed, locale);
	
		return embed;
	}
	
	private void buildTagsAndAttributes(Gear gear, EmbedBuilder embed, I18N locale) {
		GearType type = gear.getBasetype().getStats().gearType();

		// Adiciona as tags do gear
		JSONArray tags = gear.getTags();
		if (!tags.isEmpty()) {
			List<String> tagNames = tags.stream()
					.map(t -> LocalizedString.get(locale, "tag/" + t, ""))
					.toList();
			tagNames.add(type.getInfo(locale).getName());
			embed.appendDescription("-# " + String.join(", ", tagNames) + "\n\n");
		}

		// Adiciona atributos
		boolean hasStats = false;
		GearStats stats = gear.getBasetype().getStats();
		if (gear.getDmg() != 0) {
			embed.appendDescription(locale.get("str/attack") + ": " + gear.getDmg() + "\n");
		}

		if (gear.getDfs() != 0) {
			embed.appendDescription(locale.get("str/defense") + ": " + gear.getDfs() + "\n");
		}

		if (gear.getCritical() != 0) {
			embed.appendDescription(locale.get("str/critical_chance") + ": " + Utils.roundToString(gear.getCritical(), 2) + "%\n");
		}
		
		if (hasStats) {
			embed.appendDescription("\n");
		}
		
		// Adiciona requisitos de atributos
		Attributes reqs = stats.requirements();
		if (reqs.str() + reqs.dex() + reqs.wis() + reqs.vit() > 0) {
			embed.appendDescription("-# " + locale.get("str/required_attributes") + "\n");
		}
	
		List<String> attributes = new ArrayList<>();
		if (gear.getReqLevel() > 0) attributes.add(locale.get("str/level", gear.getReqLevel()));
		for (AttrType t : AttrType.values()) {
			if (t.ordinal() >= AttrType.LVL.ordinal()) break;
			if (reqs.get(t) > 0) attributes.add(t + ": " + reqs.get(t) + " ");
		}
		if (!attributes.isEmpty()) {
			embed.appendDescription(String.join(" | ", attributes) + "\n\n");
		}
	}
	
	// Adiciona afixos ao embed
	private void buildGearAffixes(Gear gear, EmbedBuilder embed, I18N locale) {
		GearAffix implicit = gear.getImplicit();
		if (implicit != null) {
			embed.appendDescription("-# " + locale.get("str/implicit") + "\n");
			embed.appendDescription(implicit.getDescription(locale, true) + "\n");
			if (!gear.getAffixes().isEmpty()) {
				embed.appendDescription("──────────────────\n");
			}
		}
		
		// Ordena e adiciona os afixos
		List<GearAffix> affixes = gear.getAffixes().stream()
				.sorted(Comparator
						.<GearAffix, Boolean>comparing(ga -> ga.getAffix().getType() == AffixType.SUFFIX, Boolean::compareTo)
						.thenComparing(ga -> ga.getAffix().getId())
				)
				.toList();
	
		for (GearAffix ga : affixes) {
			embed.appendDescription("-# %s - %s%s\n".formatted(
					locale.get("str/" + ga.getAffix().getType()), ga.getName(locale),
					ga.getAffix().getTags().isEmpty() ? "" : ga.getAffix().getTags().stream()
							.map(t -> LocalizedString.get(locale, "tag/" + t, ""))
							.collect(Collectors.joining(", "))
			));
			embed.appendDescription(ga.getDescription(locale, true) + "\n\n");
		}
	}
	
	// Cria a mensagem com embed e adiciona o ícone processado
	private MessageCreateAction createMessageWithEmbed(MessageData.Guild event, Gear gear, EmbedBuilder embed) {
		MessageCreateAction action = event.channel().sendMessageEmbeds(embed.build());
	
		GearType type = gear.getBasetype().getStats().gearType();
		if (Utils.parseEmoji(type.getIcon()) instanceof CustomEmoji e) {
			int[] color = Graph.unpackRGB((switch (gear.getRarityClass()) {
				case NORMAL -> Color.WHITE;
				case MAGIC -> new Color(0x4BA5FF);
				case RARE -> Color.ORANGE;
				case UNIQUE -> new Color(0xC64C00);
			}).getRGB());

			ThreadLocal<int[]> out = ThreadLocal.withInitial(() -> new int[4]);
			BufferedImage icon = IO.getImage(e.getImageUrl());
			Graph.forEachPixel(icon, (x, y, rgb) -> {
				double bright = (rgb & 0xFF) / 255d;
				int[] aux = out.get();

				aux[0] = (rgb >> 24) & 0xFF;
				for (int i = 1; i < color.length; i++) {
					aux[i] = (int) (color[i] * bright);
				}

				return Graph.packRGB(aux);
			});

			action.addFiles(FileUpload.fromData(IO.getBytes(icon, "png"), "thumb.png"));
		}
		return action;
	}
}
