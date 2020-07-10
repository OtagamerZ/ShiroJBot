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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.python.bouncycastle.util.Arrays;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

public enum Tag {
	NIICHAN("icons/niichan.png", TagIcons.NIICHAN, "Desenvolvedor inicial da Shiro, nominalmente KuuHaKu.",
			(user, member) -> Helper.hasPermission(member, PrivilegeLevel.NIICHAN)),

	DESENVOLVEDOR("icons/dev.png", TagIcons.DEV, "Equipe de desenvolvimento da Shiro.",
			(user, member) -> Helper.hasPermission(member, PrivilegeLevel.DEV)),

	SUPORTE("icons/support.png", TagIcons.SUPPORT, "Equipe de suporte da Shiro.",
			(user, member) -> Helper.hasPermission(member, PrivilegeLevel.SUPPORT)),

	REDATOR("icons/writer.png", TagIcons.EDITOR, "Equipe de redação da Shiro.",
			(user, member) -> false),

	MODERADOR("icons/mod.png", TagIcons.MODERATOR, "Equipe de moderação desse servidor.",
			(user, member) -> Helper.hasPermission(member, PrivilegeLevel.MOD)),

	LEITOR("icons/reader.png", TagIcons.READER, "Você leu as regras, que bom!",
			(user, member) -> TagDAO.getTagById(user.getId()).isReader()),

	VERIFICADO("icons/verified.png", TagIcons.VERIFIED, "Usuário com conduta exemplar e identidade verificada.",
			(user, member) -> TagDAO.getTagById(user.getId()).isVerified()),

	TOXICO("icons/toxic.png", TagIcons.TOXIC, "Usuário com atitude tóxica.",
			(user, member) -> TagDAO.getTagById(user.getId()).isToxic()),

	CASADO("icons/married.png", TagIcons.MARRIED, "Usuário que possui uma waifu/husbando UwU.",
			(user, member) -> WaifuDAO.isWaifued(user)),

	LEVEL("icons/lvl_{0}.png", null, "Usuário que atingiu um dos marcos de level.",
			(user, member) -> true),

	RICO("icons/rich.png", TagIcons.RICH, "Usuário que possui 100 mil créditos.",
			(user, member) -> AccountDAO.getAccount(user.getId()).getBalance() > 100000),

	COLETADO_25("icons/collection_25.png", TagIcons.COLLECTION25, "Usuário que completou 25% da coleção de Kawaipons.",
			(user, member) -> KawaiponDAO.getKawaipon(user.getId()).getCards().size() * 100 / CardDAO.totalCards() >= 25),

	COLETADO_50("icons/collection_50.png", TagIcons.COLLECTION50, "Usuário que completou 50% da coleção de Kawaipons.",
			(user, member) -> KawaiponDAO.getKawaipon(user.getId()).getCards().size() * 100 / CardDAO.totalCards() >= 50),

	COLETADO_75("icons/collection_75.png", TagIcons.COLLECTION75, "Usuário que completou 75% da coleção de Kawaipons.",
			(user, member) -> KawaiponDAO.getKawaipon(user.getId()).getCards().size() * 100 / CardDAO.totalCards() >= 75),

	COLETADO_100("icons/collection_100.png", TagIcons.COLLECTION100, "Usuário que completou 100% da coleção de Kawaipons.",
			(user, member) -> KawaiponDAO.getKawaipon(user.getId()).getCards().size() * 100 / CardDAO.totalCards() >= 100);

	private final String path;
	private final TagIcons emote;
	private final String description;
	private final BiFunction<User, Member, Boolean> condition;

	Tag(String path, TagIcons emote, String description, BiFunction<User, Member, Boolean> condition) {
		this.path = path;
		this.emote = emote;
		this.description = description;
		this.condition = condition;
	}

	public InputStream getPath(com.kuuhaku.model.persistent.Member mb) throws IOException, NullPointerException {
		return Helper.getImage(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(Objects.requireNonNull(getEmote(mb)).getId())).getImageUrl());
	}

	public TagIcons getEmote(com.kuuhaku.model.persistent.Member mb) {
		if (this.equals(LEVEL)) {
			final int[] levels = {2, 3, 4, 5, 6, 7};
			int lvl = Helper.clamp(mb.getLevel(), mb.getLevel(), 70) / 10;
			if (Arrays.contains(levels, lvl)) {
				return TagIcons.valueOf("LVL" + (lvl * 10));
			} else return null;
		}
		return emote;
	}

	public TagIcons getEmote() {
		return emote;
	}

	public String getDescription() {
		return description;
	}

	public static Set<Tag> getTags(User u, Member m) {
		Set<Tag> tags = new TreeSet<>(Comparator.comparing(Tag::toString));
		for (Tag t : values()) {
			if (t.condition.apply(u, m)) tags.add(t);
		}
		return tags;
	}

	@Override
	public String toString() {
		return StringUtils.capitalize(name().toLowerCase().replace("_", " "));
	}
}
