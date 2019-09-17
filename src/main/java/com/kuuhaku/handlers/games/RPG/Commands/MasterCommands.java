package com.kuuhaku.handlers.games.RPG.Commands;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Entities.Character;
import com.kuuhaku.handlers.games.RPG.Entities.Equipped;
import com.kuuhaku.handlers.games.RPG.Exceptions.UnknownItemException;
import com.kuuhaku.handlers.games.RPG.Handlers.*;
import com.kuuhaku.handlers.games.RPG.Utils;
import com.kuuhaku.handlers.games.RPG.World.Map;
import com.kuuhaku.handlers.games.RPG.World.World;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.util.Arrays;

public class MasterCommands implements RPGCommand {
	@Override
	public void execute(Map map, JDA jda, User player, World world, TextChannel channel, Message msg, String command, String[] args) {
		try {
			switch (command) {
				case "atacar":
					attack(jda, world, channel, msg, player, args[0]);
					break;
				case "registrar":
				case "criar":
					register(jda, player, channel, world);
					msg.delete().queue();
					break;
				case "novomapa":
					new MapRegisterHandler(channel, jda, player);
					break;
				case "trocar":
					setMap(channel, world, Integer.parseInt(args[0]));
					break;
				case "remover":
				case "deletar":
					world.getMonsters().remove(String.join(" ", args));
					msg.delete().queue();
					break;
				case "dar":
					giveOrTakeItem(world, channel, args, msg, true);
					msg.delete().queue();
					break;
				case "tirar":
					giveOrTakeItem(world, channel, args, msg, false);
					msg.delete().queue();
					break;
				case "item":
					makeItem(jda, player, channel);
					msg.delete().queue();
					break;
				case "analisar":
				case "ver":
					view(channel, world, String.join(" ", args));
					msg.delete().queue();
					return;
				case "lista":
					list(args, channel, world);
					msg.delete().queue();
					break;
				case "mapa":
					map(channel, world);
					msg.delete().queue();
					break;
				case "rolar":
					channel.sendMessage(Utils.rollDice(String.join(" ", args), null)).queue();
					msg.delete().queue();
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage(":x: | Você precisa especificar o nome do item.").queue();
		} catch (UnknownItemException e) {
			channel.sendMessage(":x: | Item desconhecido.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | O argumento precisa ser um valor numérico inteiro.").queue();
		}
	}

	private void list(String[] args, TextChannel channel, World world) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa especificar o tipo da lista").queue();
			return;
		}
		switch (args[0]) {
			case "p":
				world.listPlayers(channel).queue();
				break;
			case "m":
				world.listMonsters(channel).queue();
				break;
			case "i":
				world.listItems(channel).queue();
				break;
		}
	}

	private void register(JDA jda, User player, TextChannel channel, World world) {
		new MobRegisterHandler(channel, jda, player, world);
	}

	private void makeItem(JDA jda, User player, TextChannel channel) {
		new ItemRegisterHandler(channel, jda, player);
	}

	private void giveOrTakeItem(World world, TextChannel channel, String[] args, Message msg, boolean give) throws UnknownItemException {
		if (msg.getMentionedUsers().size() < 1) {
			channel.sendMessage(":x: | Você precisa especificar o jogador").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | O segundo argumento precisa ser o nome do item ou ouro").queue();
			return;
		} else if (args.length < 3 && (args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			channel.sendMessage(":x: | Você precisa especificar a quantidade de ouro").queue();
			return;
		}

		Equipped inv = world.getPlayers().get(msg.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
		if (give) {
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) inv.addGold(Integer.parseInt(args[2]));
			else inv.addItem(world.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
		}
		else {
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) inv.addGold(-Integer.parseInt(args[2]));
			else inv.removeItem(world.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
		}
	}

	private void map(TextChannel channel, World world) throws IOException {
		world.render(channel).queue();
	}

	private void view(TextChannel channel, World world, String name) throws UnknownItemException {
		world.getItem(name).info(channel).queue();
	}

	private void setMap(TextChannel channel, World world, int index) {
		try {
			world.switchMap(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage(":x: | Índice inválido, existem " + world.getMaps().size() + " mapas cadastrados.").queue();
		}
	}

	private void attack(JDA jda, World world, TextChannel channel, Message msg, User p, String name) {
		if (msg.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa especificar um usuário para atacar").queue();
			return;
		}
		Actor.Player player = Main.getInfo().getGames().get(msg.getGuild().getId()).getPlayers().get(p.getId());
		Actor.Monster mob = Main.getInfo().getGames().get(msg.getGuild().getId()).getMonsters().get(name);

		new CombatHandler(jda, world, channel, player, mob);
	}
}
