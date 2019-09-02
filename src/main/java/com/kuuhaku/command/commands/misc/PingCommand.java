package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.utils.ExceedEnums;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class PingCommand extends Command {
	
	public PingCommand() { super("ping", "Ping", Category.MISC); }

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE))
            channel.sendMessage("Pong! :ping_pong: ").queue(msg -> msg.editMessage(msg.getContentRaw() + Main.getInfo().getPing() + " ms!").queue());
		String ex = MySQL.getWinner();
		MySQL.getExceedMembers(ExceedEnums.getByName(ex)).forEach(em ->
				Main.getInfo().getUserByID(em.getMid()).openPrivateChannel().queue(c -> {
					try {
						c.sendMessage("O seu exceed foi campeão neste mês, parabéns!\n" +
								"Todos da " + ex + " ganharão experiência em dobro durante 1 semana.").queue();
					} catch (Exception ignore) {
					}
				}));
	}
}
