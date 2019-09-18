package com.kuuhaku.handlers.games.RPG.Handlers;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.RPG.Actors.Actor;
import com.kuuhaku.handlers.games.RPG.Entities.Character;
import com.kuuhaku.handlers.games.RPG.Exceptions.NameTakenException;
import com.kuuhaku.handlers.games.RPG.World.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class PlayerRegisterHandler extends ListenerAdapter {
	private final TextChannel channel;
	private final Map map;
	private final JDA jda;
	private final User user;
	private String name = "";
	private String image = "";
	private String desc = "";
	private int str = 0;
	private int per = 0;
	private int end = 0;
	private int cha = 0;
	private int intl = 0;
	private int agl = 0;
	private int lck = 0;
	private int page = 0;
	private final EmbedBuilder eb = new EmbedBuilder();
	private Message msg;
	private final TextChannel chn;
	private final boolean[] complete = new boolean[]{false, false, false, false};

	private static final String PREVIOUS = "\u25C0";
	private static final String CANCEL = "\u274E";
	private static final String NEXT = "\u25B6";
	private static final String ACCEPT = "\u2705";

	public PlayerRegisterHandler(Map map, TextChannel channel, JDA jda, User user) {
		this.channel = channel;
		this.jda = jda;
		this.user = user;
		this.map = map;
		this.chn = channel;
		eb.setTitle("Registro de pesonagem de " + user.getName());
		eb.setDescription("Clique nas setas para mudar as páginas.");
		channel.sendMessage(eb.build()).queue(m -> {
			msg = m;
			msg.addReaction(CANCEL).queue();
			msg.addReaction(NEXT).queue();
		});
		jda.addEventListener(this);
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor() != user || event.getChannel() != chn) return;
		try {
			switch (page) {
				case 0:
					msg.addReaction(NEXT).queue();
					render(msg);
					break;
				case 1:
					if (Main.getInfo().getGames().get(event.getGuild().getId()).getPlayers().values().stream().anyMatch(p -> p.getCharacter().getName().equals(event.getMessage().getContentRaw()))) {
						channel.sendMessage(":x: | Este nome já está em uso").queue();
						return;
					}
					name = event.getMessage().getContentRaw();
					event.getChannel().sendMessage("Nome trocado para **" + event.getMessage().getContentRaw() + "**!").queue();
					complete[0] = true;
					render(msg);
					break;
				case 2:
					image = event.getMessage().getContentRaw();
					try {
						HttpURLConnection con = (HttpURLConnection) new URL(image).openConnection();
						con.setRequestProperty("User-Agent", "Mozilla/5.0");
						BufferedImage map = ImageIO.read(con.getInputStream());

						event.getChannel().sendMessage("Imagem trocada com sucesso!").queue();
						complete[1] = true;
						render(msg);
					} catch (IOException e) {
						e.printStackTrace();
						event.getChannel().sendMessage(":x: | Imagem inválida, veja se pegou o link corretamente.").queue();
					}
					break;
				case 3:
					desc = event.getMessage().getContentRaw();
					event.getChannel().sendMessage("Descrição trocada com sucesso!").queue();
					complete[2] = true;
					render(msg);
					break;
				case 4:
					String[] args = event.getMessage().getContentRaw().split(";");
					int[] stats = Arrays.stream(args).mapToInt(Integer::parseInt).toArray();

					if (Arrays.stream(stats).sum() > 10) throw new IllegalArgumentException();

					str = stats[0];
					per = stats[1];
					end = stats[2];
					cha = stats[3];
					intl = stats[4];
					agl = stats[5];
					lck = stats[6];
					complete[3] = true;
					render(msg);
					event.getChannel().sendMessage("Registro completo!\nConfirme o cadastro na mensagem inicial ou use os botões para alterar valores anteriores!").queue();
					break;
			}
		} catch (NameTakenException e) {
			event.getChannel().sendMessage(":x: | Este nome já está em uso!").queue();
		} catch (NumberFormatException e) {
			event.getChannel().sendMessage(":x: | Os atributos devem estar no formato:\n `FORÇA;PERCEPÇÃO;RESISTÊNCIA;CARISMA;INTELIGENCIA;AGILIDADE;SORTE`.").queue();
		} catch (IllegalArgumentException e) {
			event.getChannel().sendMessage(":x: | Você pode atribuir no máximo 10 pontos de atributo.").queue();
		}
		event.getMessage().delete().queue();
	}

	@Override
	public void onGenericGuildMessageReaction(GenericGuildMessageReactionEvent event) {
		if (event.getUser().isBot() || event.getUser() != user || event.getChannel() != chn) return;
		switch (event.getReactionEmote().getName()) {
			case CANCEL:
				channel.sendMessage("Registro abortado!").queue();
				jda.removeEventListener(this);
				msg.delete().queue();
				break;
			case ACCEPT:
				try {
					Main.getInfo().getGames().get(event.getGuild().getId()).addPlayer(new Actor.Player(map, user, new Character(name, image, desc, str, per, end, cha, intl, agl, lck)));
					jda.removeEventListener(this);
					channel.sendMessage("Registrado com sucesso!").queue();
				} catch (IOException ignore) {
				}
				msg.clearReactions().queue();
				break;
			case PREVIOUS:
				page--;
				render(msg);
				break;
			case NEXT:
				page++;
				render(msg);
				break;
		}
	}

	private void render(Message msg) {
		try {
			msg.clearReactions().complete();
			Thread.sleep(500);
			eb.clear();
			eb.setTitle("Registro de pesonagem de " + user.getName());
			switch (page) {
				case 0:
				case 1:
					eb.setDescription("Digite um nome");
					msg.addReaction(CANCEL).queue(s -> {
						if (complete[0]) msg.addReaction(NEXT).queue();
					});
					break;
				case 2:
					eb.setDescription("Escolha uma imagem");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[1]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 3:
					eb.setDescription("Digite uma descrição");
					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[2]) msg.addReaction(NEXT).queue();
							}));
					break;
				case 4:
					eb.setDescription("Digite os atributos no seguinte formato:\n`FORÇA`;`PERCEPÇÃO`;`RESISTENCIA`;`CARISMA`;`INTELIGÊNCIA`;`AGILIDADE`;`SORTE`");

					eb.addField("Pontos restantes: " + (10 - (str + per + end + cha + intl + agl + lck)), "", false);
					eb.addField("Força: " + str, "", true);
					eb.addField("Percepção: " + per, "", true);
					eb.addField("Resistência: " + end, "", true);
					eb.addField("Carisma: " + cha, "", true);
					eb.addField("Inteligência: " + intl, "", true);
					eb.addField("Agilidade: " + agl, "", true);
					eb.addField("Sorte: " + lck, "", true);

					msg.addReaction(PREVIOUS).queue(s ->
							msg.addReaction(CANCEL).queue(s1 -> {
								if (complete[3]) msg.addReaction(ACCEPT).queue();
							}));
					break;
			}
			if (complete[3]) {
				msg.editMessage(user.getAsMention()).embed(eb.build()).queue();
			} else {
				msg.editMessage(eb.build()).queue();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
