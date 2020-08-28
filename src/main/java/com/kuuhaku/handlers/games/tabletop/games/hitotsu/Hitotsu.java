/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.games.tabletop.games.hitotsu;

import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Hitotsu extends Game {
	private final Seats seats = new Seats();
	private final List<KawaiponCard> available = new ArrayList<>();
	private final LinkedList<KawaiponCard> played = new LinkedList<>();
	private final GameDeque<KawaiponCard> deque = new GameDeque<>(this);
	private final TextChannel channel;
	private final ListenerAdapter listener = new ListenerAdapter() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private BufferedImage mount = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	private Message message = null;
	private boolean suddenDeath = false;

	public Hitotsu(JDA handler, TextChannel channel, int bet, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;

		setActions(
				s -> close(),
				s -> {
					getBoard().leaveGame();
					resetTimer();
					if (getBoard().getInGamePlayers().size() == 1) {
						getBoard().awardWinner(this, getCurrent().getId());
						channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)").queue();
						close();
					}
				}
		);

		for (User u : players) {
			Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
			available.addAll(kp.getCards());
		}

		Collections.shuffle(available);
		deque.addAll(available);

		for (User u : players) {
			seats.put(new Hand(u, deque));
		}
	}

	@Override
	public void start() {
		message = channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)").complete();
		getHandler().addEventListener(listener);
		seats.get(getCurrent().getId()).showHand();
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.test(evt);
	}

	@Override
	public void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String command = message.getContentRaw();

		try {
			if (StringUtils.deleteWhitespace(command).split(",").length > 1) {
				int[] digits = Arrays.stream(StringUtils.deleteWhitespace(command).split(",")).mapToInt(Integer::parseInt).toArray();

				if (handleChain(digits)) {
					declareWinner();
				}
			} else if (StringUtils.isNumeric(command)) {
				if (handle(Integer.parseInt(command))) {
					declareWinner();
				}
			} else if (Helper.equalsAny(command, "comprar", "buy")) {
				seats.get(getCurrent().getId()).draw(getDeque());

				resetTimer();
				if (this.message != null) message.delete().queue();
				this.message = channel.sendMessage(Objects.requireNonNull(evt.getJDA().getUserById(getBoard().getInGamePlayers().peekLast().getId())).getAsMention() + " passou a vez, agora é você " + getCurrent().getAsMention() + ".")
						.addFile(Helper.getBytes(mount, "png"), "mount.png")
						.complete();

				seats.get(getCurrent().getId()).showHand();
			} else if (Helper.equalsAny(command, "desistir", "forfeit", "ff", "surrender")) {
				channel.sendMessage(getCurrent().getAsMention() + " desistiu!").queue();
				getBoard().leaveGame();
				resetTimer();

				if (getBoard().getInGamePlayers().size() == 1) {
					getBoard().awardWinner(this, getCurrent().getId());
					channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)").queue();
					close();
				} else {
					this.message = channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : "")).complete();
					seats.get(getCurrent().getId()).showHand();
				}
			} else if (Helper.equalsAny(command, "lista", "cartas", "list", "cards")) {
				EmbedBuilder eb = new EmbedBuilder();
				StringBuilder sb = new StringBuilder();
				List<KawaiponCard> cards = seats.get(getCurrent().getId()).getCards();

				eb.setTitle("Suas cartas");
				for (int i = 0; i < cards.size(); i++) {
					sb.append("**")
							.append(i)
							.append("** - ")
							.append("(")
							.append(cards.get(i).getCard().getAnime().toString())
							.append(")")
							.append(cards.get(i).getCard().getRarity().getEmote())
							.append(cards.get(i).getName())
							.append("\n");
				}
				eb.setDescription(sb.toString());
				if (played.size() > 0)
					eb.addField("Carta atual", "(" + played.getLast().getCard().getAnime() + ")" + played.getLast().getCard().getRarity().getEmote() + played.getLast().getName(), false);

				getCurrent().openPrivateChannel().complete().sendMessage(eb.build()).queue();
			}
		} catch (IllegalCardException e) {
			channel.sendMessage("❌ | Você só pode jogar uma carta que seja do mesmo anime ou da mesma raridade.").queue();
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão..").queue();
		} catch (NumberFormatException | IllegalChainException e) {
			channel.sendMessage("❌ | Para executar uma corrente você deve informar 2 ou mais índices de cartas do mesmo anime separados por vírgula.").queue();
		} catch (NoSuchElementException e) {
			int lowest = 999;
			for (Hand h : seats.values()) {
				lowest = Math.min(lowest, h.getCards().size());
			}

			int finalLowest = lowest;
			List<Hand> winners = seats.values().stream().filter(h -> h.getCards().size() <= finalLowest).collect(Collectors.toList());

			if (winners.size() == 1) {
				Hand h = winners.get(0);
				channel.sendMessage(h.getUser().getAsMention() + " é o jogador que possui menos cartas, temos um vencedor!! (" + getRound() + " turnos)").queue();
				getBoard().awardWinners(this, h.getUser().getId());
				close();
			} else if (winners.size() != getBoard().getPlayers().size()) {
				channel.sendMessage(String.join(", ", winners.stream().map(h -> h.getUser().getAsMention()).toArray(String[]::new)) + " são os jogadores que possuem menos cartas, temos " + winners.size() + " vencedores!! (" + getRound() + " turnos)").queue();
				getBoard().awardWinners(this, winners.stream().map(h -> h.getUser().getId()).toArray(String[]::new));
				close();
			} else {
				channel.sendMessage("Temos um empate! (" + getRound() + " turnos)").queue();
				close();
			}
		}
	}

	private void declareWinner() {
		justShow();
		channel.sendMessage("Não restam mais cartas para " + getCurrent().getAsMention() + ", temos um vencedor!! (" + getRound() + " turnos)").queue();
		getBoard().awardWinner(this, getCurrent().getId());
		close();
	}

	public boolean handle(int card) throws IllegalCardException {
		Hand hand = seats.get(getCurrent().getId());
		KawaiponCard c = hand.getCards().get(card);
		KawaiponCard lastest = played.peekLast();

		if (lastest != null) {
			boolean sameAnime = c.getCard().getAnime().equals(lastest.getCard().getAnime());
			boolean sameRarity = c.getCard().getRarity().equals(lastest.getCard().getRarity());
			if (!sameAnime && !sameRarity) throw new IllegalCardException();
		}

		played.add(c);
		hand.getCards().remove(card);
		if (c.isFoil())
			CardEffect.getEffect(c.getCard().getRarity()).accept(this, seats.get(getBoard().getInGamePlayers().peekNext().getId()));

		User winner = seats.values().stream().filter(h -> h.getCards().size() == 0).map(Hand::getUser).findFirst().orElse(null);
		if (winner != null) {
			getBoard().awardWinner(this, winner.getId());
			return true;
		}

		if (deque.size() == 0) {
			shuffle();
			suddenDeath = true;
		}
		resetTimer();
		seats.get(getCurrent().getId()).showHand();
		putAndShow(c);
		return false;
	}

	public boolean handleChain(int[] card) throws IllegalCardException {
		Hand hand = seats.get(getCurrent().getId());
		List<KawaiponCard> c = new ArrayList<>() {{
			for (int i : card) add(hand.getCards().get(i));
		}};
		KawaiponCard lastest = played.peekLast();

		if (lastest != null) {
			boolean sameAnime = c.get(0).getCard().getAnime().equals(lastest.getCard().getAnime());
			boolean sameRarity = c.get(0).getCard().getRarity().equals(lastest.getCard().getRarity());
			if (!sameAnime && !sameRarity) throw new IllegalCardException();
		}

		for (KawaiponCard cd : c)
			if (!c.get(0).getCard().getAnime().equals(cd.getCard().getAnime())) throw new IllegalChainException();

		c.forEach(cd -> {
			played.add(cd);
			justPut(cd);
			hand.getCards().remove(cd);
			if (cd.isFoil())
				CardEffect.getEffect(cd.getCard().getRarity()).accept(this, seats.get(getBoard().getInGamePlayers().peekNext().getId()));
		});

		User winner = seats.values().stream().filter(h -> h.getCards().size() == 0).map(Hand::getUser).findFirst().orElse(null);
		if (winner != null) {
			getBoard().awardWinner(this, winner.getId());
			return true;
		}

		if (deque.size() == 0) {
			shuffle();
			suddenDeath = true;
		}
		resetTimer();
		seats.get(getCurrent().getId()).showHand();
		justShow();
		return false;
	}

	public void putAndShow(KawaiponCard c) {
		Helper.darkenImage(0.5f, mount);

		BufferedImage card = c.getCard().drawCard(c.isFoil());
		Graphics2D g2d = mount.createGraphics();
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Math.random() * 90 - 45);
		g2d.dispose();

		if (message != null) message.delete().queue();
		message = channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : "")).addFile(Helper.getBytes(mount, "png"), "mount.png").complete();
	}

	public void justPut(KawaiponCard c) {
		Helper.darkenImage(0.5f, mount);

		BufferedImage card = c.getCard().drawCard(c.isFoil());
		Graphics2D g2d = mount.createGraphics();
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Math.random() * 90 - 45);
		g2d.dispose();
	}

	public void justShow() {
		if (message != null) message.delete().queue();
		message = channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : "")).addFile(Helper.getBytes(mount, "png"), "mount.png").complete();
	}

	public void shuffle() {
		KawaiponCard lastest = played.getLast();
		played.clear();
		played.add(lastest);
		deque.addAll(available);
		deque.remove(lastest);
		Collections.shuffle(deque);
		mount = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	}

	public Seats getSeats() {
		return seats;
	}

	public List<KawaiponCard> getAvailable() {
		return available;
	}

	public LinkedList<KawaiponCard> getPlayed() {
		return played;
	}

	public GameDeque<KawaiponCard> getDeque() {
		return deque;
	}

	public BufferedImage getMount() {
		return mount;
	}

	public TextChannel getChannel() {
		return channel;
	}

	@Override
	public void close() {
		super.close();
		getHandler().removeEventListener(listener);
	}
}
