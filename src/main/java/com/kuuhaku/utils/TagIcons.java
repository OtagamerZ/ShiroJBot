/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.utils;

public enum TagIcons {
	DEV, SHERIFF, EDITOR, READER, MODERATOR, CHAMPION, LVL20, LVL30, LVL40, LVL50, LVL60, LVL70, VERIFIED, TOXIC, MARRIED;

	public static String getTag(TagIcons t) {
		switch (t) {
			case DEV:
				return "<:Dev:589103373354270760> ";
			case SHERIFF:
				return "<:sheriff:613934507619385374>";
			case EDITOR:
				return "<:Editor:589120809428058123> ";
			case READER:
				return "<:reader:616680618037870642> ";
			case MODERATOR:
				return "<:Moderator:589121447314587744> ";
			case CHAMPION:
				return "<:Champion:589120809616932864> ";
			case LVL20:
				return "<:lvl_20:611156384909623296> ";
			case LVL30:
				return "<:lvl_30:611156385157349385> ";
			case LVL40:
				return "<:lvl_40:611156385291305001> ";
			case LVL50:
				return "<:lvl_50:611156384935051274> ";
			case LVL60:
				return "<:lvl_60:611156384989577241> ";
			case LVL70:
				return "<:lvl_70:616680678804815927> ";
			case VERIFIED:
				return "<:Verified:591425071772467211> ";
			case TOXIC:
				return "<:Toxic:589103372926451713> ";
			case MARRIED:
				return "<:Married:598908829769400320>";
		}
		return null;
	}

	public static String getExceed(ExceedEnums t) {
		switch (t) {
			case IMANITY:
				return "<:imanity:613741198288617474>";
			case SEIREN:
				return "<:seiren:613741198334754827>";
			case WEREBEAST:
				return "<:werebeast:613741197680312321>";
			case ELF:
				return "<:lumamana:613741197764067358>";
			case EXMACHINA:
				return "<:exmachina:613741197231390720>";
			case FLUGEL:
				return "<:flugel:613741197726318592>";
		}
		return null;
	}
}
