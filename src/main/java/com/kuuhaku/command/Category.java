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

import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.PrivilegeLevel;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public enum Category {
	DEV("Dev", "<:dev:674261700333142046>", "674261700333142046", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
	SHERIFFS("Sheriffs", "<:sheriff:674261700538662913>", "674261700538662913", "Comandos de moderação global.", PrivilegeLevel.SHERIFF),
	MODERACAO("Moderação", "<:mod:674261700844716082>", "674261700844716082", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "<:partner:674261701109219328>", "674261701109219328", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "<:fun:674261700941185035>", "674261700941185035", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "<:misc:674261700354113536>", "674261700354113536", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "<:info:674261700643651645>", "674261700643651645", "Comandos de informação", PrivilegeLevel.USER),
	RPG("RPG", "<:rpg:674261700962418688>", "674261700962418688", "Comandos de RPG (Módulo Tet).", PrivilegeLevel.USER),
	MUSICA("Música", "<:music:674261701507678220>", "674261701507678220", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "<:exceed:674261700312170496>", "674261700312170496", "Comandos de exceed", PrivilegeLevel.EXCEED);

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
			case DEV:
			case EXCEED:
			case MUSICA:
			case MODERACAO:
			case SHERIFFS:
				return true;
			default:
				return false;
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
		if (cmd.getCategory() == this)
			cmds.add(cmd);
	}

	public ArrayList<Command> getCmds() {
		return cmds;
	}

	public boolean isEnabled(GuildConfig gc, Guild g, User u) {
		if (this == DEV && (!g.getId().equals(ShiroInfo.getSupportServerID()) && !Main.getInfo().getDevelopers().contains(u.getId()))) {
			return false;
		} else if (this == PARTNER && (!TagDAO.getTagById(g.getOwnerId()).isPartner() && !Main.getInfo().getDevelopers().contains(u.getId()))) {
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
