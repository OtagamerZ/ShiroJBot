package com.kuuhaku.handlers.games.RPG.Commands;

import com.kuuhaku.handlers.games.RPG.World.Map;
import com.kuuhaku.handlers.games.RPG.World.World;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public interface RPGCommand {
	void execute(Map map, JDA jda, User player, World world, TextChannel channel, Message msg, String command, String[] args);
}
