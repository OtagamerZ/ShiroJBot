/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command;

import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.PrivilegeLevel;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public enum Category {
	DEVS("Dev", "\uD83D\uDEE0", "674247107053617192", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
	SHERIFFS("Sheriffs", "\uD83D\uDCDB", "674247106487255042", "Comandos de moderação global.", PrivilegeLevel.SHERIFF),
	MODERACAO("Moderação", "\u2699", "674247106868805639", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "\uD83D\uDC8E", "674247106856353830", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "\uD83C\uDF89", "674247106923462667", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "\u2733", "674247106776530946", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "\u2139", "674247107070263296", "Comandos de informação", PrivilegeLevel.USER),
	RPG("RPG", "\uD83D\uDCA0", "674247106890039297", "Comandos de RPG (Módulo Tet).", PrivilegeLevel.USER),
	MUSICA("Música", "\uD83C\uDFB6", "674247106956886016", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "\uD83C\uDF8C", "674247106906685461", "Comandos de exceed", PrivilegeLevel.EXCEED);

	private final String name;
	private final String emote;
	private final String emoteId;
	private final String description;
	private final PrivilegeLevel privilegeLevel;

	Category(String name, String emote, String emoteId, String description, PrivilegeLevel privilegeLevel) {
		this.name = name;
		this.emote = emote;
		this.emoteId = emoteId;
		this.description = description;
		this.privilegeLevel = privilegeLevel;

		cmds = new ArrayList<>();
	}

	public boolean equals(Category other) {
		return this.getName().equals(other.getName());
	}
	
	public String getName() {
		return name;
	}

	public static Category getByName(String name) throws ArrayIndexOutOfBoundsException {
		return Arrays.stream(Category.values()).filter(c -> c.name.equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
	}

	public boolean isBotBlocked() {
		switch (this) {
			case DEVS:
			case EXCEED:
			case MUSICA:
			case MODERACAO:
			case SHERIFFS:
				return true;
			default: return false;
		}
	}

	public String getDescription() {
		return description;
	}

	public PrivilegeLevel getPrivilegeLevel() {
		return privilegeLevel;
	}

	private final ArrayList<Command> cmds;
	
    void addCommand(Command cmd) {
		if(cmd.getCategory() == this)
			cmds.add(cmd);
	}
	
	public ArrayList<Command> getCmds(){
		return cmds;
	}

	public boolean isEnabled(GuildConfig gc, Guild g) {
		if (this == DEVS && !g.getId().equals(ShiroInfo.getSupportServerID())) {
			return false;
		} else if (this == PARTNER && !TagDAO.getTagById(g.getOwnerId()).isPartner()) {
			return false;
		} else return !gc.getDisabledModules().contains(this);
	}

	public String getEmote() {
		return emote;
	}

	public String getEmoteId() {
		return emoteId;
	}
}
