package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.World.World;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class NewCampaignCommand extends Command {

	public NewCampaignCommand() {
		super("rnovacampanha", new String[]{"rnewcampaign"}, "Abre uma nova campanha de RPG no servidor.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Helper.hasPermission(member, PrivilegeLevel.MOD)) {
			if (Main.getInfo().getGames().get(guild.getId()) != null) {
				channel.sendMessage(":x: | Já existe uma campanha iniciada neste servidor.").queue();
				return;
			}
			Main.getInfo().getGames().put(guild.getId(), new World(author));
			channel.sendMessage("Nova campanha iniciada com sucesso.\nMestre da campanha: " + author.getAsMention()).queue();
		} else {
			channel.sendMessage(":x: | Apenas moderadores podem mestrar campanhas.").queue();
		}
	}
}
