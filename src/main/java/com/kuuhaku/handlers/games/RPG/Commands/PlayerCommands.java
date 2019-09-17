package com.kuuhaku.handlers.games.RPG.Commands;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Entities.Equipped;
import com.kuuhaku.handlers.games.RPG.Exceptions.NoSlotAvailableException;
import com.kuuhaku.handlers.games.RPG.Exceptions.UnknownItemException;
import com.kuuhaku.handlers.games.RPG.Handlers.PlayerRegisterHandler;
import com.kuuhaku.handlers.games.RPG.Handlers.PvPHandler;
import com.kuuhaku.handlers.games.RPG.Utils;
import com.kuuhaku.handlers.games.RPG.World.Map;
import com.kuuhaku.handlers.games.RPG.World.World;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PlayerCommands implements RPGCommand {
	@Override
	public void execute(Map map, JDA jda, User player, World world, TextChannel channel, Message msg, String command, String[] args) {
		String text = msg.getContentRaw();
		if (text.startsWith("-")) {
			msg.delete().queue(s -> speak(world, channel, player, text));
		}

		try {
			switch (command) {
				case "duelar":
				case "atacar":
					attack(jda, channel, msg, player);
					break;
				case "registrar":
				case "criar":
					register(map, jda, player, channel);
					msg.delete().queue();
					return;
				case "mover":
				case "ir":
					move(map, player, channel, world, args[0].toCharArray());
					msg.delete().queue();
					return;
				case "entrar":
					enterMap(world, player);
					break;
				case "inventario":
				case "bolsa":
					inventory(player, channel, world);
					msg.delete().queue();
					return;
				case "itens":
					bag(player, channel, world);
					msg.delete().queue();
					return;
				case "perfil":
					profile(player, channel, world);
					msg.delete().queue();
					return;
				case "analisar":
				case "ver":
					view(player, channel, world, String.join(" ", args));
					msg.delete().queue();
					return;
				case "equipar":
				case "usar":
					equip(player, channel, world, String.join(" ", args));
					msg.delete().queue();
					return;
				case "dar":
					giveItem(player, world, channel, args, msg);
					msg.delete().queue();
					break;
				case "desequipar":
				case "guardar":
					unequip(player, channel, world, String.join(" ", args));
					msg.delete().queue();
					return;
				case "mapa":
					map(channel, world);
					msg.delete().queue();
					return;
				case "rolar":
					speak(world, channel, player, Utils.rollDice(String.join(" ", args), world.getPlayers().get(player.getId()).getCharacter().getStatus()));
					msg.delete().queue();
			}
		} catch (IOException | FontFormatException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage(":x: | Você precisa especificar o nome do item.").queue();
		} catch (UnknownItemException e) {
			channel.sendMessage(":x: | Item desconhecido.").queue();
		}
	}

	private void register(Map map, JDA jda, User player, TextChannel channel) {
		new PlayerRegisterHandler(map, channel, jda, player);
	}

	private void move(Map map, User u, TextChannel channel, World world, char[] coord) throws IOException {
		world.getPlayers().get(u.getId()).move(map, Utils.coordToArray(coord[0], coord[1]));
		world.render(channel).queue();
	}

	private void profile(User u, TextChannel channel, World world) {
		world.getPlayers().get(u.getId()).getCharacter().openProfile(channel).queue();
	}

	private void inventory(User u, TextChannel channel, World world) throws IOException, FontFormatException {
		world.getPlayers().get(u.getId()).getCharacter().openNiceInventory(channel).queue();
	}

	private void bag(User u, TextChannel channel, World world) {
		world.getPlayers().get(u.getId()).getCharacter().openInventory(channel).queue();
	}

	private void view(User u, TextChannel channel, World world, String name) throws UnknownItemException {
		world.getPlayers().get(u.getId()).getCharacter().getInventory().getItems().stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst().orElseThrow(UnknownItemException::new).info(channel).queue();
	}

	private void equip(User u, TextChannel channel, World world, String name) throws UnknownItemException, NoSlotAvailableException {
		Equipped inv = world.getPlayers().get(u.getId()).getCharacter().getInventory();
		inv.equip(inv.getItem(name));
		channel.sendMessage("Item " + name + " equipado.").queue();
	}

	private void unequip(User u, TextChannel channel, World world, String name) throws UnknownItemException, NoSlotAvailableException {
		Equipped inv = world.getPlayers().get(u.getId()).getCharacter().getInventory();
		inv.unequip(inv.getItem(name));
		channel.sendMessage("Item " + name + " desequipado.").queue();
	}

	private void map(TextChannel channel, World world) throws IOException {
		world.render(channel).queue();
	}

	private WebhookClient getClient(TextChannel ch, Guild s) throws ExecutionException, InterruptedException {
		List<Webhook> wbs = ch.retrieveWebhooks().submit().get().stream().filter(w -> w.getOwner() == s.getSelfMember()).collect(Collectors.toList());
		if (wbs.size() != 0) {
			WebhookClientBuilder wcb = new WebhookClientBuilder(wbs.get(0).getUrl());
			return wcb.build();
		} else {
			WebhookClientBuilder wcb = new WebhookClientBuilder(Objects.requireNonNull(Helper.getOrCreateWebhook(ch)).getUrl());
			return wcb.build();
		}
	}

	private void speak(World world, TextChannel channel, User u, String text) {
		try {
			Actor.Player character = world.getPlayers().get(u.getId());
			WebhookClient client = getClient(channel, channel.getGuild());

			WebhookMessageBuilder wmb = new WebhookMessageBuilder();
			wmb.setUsername(character.getCharacter().getName());
			wmb.setAvatarUrl(character.getCharacter().getImage());
			wmb.setContent("*" + text.substring(1) + "*");

			client.send(wmb.build()).get();
			client.close();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void enterMap(World world, User player) {
		world.getPlayers().get(player.getId()).toMap(world.getCurrentMap());
	}

	private void giveItem(User u, World world, TextChannel channel, String[] args, Message msg) throws UnknownItemException {
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

		Equipped selfInv = world.getPlayers().get(u.getId()).getCharacter().getInventory();
		Equipped targetInv = world.getPlayers().get(msg.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
		if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			selfInv.addGold(-Integer.parseInt(args[2]));
			targetInv.addGold(Integer.parseInt(args[2]));
		} else {
			selfInv.removeItem(selfInv.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
			targetInv.addItem(world.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
		}
	}

	private void attack(JDA jda, TextChannel channel, Message msg, User p1) {
		if (msg.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa especificar um usuário para duelar").queue();
			return;
		}
		Actor.Player player = Main.getInfo().getGames().get(msg.getGuild().getId()).getPlayers().get(p1.getId());
		Actor.Player target = Main.getInfo().getGames().get(msg.getGuild().getId()).getPlayers().get(msg.getMentionedUsers().get(0).getId());

		if (player.getPos() != target.getPos()) {
			channel.sendMessage(":x: | Este jogador não está na mesma área que você").queue();
			return;
		}

		new PvPHandler(jda, channel, player, target);
	}
}
