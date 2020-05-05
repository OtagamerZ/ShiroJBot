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
	DEV, SUPPORT, EDITOR, READER, MODERATOR, LVL20, LVL30, LVL40, LVL50, LVL60, LVL70, VERIFIED, TOXIC, MARRIED;

	public static String getTag(TagIcons t) {
		switch (t) {
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
			case LVL20:
				return "<:lvl_20:697879725972127885> ";
			case LVL30:
				return "<:lvl_30:697879725951156294> ";
			case LVL40:
				return "<:lvl_40:697879726043299942> ";
			case LVL50:
				return "<:lvl_50:697879725862944899> ";
			case LVL60:
				return "<:lvl_60:697879725984710777> ";
			case LVL70:
				return "<:lvl_70:697879726009745458> ";
			case VERIFIED:
				return "<:verified:697879725887979621> ";
			case TOXIC:
				return "<:toxic:697879725816676442> ";
			case MARRIED:
				return "<:married:697879725888241684> ";
		}
		throw new IllegalStateException();
	}

	public static String getId(TagIcons t) {
		switch (t) {
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
			case LVL20:
				return "697879725972127885";
			case LVL30:
				return "697879725951156294";
			case LVL40:
				return "697879726043299942";
			case LVL50:
				return "697879725862944899";
			case LVL60:
				return "697879725984710777";
			case LVL70:
				return "697879726009745458";
			case VERIFIED:
				return "697879725887979621";
			case TOXIC:
				return "697879725816676442";
			case MARRIED:
				return "697879725888241684";
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
