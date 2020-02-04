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
	DEVS("Dev", "<:1_:674247107053617192>", "674247107053617192", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
	SHERIFFS("Sheriffs", "<:2_:674247106487255042>", "674247106487255042", "Comandos de moderação global.", PrivilegeLevel.SHERIFF),
	MODERACAO("Moderação", "<:3_:674247106868805639>", "674247106868805639", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "<:4_:674247106856353830>", "674247106856353830", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "<:5_:674247106923462667>", "674247106923462667", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "<:6_:674247106776530946>", "674247106776530946", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "<:7_:674247107070263296>", "674247107070263296", "Comandos de informação", PrivilegeLevel.USER),
	RPG("RPG", "<:8_:674247106890039297>", "674247106890039297", "Comandos de RPG (Módulo Tet).", PrivilegeLevel.USER),
	MUSICA("Música", "<:9_:674247106956886016>", "674247106956886016", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "<:10:674247106906685461>", "674247106906685461", "Comandos de exceed", PrivilegeLevel.EXCEED);

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
