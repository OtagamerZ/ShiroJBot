package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Handlers.CombatHandler;
import com.kuuhaku.handlers.games.RPG.Handlers.PvPHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class AttackCommand extends Command {

	public AttackCommand() {
		super("rattack", new String[]{"rduelar", "rduel"}, "Inicia um duelo com um jogador", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (message.getMentionedUsers().size() == 0) {
				channel.sendMessage(":x: | Você precisa especificar um usuário para atacar").queue();
				return;
			} else if (args.length == 1) {
				channel.sendMessage(":x: | Você precisa especificar um monstro como atacante").queue();
				return;
			}

			Actor.Player player = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
			Actor.Monster mob = Main.getInfo().getGames().get(guild.getId()).getMonsters().get(args[1]);

			new CombatHandler(Main.getTet(), Main.getInfo().getGames().get(guild.getId()), message.getTextChannel(), player, mob);
			return;
		}
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa especificar um usuário para duelar").queue();
			return;
		}
		Actor.Player player = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
		Actor.Player target = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId());

		if (player.getPos() != target.getPos()) {
			channel.sendMessage(":x: | Este jogador não está na mesma área que você").queue();
			return;
		}

		new PvPHandler(Main.getTet(), message.getTextChannel(), player, target);
	}
}
