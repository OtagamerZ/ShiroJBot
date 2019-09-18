package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.World.World;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class EndCampaignCommand extends Command {

	public EndCampaignCommand() {
		super("rfimdejogo", new String[]{"rendcampaign", "rgameover"}, "Finaliza a campanha ativa no servidor", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			Main.getInfo().getGames().remove(guild.getId());
			channel.sendMessage("Campanha encerrada com sucesso.").queue();
		}
	}
}
