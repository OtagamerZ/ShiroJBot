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
import net.dv8tion.jda.api.entities.Emote;

import java.util.NoSuchElementException;

public enum TagIcons {
	NIICHAN,
	DEV,
	SUPPORT,
	EDITOR,
	READER,
	MODERATOR,
	VERIFIED,
	TOXIC,
	MARRIED,
	RICH,
	COLLECTION25,
	COLLECTION50,
	COLLECTION75,
	COLLECTION100,
	FOIL25,
	FOIL50,
	FOIL75,
	FOIL100,
	LEVEL;

	public String getTag(int lvl) {
		switch (this) {
			case NIICHAN:
				return "<:niichan:697879726018003115> ";
			case DEV:
				return "<:developer:697879725925990466> ";
			case SUPPORT:
				return "<:support:697879726047625216> ";
			case EDITOR:
				return "<:writer:697879725497909310> ";
			case READER:
				return "<:reader:697879726148288542> ";
			case MODERATOR:
				return "<:moderator:697879725628194878> ";
			case VERIFIED:
				return "<:verified:697879725887979621> ";
			case TOXIC:
				return "<:toxic:697879725816676442> ";
			case MARRIED:
				return "<:married:697879725888241684> ";
			case RICH:
				return "<:rich:718447753944105012> ";
			case COLLECTION25:
				return "<:collection_25:724662152366915636> ";
			case COLLECTION50:
				return "<:collection_50:724662153570812446> ";
			case COLLECTION75:
				return "<:collection_75:724662152602058762> ";
			case COLLECTION100:
				return "<:collection_100:724662152824225862> ";
			case FOIL25:
				return "<:foil_25:747511886186151956> ";
			case FOIL50:
				return "<:foil_50:747511886035026092> ";
			case FOIL75:
				return "<:foil_75:747511886202798192> ";
			case FOIL100:
				return "<:foil_100:747511886307655770> ";
			case LEVEL:
				return getLevelEmote(lvl).getAsMention() + " ";
		}
		throw new IllegalStateException();
	}

	public String getId(int lvl) {
		switch (this) {
			case NIICHAN:
				return "697879726018003115";
			case DEV:
				return "697879725925990466";
			case SUPPORT:
				return "697879726047625216";
			case EDITOR:
				return "697879725497909310";
			case READER:
				return "697879726148288542";
			case MODERATOR:
				return "697879725628194878";
			case VERIFIED:
				return "697879725887979621";
			case TOXIC:
				return "697879725816676442";
			case MARRIED:
				return "697879725888241684";
			case RICH:
				return "718447753944105012";
			case COLLECTION25:
				return "724662152366915636";
			case COLLECTION50:
				return "724662153570812446";
			case COLLECTION75:
				return "724662152602058762";
			case COLLECTION100:
				return "724662152824225862";
			case FOIL25:
				return "747511886186151956";
			case FOIL50:
				return "747511886035026092";
			case FOIL75:
				return "747511886202798192";
			case FOIL100:
				return "74751188630765577";
			case LEVEL:
				return getLevelEmote(lvl).getId();
		}
		throw new IllegalStateException();
	}

	public static String getExceed(ExceedEnums t) {
		switch (t) {
			case IMANITY:
				return "<:imanity:697879725690847324> ";
			case SEIREN:
				return "<:seiren:697879725640515685> ";
			case WEREBEAST:
				return "<:werebeast:697879725934379178> ";
			case ELF:
				return "<:elf:697879725661749300> ";
			case EXMACHINA:
				return "<:exmachina:697879725988904971> ";
			case FLUGEL:
				return "<:flugel:697879725967933440> ";
		}
		throw new IllegalStateException();
	}

	public static String getExceedId(ExceedEnums t) {
		switch (t) {
			case IMANITY:
				return "697879725690847324";
			case SEIREN:
				return "697879725640515685";
			case WEREBEAST:
				return "697879725934379178";
			case ELF:
				return "697879725661749300";
			case EXMACHINA:
				return "697879725988904971";
			case FLUGEL:
				return "697879725967933440";
		}
		throw new IllegalStateException();
	}

	public static Emote getLevelEmote(int lvl) {
		int l = -1;
		for (int i = 5; true; i += 5) {
			if (lvl >= i) l = i;
			else break;
		}
		return Main.getInfo().getAPI().getEmotesByName("lvl_" + Math.min(l, 120), true)
				.stream()
				.filter(e -> e.getGuild() != null && ShiroInfo.getEmoteRepo().contains(e.getGuild().getId()))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Emblema inexistente para o level " + lvl));
	}
}
