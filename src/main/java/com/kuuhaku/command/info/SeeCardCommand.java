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

package com.kuuhaku.command.info;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;

import com.kuuhaku.util.Utils;
import com.kuuhaku.util.ImageFilters;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Bit32;

import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "see",
		category = Category.INFO
)
@Syntax("<card:word:r> <kind:word>[n,c,s]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SeeCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		// Obtém o "Kawaipon" associado ao usuário que executou o comando.
		Kawaipon kp = getKawaipon(event);

		// Obtém o Card a partir dos argumentos fornecidos.
		Card card = getCard(event, args, locale);
        if (card == null) return;

		// Verifica se o cartão está armazenado na conta do usuário.
		int stored = DAO.queryNative(Integer.class, "SELECT count(1) FROM stashed_card WHERE kawaipon_uid = ?1 AND card_id = ?2",
				event.user().getId(),
				card.getId()
		);

		// Cria o embed que será enviado com a imagem do cartão e informações.
		EmbedBuilder embed = createEmbed(card, kp, locale, stored);

		String type = args.getString("kind", "n").toLowerCase();

		// Variável da imagem que será renderizada.
		BufferedImage bi = null;

		switch (type) {
			case "n", "c" -> {
				bi = processNormalOrChromeCard(type, kp, card, locale, event, embed);
			}
			case "s" -> {
				bi = processShoukanCard(kp, card, locale, event, embed, data);
			}
		}
		if (bi == null) return;

		// Envia o embed com a imagem gerada.
		sendEmbedWithImage(event, embed, bi);
	}

	private Kawaipon getKawaipon(MessageData.Guild event) {
        return DAO.find(Kawaipon.class, event.user().getId());
    }
	
	private Card getCard(MessageData.Guild event, JSONObject args, I18N locale) {
        // Tenta encontrar o objeto Card pelo identificador fornecido.
		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			String sug = Utils.didYouMean(args.getString("card"), "SELECT id AS value FROM v_card_names");
			if (sug == null) {
				// Se não houver sugestões, envia mensagem de erro.
				event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
			}
			else {
				// Caso haja sugestão, envia a sugestão de nome.
				event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			}
		}
		return card;
    }

	private EmbedBuilder createEmbed(Card card, Kawaipon kp, I18N locale, int stored) {
        EmbedBuilder embed = new ColorlessEmbedBuilder()
			.setAuthor(locale.get("str/in_stash", stored))
			.setTitle(card.getName() + " (" + card.getAnime() + ")")
			.setImage("attachment://card.png");

		// Se o cartão for o favorito do Kawaipon, adiciona essa informação ao embed.	
        if (card.equals(kp.getFavCard())) {
            embed.addField(locale.get("str/favored").toUpperCase(), Constants.VOID, false); // Indica se é o card favorito.
        }
        return embed;
    }

	private BufferedImage processNormalOrChromeCard(String type, Kawaipon kp, Card card, I18N locale, MessageData.Guild event, EmbedBuilder embed) {
		// Verifica se o cartão possui uma raridade válida.
		if (card.getRarity().getIndex() == -1) {
			event.channel().sendMessage(locale.get("error/not_kawaipon")).queue();
			return null;
		}

		boolean chrome = type.equals("c");
		KawaiponCard kc = kp.getCard(card, chrome);
		BufferedImage bi;
		if (kc == null) {
			// Se o cartão do Kawaipon não existir, desenha a versão padrão do cartão.
			bi = card.drawCard(false);
			ImageFilters.silhouette(bi);
			Graph.overlay(bi, IO.getResourceAsImage("kawaipon/missing.png"));
		} else {
			// Caso o cartão do Kawaipon exista, utiliza a versão renderizada dele.
			bi = kc.render();
		}

		if (kc != null) {
			// Se o KawaiponCard existir, adiciona informações sobre a qualidade e preço sugerido ao embed.
			XStringBuilder sb = new XStringBuilder();
			sb.appendNewLine(locale.get("str/quality", Utils.roundToString(kc.getQuality(), 1)));
			sb.appendNewLine(locale.get("str/suggested_price", kc.getSuggestedPrice()));

			embed.addField(locale.get("str/information"), sb.toString(), true);
		}

		addSenshiInfo(card, embed, locale);
		return bi;
	}
	
	private void addSenshiInfo(Card card, EmbedBuilder embed, I18N locale) {
		Senshi senshi = card.asSenshi();
		// Verifica se o cartão é um "Senshi" e adiciona informações sobre ele ao embed.
		if (senshi != null) {
			if (senshi.isFusion()) {
				embed.addField(locale.get("str/shoukan_enabled"), locale.get("icon/alert") + " " + locale.get("str/as_fusion"), true);
			} else {
				embed.addField(locale.get("str/shoukan_enabled"), locale.get("icon/success") + " " + locale.get("str/yes"), true);
			}
		} else {
			embed.addField(locale.get("str/shoukan_enabled"), locale.get("icon/error") + " " + locale.get("str/no"), true);
		}
	}

	private BufferedImage processShoukanCard(Kawaipon kp, Card card, I18N locale, MessageData.Guild event, EmbedBuilder embed, EventData data) {
		// Verifica se o usuário possui um deck.
		Deck dk = kp.getAccount().getDeck();
		if (dk == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return null;
		}

		// Obtém os tipos de cartão associados ao cartão em questão.
		List<CardType> types = List.copyOf(Bit32.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card.getId())));
		if (types.isEmpty()) {
			event.channel().sendMessage(locale.get("error/not_in_shoukan")).queue();
			return null;
		}

		// Determina o tipo de desenho que será gerado com base no tipo de cartão.
		Drawable<?> d = switch (types.getLast()) {
			case SENSHI -> card.asSenshi();
			case EVOGEAR -> card.asEvogear();
			case FIELD -> card.asField();
			default -> null;
		};

		// Processa o desenho e gera a imagem correspondente.
		return handleDrawable(d, locale, dk, embed);
	}

	private BufferedImage handleDrawable(Drawable<?> drawable, I18N locale, Deck deck, EmbedBuilder embed) {
		BufferedImage bi = drawable.render(locale, deck);

		// Verifica o tipo de "drawable" e adiciona informações específicas ao embed.
		if (drawable instanceof EffectHolder<?> eh) {
			embed.setDescription(eh.getReadableDescription(locale));
		}

		// Adiciona informações específicas para Senshi, Evogear ou Field.
		if (drawable instanceof Senshi s) {
			addSenshiDetails(s, embed, locale);
		} 
		
		else if (drawable instanceof Evogear e) {
			addEvogearDetails(e, embed, locale);
		} 
		
		else if (drawable instanceof Field f) {
			addFieldDetails(f, embed, locale);
		}

		// Adiciona tags associadas ao drawable.
		addTags(drawable, embed, locale);
		return bi;
	}

	private void addSenshiDetails(Senshi s, EmbedBuilder embed, I18N locale) {
		if (s.getCard().getRarity() == Rarity.NONE) {
			if (!embed.getDescriptionBuilder().isEmpty()) {
				embed.appendDescription("\n\n");
			}

			embed.appendDescription("**" + locale.get("str/effect_only") + "**");
		}

		if (s.isFusion()) {
			embed.setAuthor(null);
		}
	}

	private void addEvogearDetails(Evogear e, EmbedBuilder embed, I18N locale) {
		if (e.getTier() < 0) {
			if (!embed.getDescriptionBuilder().isEmpty()) {
				embed.appendDescription("\n\n");
			}

			embed.appendDescription("**" + locale.get("str/effect_only") + "**");
		}

		embed.addField(locale.get("str/charms"),
				e.getCharms().stream()
						.map(c -> Charm.valueOf(String.valueOf(c)))
						.map(c -> "**" + c.getName(locale) + ":** " + c.getDescription(locale, e.getTier()))
						.collect(Collectors.joining("\n")),
				false
		);
	}

	private void addFieldDetails(Field f, EmbedBuilder embed, I18N locale) {
		if (f.isEffect()) {
			if (!embed.getDescriptionBuilder().isEmpty()) {
				embed.appendDescription("\n\n");
			}

			embed.appendDescription("**" + locale.get("str/effect_only") + "**");
		}

		if (f.getType() != FieldType.NONE) {
			embed.addField(locale.get("field/" + f.getType()), locale.get("field/" + f.getType() + "_desc"), false);
		}
	}

	private void addTags(Drawable<?> drawable, EmbedBuilder embed, I18N locale) {
		if (!drawable.getTagBundle().isEmpty()) {
			embed.addField(locale.get("str/tags"),
					drawable.getTags(locale).stream()
							.map(s -> "`" + s + "`")
							.collect(Collectors.joining(" ")),
					false
			);
		}
	}

	private void sendEmbedWithImage(MessageData.Guild event, EmbedBuilder embed, BufferedImage bi) {
		// Converte a imagem para formato PNG e envia o embed com a imagem gerada.
		event.channel().sendMessageEmbeds(embed.build())
				.addFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "card.png"))
				.queue();
	}
}
