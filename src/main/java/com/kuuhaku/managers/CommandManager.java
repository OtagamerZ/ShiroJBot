package com.kuuhaku.managers;

import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.owner.*;
//import com.kuuhaku.command.commands.moderation.*;
import com.kuuhaku.command.commands.information.*;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.fun.*;

import java.util.ArrayList;

public class CommandManager {

	private ArrayList<Command> commands;
	
	public CommandManager() {
		commands = new ArrayList<>();

		//EXEMPLO: commands.add(new KillCommand());

        //OWNER
        commands.add(new KillCommand());
        commands.add(new LeaveCommand());

        //MODERATION

        //INFORMATION
        commands.add(new ComandosCommand());

        //MISC
        commands.add(new AvatarCommand());
        commands.add(new PingCommand());
        commands.add(new UptimeCommand());

        //FUN
        commands.add(new AbracarCommand());
        commands.add(new BeijarCommand());
		commands.add(new OhNoCommand());
	}
	
	public ArrayList<Command> getCommands() {
		return commands;
	}
	
}
