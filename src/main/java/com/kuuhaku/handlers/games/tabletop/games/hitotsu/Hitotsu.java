/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Hitotsu extends Game {
	private final Seats seats = new Seats();
	private final List<KawaiponCard> available = new ArrayList<>();
	private final LinkedList<KawaiponCard> played = new LinkedList<>();
	private final GameDeque<KawaiponCard> deque = new GameDeque<>(this);
	private final TextChannel channel;
	private final SimpleMessageListener listener;
	private final BufferedImage mount = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	private Message message = null;
	private boolean suddenDeath = false;

	public Hitotsu(ShardManager handler, TextChannel channel, int bet, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;
		this.listener = new SimpleMessageListener(channel) {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (canInteract(event)) play(event);
			}
		};

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
							.queue(msg -> {
								if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							});
				},
				s -> {
					getBoard().leaveGame();
					resetTimer();

					if (getBoard().getInGamePlayers().size() == 1) {
						getBoard().awardWinner(this, getCurrent().getId());
						close();
						channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)")
								.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
								.queue(msg -> {
									if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
								});
					} else {
						channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : ""))
								.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
								.queue(msg -> {
									if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
									this.message = msg;
									seats.get(getCurrent().getId()).showHand();
									Pages.buttonize(msg, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								});
					}
				}
		);

		for (User u : players) {
			Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
			available.addAll(kp.getCards());
		}

		Map<AddedAnime, List<KawaiponCard>> animes = available.stream()
				.collect(Collectors.groupingBy(kc -> kc.getCard().getAnime()));

		List<KawaiponCard> inGame = animes.values().stream()
				.sorted(Comparator.<List<KawaiponCard>>comparingInt(List::size).reversed())
				.limit(5)
				.flatMap(l -> {
					Collections.shuffle(l);
					return l.stream().limit(25);
				})
				.toList();

		deque.addAll(inGame);
		if (deque.size() < 100) {
			Collections.shuffle(available);
			deque.addAll(available.subList(0, 100 - deque.size()));
		}

		Collections.shuffle(deque);

		for (User u : players) {
			seats.put(new Hand(u, deque));
		}
	}

	@Override
	public void start() {
		resetTimerKeepTurn();
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.queue(s -> {
					this.message = s;
					ShiroInfo.getShiroEvents().addHandler(channel.getGuild(), listener);
					seats.get(getCurrent().getId()).showHand();
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				});
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> isOpen())
				.test(evt);
	}

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
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
			}
		} catch (IllegalCardException e) {
			channel.sendMessage("❌ | Você só pode jogar uma carta que seja do mesmo anime ou da mesma raridade.").queue(null, Helper::doNothing);
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue(null, Helper::doNothing);
		} catch (NumberFormatException | IllegalChainException e) {
			channel.sendMessage("❌ | Para executar uma corrente você deve informar 2 ou mais índices de cartas do mesmo anime separados por vírgula.").queue(null, Helper::doNothing);
		} catch (NoSuchElementException e) {
			int lowest = 999;
			for (Hand h : seats.values()) {
				lowest = Math.min(lowest, h.getCards().size());
			}

			int finalLowest = lowest;
			List<Hand> winners = seats.values().stream().filter(h -> h.getCards().size() <= finalLowest).toList();

			if (winners.size() == 1) {
				Hand h = winners.get(0);
				channel.sendMessage(h.getUser().getAsMention() + " é o jogador que possui menos cartas, temos um vencedor!! (" + getRound() + " turnos)")
						.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
				getBoard().awardWinner(this, h.getUser().getId());
				close();
			} else if (winners.size() != getBoard().getPlayers().size()) {
				channel.sendMessage(String.join(", ", winners.stream().map(h -> h.getUser().getAsMention()).toArray(String[]::new)) + " são os jogadores que possuem menos cartas, temos " + winners.size() + " vencedores!! (" + getRound() + " turnos)")
						.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
				getBoard().awardWinners(this, winners.stream().map(h -> h.getUser().getId()).toArray(String[]::new));
				close();
			} else {
				close();
				channel.sendMessage("Temos um empate! (" + getRound() + " turnos)")
						.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			}
		}
	}

	private void declareWinner() {
		getBoard().awardWinner(this, getCurrent().getId());
		close();
		channel.sendMessage("Não restam mais cartas para " + getCurrent().getAsMention() + ", temos um vencedor!! (" + getRound() + " turnos)")
				.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
				.queue(msg -> {
					if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
				});
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

		User winner = seats.values().stream().filter(h -> h.getCards().isEmpty()).map(Hand::getUser).findFirst().orElse(null);
		if (winner != null) {
			return true;
		}

		if (deque.isEmpty()) {
			shuffle();
			suddenDeath = true;
		}
		resetTimer();
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

		for (KawaiponCard cd : c) {
			played.add(cd);
			justPut(cd);
			hand.getCards().remove(cd);
			if (cd.isFoil())
				CardEffect.getEffect(cd.getCard().getRarity()).accept(this, seats.get(getBoard().getInGamePlayers().peekNext().getId()));
		}

		User winner = seats.values().stream().filter(h -> h.getCards().isEmpty()).map(Hand::getUser).findFirst().orElse(null);
		if (winner != null) {
			return true;
		}

		if (deque.isEmpty()) {
			shuffle();
			suddenDeath = true;
		}
		resetTimer();
		channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : ""))
				.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
				.queue(s -> {
					if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
					this.message = s;
					seats.get(getCurrent().getId()).showHand();
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				});
		return false;
	}

	public void putAndShow(KawaiponCard c) {
		Helper.darkenImage(0.5f, mount);

		Graphics2D g2d = mount.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		BufferedImage card = c.getCard().drawCard(c.isFoil());
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Helper.rng(-45, 45));

		channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : ""))
				.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
				.queue(s -> {
					if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
					this.message = s;
					seats.get(getCurrent().getId()).showHand();
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				});

		g2d.dispose();
	}

	public void justPut(KawaiponCard c) {
		Helper.darkenImage(0.5f, mount);

		Graphics2D g2d = mount.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		BufferedImage card = c.getCard().drawCard(c.isFoil());
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Helper.rng(-45, 45));

		g2d.dispose();
	}

	public void shuffle() {
		KawaiponCard lastest = played.getLast();
		played.clear();
		played.add(lastest);
		deque.addAll(available);
		deque.remove(lastest);
		Collections.shuffle(deque);

		Graphics2D g2d = mount.createGraphics();

		g2d.setComposite(AlphaComposite.Clear);
		g2d.fillRect(0, 0, mount.getWidth(), mount.getHeight());
		g2d.setComposite(AlphaComposite.SrcOver);

		g2d.dispose();
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
	public Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons() {
		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		buttons.put(Helper.parseEmoji("\uD83D\uDCCB"), wrapper -> {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Suas cartas");

			if (played.size() > 0)
				eb.addField("Carta atual", "(" + played.getLast().getCard().getAnime().toString() + ")" + played.getLast().getCard().getRarity().getEmote() + played.getLast().getName(), false);

			List<Page> pages = new ArrayList<>();
			List<List<KawaiponCard>> chunks = Helper.chunkify(seats.get(getCurrent().getId()).getCards(), 5);

			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < chunks.size(); j++) {
				List<KawaiponCard> cards = chunks.get(j);
				sb.setLength(0);

				for (int i = 0; i < cards.size(); i++) {
					KawaiponCard kc = cards.get(i);
					sb.append("**%s** - (%s)%s%s\n".formatted(
							i + j * 5,
							kc.getCard().getAnime(),
							kc.getCard().getRarity().getEmote(),
							kc.getName()
					));
				}

				eb.setDescription(sb.toString());
				pages.add(new InteractPage(eb.build()));
			}

			wrapper.getUser().openPrivateChannel()
					.flatMap(s -> s.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()))
					.queue(s -> Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES));
		});
		buttons.put(Helper.parseEmoji("\uD83D\uDCE4"), wrapper -> {
			Card last = played.getLast().getCard();
			Hand h = seats.get(getCurrent().getId());
			if (h.getCards().stream().anyMatch(c -> c.getCard().getAnime().equals(last.getAnime()) || c.getCard().getRarity().equals(last.getRarity()))) {
				channel.sendMessage("❌ | Você não pode passar a vez se tiver uma carta válida.").queue(null, Helper::doNothing);
				return;
			}

			h.draw(getDeque());

			User u = getCurrent();
			resetTimer();
			channel.sendMessage(u.getName() + " passou a vez, agora é você " + getCurrent().getAsMention() + ".")
					.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
					.queue(s -> {
						if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						this.message = s;
						seats.get(getCurrent().getId()).showHand();
						Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					});
		});
		buttons.put(Helper.parseEmoji("\uD83C\uDFF3️"), wrapper -> {
			channel.sendMessage(getCurrent().getAsMention() + " desistiu!").queue(null, Helper::doNothing);
			getBoard().leaveGame();
			resetTimer();

			if (getBoard().getInGamePlayers().size() == 1) {
				getBoard().awardWinner(this, getCurrent().getId());
				close();
				channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)")
						.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			} else {
				channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez." + (suddenDeath ? " (MORTE SÚBITA | " + deque.size() + " cartas restantes)" : ""))
						.addFile(Helper.writeAndGet(mount, String.valueOf(this.hashCode()), "png"))
						.queue(s -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							this.message = s;
							seats.get(getCurrent().getId()).showHand();
							Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		});

		return buttons;
	}

	@Override
	public void close() {
		listener.close();
		super.close();
	}
}
