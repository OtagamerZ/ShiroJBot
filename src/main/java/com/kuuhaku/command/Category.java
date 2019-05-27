package com.kuuhaku.command;

import com.kuuhaku.utils.PrivilegeLevel;

import java.util.ArrayList;

public enum Category {
	OWNER("Owner", "Comandos dedicados ao dono do bot.", PrivilegeLevel.OWNER),
	FUN("Diversão", "Comandos para diversão.", PrivilegeLevel.USER),
	MISC("Diversos", "Comandos diversos.", PrivilegeLevel.USER),
	INFO("Informação", "Comandos de informação", PrivilegeLevel.USER),
	MODERACAO("Moderação", "Comandos dedicados à staff do servidor.", PrivilegeLevel.STAFF),
	;
	
	private String name;
	private String description;
	private PrivilegeLevel privilegeLevel;
	private boolean enabled = true;
	
	Category(String name, String description, PrivilegeLevel privilegeLevel) {
		this.name = name;
		this.description = description;
		this.privilegeLevel = privilegeLevel;
		
		cmds = new ArrayList<>();
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
	
	private ArrayList<Command> cmds;
	
    void addCommand(Command cmd) {
		if(cmd.getCategory() == this)
			cmds.add(cmd);
	}
	
	public ArrayList<Command> getCmds(){
		return cmds;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void disable() {
		enabled = false;
	}
}
