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

import com.kuuhaku.utils.PrivilegeLevel;

import java.util.ArrayList;

public enum Category {
    DEVS("Dev", "\uD83D\uDEE0", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
	SHERIFFS("Sheriffs", "\uD83D\uDCDB", "Comandos de moderação global.", PrivilegeLevel.SHERIFF),
    MODERACAO("Moderação", "\u2699", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "\uD83D\uDC8E", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "\uD83C\uDF89", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "\u2733", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "\u2139", "Comandos de informação", PrivilegeLevel.USER),
	BEYBLADE("Beyblade", "\uD83D\uDCA0", "Comandos de Beyblade.", PrivilegeLevel.USER),
	MUSICA("Música", "\uD83C\uDFB6", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "\uD83C\uDF8C", "Comandos de exceed", PrivilegeLevel.EXCEED)
	;

	private final String name;
	private final String EMOTE;
	private final String description;
	private final PrivilegeLevel privilegeLevel;
	private boolean enabled = true;
	
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
	
	public boolean isEnabled() {
		return !enabled;
	}
	
	public void disable() {
		enabled = false;
	}

	public String getEMOTE() {
		return EMOTE;
	}
}
