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
	LVL_5,
	LVL_10,
	LVL_15,
	LVL_20,
	LVL_25,
	LVL_30,
	LVL_35,
	LVL_40,
	LVL_45,
	LVL_50,
	LVL_55,
	LVL_60,
	LVL_65,
	LVL_70,
	LVL_75,
	LVL_80,
	LVL_85,
	LVL_90,
	LVL_95,
	LVL_100,
	LVL_105,
	LVL_110,
	LVL_115,
	LVL_120;

	public String getTag() {
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
			case LVL_5:
				return "<:lvl_5:732300509775527967> ";
			case LVL_10:
				return "<:lvl_10:732300509855088840> ";
			case LVL_15:
				return "<:lvl_15:732300509993762877> ";
			case LVL_20:
				return "<:lvl_20:732300510131912766> ";
			case LVL_25:
				return "<:lvl_25:732300510626840577> ";
			case LVL_30:
				return "<:lvl_30:732300510689755246> ";
			case LVL_35:
				return "<:lvl_35:732300510610325616> ";
			case LVL_40:
				return "<:lvl_40:732300510056677426> ";
			case LVL_45:
				return "<:lvl_45:732300509872128092> ";
			case LVL_50:
				return "<:lvl_50:732300510471782451> ";
			case LVL_55:
				return "<:lvl_55:732300510710857789> ";
			case LVL_60:
				return "<:lvl_60:732300510513856574> ";
			case LVL_65:
				return "<:lvl_65:732300510392221856> ";
			case LVL_70:
				return "<:lvl_70:732300510341627955> ";
			case LVL_75:
				return "<:lvl_75:732300510320918648> ";
			case LVL_80:
				return "<:lvl_80:732300510840750220> ";
			case LVL_85:
				return "<:lvl_85:732300510522114172> ";
			case LVL_90:
				return "<:lvl_90:732300510568251444> ";
			case LVL_95:
				return "<:lvl_95:732300510924636514> ";
			case LVL_100:
				return "<:lvl_100:732300510664589342> ";
			case LVL_105:
				return "<:lvl_105:732300510501142620> ";
			case LVL_110:
				return "<:lvl_110:732300510530371599> ";
			case LVL_115:
				return "<:lvl_115:732300510614519860> ";
			case LVL_120:
				return "<:lvl_120:732300510891212938> ";
		}
		throw new IllegalStateException();
	}

	public String getId() {
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
			case LVL_5:
				return "732300509775527967";
			case LVL_10:
				return "732300509855088840";
			case LVL_15:
				return "732300509993762877";
			case LVL_20:
				return "732300510131912766";
			case LVL_25:
				return "732300510626840577";
			case LVL_30:
				return "732300510689755246";
			case LVL_35:
				return "732300510610325616";
			case LVL_40:
				return "732300510056677426";
			case LVL_45:
				return "732300509872128092";
			case LVL_50:
				return "732300510471782451";
			case LVL_55:
				return "732300510710857789";
			case LVL_60:
				return "732300510513856574";
			case LVL_65:
				return "732300510392221856";
			case LVL_70:
				return "732300510341627955";
			case LVL_75:
				return "732300510320918648";
			case LVL_80:
				return "732300510840750220";
			case LVL_85:
				return "732300510522114172";
			case LVL_90:
				return "732300510568251444";
			case LVL_95:
				return "732300510924636514";
			case LVL_100:
				return "732300510664589342";
			case LVL_105:
				return "732300510501142620";
			case LVL_110:
				return "732300510530371599";
			case LVL_115:
				return "732300510614519860";
			case LVL_120:
				return "732300510891212938";
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
}
