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
    DEV, EDITOR, PARTNER, MODERATOR, CHAMPION, VETERAN, VERIFIED, TOXIC;

    public static String getTag(TagIcons t) {
        switch (t) {
            case DEV:
                return "<:Dev:589103373354270760> ";
            case EDITOR:
                return "<:Editor:589120809428058123> ";
            case PARTNER:
                return "<:Partner:589103374033485833> ";
            case MODERATOR:
                return "<:Moderator:589121447314587744> ";
            case CHAMPION:
                return "<:Champion:589120809616932864> ";
            case VETERAN:
                return "<:Veteran:589121447151271976> ";
            case VERIFIED:
                return "<:Verified:591425071772467211> ";
            case TOXIC:
                return "<:Toxic:589103372926451713> ";
        }
        return null;
    }
}
