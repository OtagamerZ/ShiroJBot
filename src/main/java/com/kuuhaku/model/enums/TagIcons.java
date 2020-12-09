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

package com.kuuhaku.model.enums;

import com.kuuhaku.Main;
import com.kuuhaku.utils.ShiroInfo;
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
    LEVEL,
    EXCEED_CHAMPION,
    BUGHUNTER;

    public String getTag(int lvl) {
        return switch (this) {
            case NIICHAN -> "<:niichan:697879726018003115> ";
            case DEV -> "<:developer:697879725925990466> ";
            case SUPPORT -> "<:support:697879726047625216> ";
            case EDITOR -> "<:writer:697879725497909310> ";
            case READER -> "<:reader:697879726148288542> ";
            case MODERATOR -> "<:moderator:697879725628194878> ";
            case VERIFIED -> "<:verified:697879725887979621> ";
			case TOXIC -> "<:toxic:697879725816676442> ";
			case MARRIED -> "<:married:697879725888241684> ";
			case RICH -> "<:rich:718447753944105012> ";
            case COLLECTION25 -> "<:collection_25:724662152366915636> ";
            case COLLECTION50 -> "<:collection_50:724662153570812446> ";
            case COLLECTION75 -> "<:collection_75:724662152602058762> ";
            case COLLECTION100 -> "<:collection_100:724662152824225862> ";
            case FOIL25 -> "<:foil_25:747511886186151956> ";
            case FOIL50 -> "<:foil_50:747511886035026092> ";
            case FOIL75 -> "<:foil_75:747511886202798192> ";
            case FOIL100 -> "<:foil_100:747511886307655770> ";
            case LEVEL -> getLevelEmote(lvl).getAsMention() + " ";
            case EXCEED_CHAMPION -> "<:exceed_champion:755126333482336326> ";
            case BUGHUNTER -> "<:bughunter:775923780643061780> ";
        };
	}

	public String getId(int lvl) {
		return switch (this) {
			case NIICHAN -> "697879726018003115";
			case DEV -> "697879725925990466";
			case SUPPORT -> "697879726047625216";
			case EDITOR -> "697879725497909310";
			case READER -> "697879726148288542";
			case MODERATOR -> "697879725628194878";
			case VERIFIED -> "697879725887979621";
			case TOXIC -> "697879725816676442";
			case MARRIED -> "697879725888241684";
			case RICH -> "718447753944105012";
            case COLLECTION25 -> "724662152366915636";
            case COLLECTION50 -> "724662153570812446";
            case COLLECTION75 -> "724662152602058762";
            case COLLECTION100 -> "724662152824225862";
            case FOIL25 -> "747511886186151956";
            case FOIL50 -> "747511886035026092";
            case FOIL75 -> "747511886202798192";
            case FOIL100 -> "74751188630765577";
            case LEVEL -> getLevelEmote(lvl).getId();
            case EXCEED_CHAMPION -> "755126333482336326";
            case BUGHUNTER -> "775923780643061780";
        };
	}

	public static String getExceed(ExceedEnum t) {
		return switch (t) {
			case IMANITY -> "<:imanity:771067569490755624> "; //Old: 697879725690847324
			case SEIREN -> "<:seiren:771067569603084373> "; //Old: 697879725640515685
			case WEREBEAST -> "<:werebeast:771067569229791253> "; //Old: 697879725934379178
			case ELF -> "<:elf:771067569301880853> "; //Old: 697879725661749300
			case EXMACHINA -> "<:exmachina:771067569649483846> "; //Old: 697879725988904971
			case FLUGEL -> "<:flugel:771067569389305896> "; //Old: 697879725967933440
		};
	}

	public static String getExceedId(ExceedEnum t) {
		return switch (t) {
			case IMANITY -> "771067569490755624";
			case SEIREN -> "771067569603084373";
			case WEREBEAST -> "771067569229791253";
			case ELF -> "771067569301880853";
			case EXMACHINA -> "771067569649483846";
			case FLUGEL -> "771067569389305896";
		};
	}

	public static Emote getLevelEmote(int lvl) {
		int l = lvl - (lvl % 5);
		return Main.getShiroShards().getEmotesByName("lvl_" + Math.min(l, 320), true)
				.stream()
				.filter(e -> e.getGuild() != null && ShiroInfo.getEmoteRepo().contains(e.getGuild().getId()))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Emblema inexistente para o level " + l));
	}
}
