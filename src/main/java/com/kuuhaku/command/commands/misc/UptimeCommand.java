package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends Command {

	public UptimeCommand() {
		super("uptime", "Diz-lhe à quanto tempo estou acordada.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		long uptimeSec = Instant.now().getEpochSecond() - Main.getInfo().getStartTime();

		int dias = (int)TimeUnit.SECONDS.toDays(uptimeSec);
		long horas = TimeUnit.SECONDS.toHours(uptimeSec) - (dias *24);
		long minutos = TimeUnit.SECONDS.toMinutes(uptimeSec) - (TimeUnit.SECONDS.toHours(uptimeSec)* 60);
		long segundos = TimeUnit.SECONDS.toSeconds(uptimeSec) - (TimeUnit.SECONDS.toMinutes(uptimeSec) * 60);

		String uptime = dias + " dias " + horas + " horas " + minutos + " mins " + segundos + " segundos...";

		channel.sendMessage("Hummm... acho que estou acordada à uns " + uptime).queue();
		//channel.sendMessage("Hummm... acho que estou acordada a " + (int) rb.getUptime() / 1000 + " segundos!").queue();
	}

}
