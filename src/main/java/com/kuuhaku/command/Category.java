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
    DEVS("Dev", "Comandos dedicados aos devs do bot.", PrivilegeLevel.DEV),
    MODERACAO("Moderação", "Comandos dedicados à staff do servidor.", PrivilegeLevel.MOD),
	PARTNER("Parceiros", "Comandos exclusivos para parceiros", PrivilegeLevel.USER),
	FUN("Diversão", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "Comandos de informação", PrivilegeLevel.USER),
	BEYBLADE("Beyblade", "Comandos de Beyblade.", PrivilegeLevel.USER),
	MUSICA("Música", "Comandos de música.", PrivilegeLevel.DJ),
	EXCEED("Exceed", "Comandos de exceed", PrivilegeLevel.EXCEED)
	;

	private final String name;
	private final String description;
	private final PrivilegeLevel privilegeLevel;
	private boolean enabled = true;
	
	Category(String name, String description, PrivilegeLevel privilegeLevel) {
		this.name = name;
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
}
