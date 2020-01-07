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

package com.kuuhaku.command;

import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.Arrays;

public enum Category {
    DEVS("Dev", "\uD83D\uDEE0", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
	SHERIFFS("Sheriffs", "\uD83D\uDCDB", "Comandos de moderação global.", PrivilegeLevel.SHERIFF),
    MODERACAO("Moderação", "\u2699", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "\uD83D\uDC8E", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "\uD83C\uDF89", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "\u2733", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "\u2139", "Comandos de informação", PrivilegeLevel.USER),
	RPG("RPG", "\uD83D\uDCA0", "Comandos de RPG (Módulo Tet).", PrivilegeLevel.USER),
	MUSICA("Música", "\uD83C\uDFB6", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "\uD83C\uDF8C", "Comandos de exceed", PrivilegeLevel.EXCEED);

	private final String name;
	private final String EMOTE;
	private final String description;
	private final PrivilegeLevel privilegeLevel;
	
	Category(String name, String emote, String description, PrivilegeLevel privilegeLevel) {
		this.name = name;
		this.EMOTE = emote;
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

	public static Category getByName(String name) {
		System.out.println(name);
		return Arrays.stream(Category.values()).filter(c -> c.name.equalsIgnoreCase(name)).findFirst().orElseThrow(RuntimeException::new);
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
	
	public boolean isEnabled(guildConfig gc, Guild g) {
		return gc.getDisabledModules().contains(this) || (this == PARTNER && TagDAO.getTagById(g.getOwnerId()).isPartner()) || (this == DEVS && g.getId().equals("421495229594730496"));
	}

	public String getEMOTE() {
		return EMOTE;
	}
}
