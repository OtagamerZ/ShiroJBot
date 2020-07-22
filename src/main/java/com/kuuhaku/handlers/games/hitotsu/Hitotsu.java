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

package com.kuuhaku.handlers.games.hitotsu;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.framework.Tabletop;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Hitotsu extends Tabletop {
	private final Seats seats = new Seats();
	private final List<KawaiponCard> available = new ArrayList<>();
	private final LinkedList<KawaiponCard> played = new LinkedList<>();
	private final GameDeque<KawaiponCard> deque = new GameDeque<>(this);
	private BufferedImage mount = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	private Future<?> timeout;
	private Message message = null;

	public Hitotsu(TextChannel table, String id, User... players) {
		super(table, null, id, players);
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
	public void execute(int bet) {
		getPlayers().lastOneStarts();
		seats.get(getPlayers().getCurrent().getId()).showHand();
		message = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " você começa!").complete();
		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			{
				refresh(this);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				User u = event.getAuthor();
				TextChannel chn = event.getChannel();
				Message m = event.getMessage();

				System.out.println(
						"\nCHN: " + chn + " | Table: " + getTable() +
								"\nU: " + u + " | Current: " + getPlayers().getCurrent()
				);
				if (chn.getId().equals(getTable().getId()) && u.getId().equals(getPlayers().getCurrent().getId())) {
					try {
						if (StringUtils.deleteWhitespace(m.getContentRaw()).split(",").length > 1) {
							int[] digits = Arrays.stream(StringUtils.deleteWhitespace(m.getContentRaw()).split(",")).mapToInt(Integer::parseInt).toArray();

							if (handleChain(digits)) {
								declareWinner();
								return;
							}
							refresh(this);
						} else if (StringUtils.isNumeric(m.getContentRaw())) {
							if (handle(Integer.parseInt(m.getContentRaw()))) {
								declareWinner();
								return;
							}
							refresh(this);
						} else if (Helper.equalsAny(m.getContentRaw(), "comprar", "buy")) {
							seats.get(getPlayers().getCurrent().getId()).draw(getDeque());

							if (message != null) message.delete().queue();
							message = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " passou a vez, agora é você " + getPlayers().nextTurn().getAsMention() + ".")
									.addFile(Helper.getBytes(mount, "png"), "mount.png")
									.complete();

							seats.get(getPlayers().getCurrent().getId()).showHand();
							refresh(this);
						} else if (Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender")) {
							getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " desistiu!").queue();
							getPlayers().setLoser();
							getPlayers().nextTurn();

							if (getPlayers().getPlayers().size() == 1) {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
								getPlayers().setWinner();
								awardWinner(bet);
								getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!!").queue();
								timeout.cancel(true);
							} else {
								message = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " agora é sua vez.").complete();
								seats.get(getPlayers().getCurrent().getId()).showHand();
							}
						} else if (Helper.equalsAny(m.getContentRaw(), "lista", "cartas", "list", "cards")) {
							EmbedBuilder eb = new EmbedBuilder();
							StringBuilder sb = new StringBuilder();
							List<KawaiponCard> cards = seats.get(getPlayers().getCurrent()).getCards();

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
								eb.setFooter("Carta atual: " + played.getLast().getName() + " (" + played.getLast().getCard().getAnime() + ")");

							getPlayers().getCurrent().openPrivateChannel().complete().sendMessage(eb.build()).queue();
						}
					} catch (IllegalCardException e) {
						getTable().sendMessage(":x: | Você só pode jogar uma carta que seja do mesmo anime ou da mesma raridade.").queue();
					} catch (IndexOutOfBoundsException e) {
						getTable().sendMessage(":x: | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão..").queue();
					} catch (NumberFormatException | IllegalChainException e) {
						getTable().sendMessage(":x: | Para executar uma corrente você deve informar 2 ou mais índices de cartas do mesmo anime separados por vírgula.").queue();
					}
				}
			}

			private void declareWinner() {
				Main.getInfo().getAPI().removeEventListener(this);
				ShiroInfo.getGames().remove(getId());
				justShow();
				getTable().sendMessage("Não restam mais cartas para " + getPlayers().getWinner().getAsMention() + ", temos um vencedor!!").queue();
				timeout.cancel(true);
				awardWinner(bet);
			}
		});
	}

	public boolean handle(int card) throws IllegalCardException {
		Hand hand = seats.get(getPlayers().getCurrent().getId());
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
			CardEffect.getEffect(c.getCard().getRarity()).accept(this, seats.get(getPlayers().peekNext().getId()));

		getPlayers().setWinner(seats.values().stream().filter(h -> h.getCards().size() == 0).map(Hand::getUser).findFirst().orElse(null));
		if (getPlayers().getWinner() != null) return true;

		if (deque.size() == 0) shuffle();
		getPlayers().nextTurn();
		seats.get(getPlayers().getCurrent().getId()).showHand();
		putAndShow(c);
		return false;
	}

	public boolean handleChain(int[] card) throws IllegalCardException {
		Hand hand = seats.get(getPlayers().getCurrent().getId());
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
				CardEffect.getEffect(cd.getCard().getRarity()).accept(this, seats.get(getPlayers().peekNext().getId()));
		});

		getPlayers().setWinner(seats.values().stream().filter(h -> h.getCards().size() == 0).map(Hand::getUser).findFirst().orElse(null));
		if (getPlayers().getWinner() != null) return true;

		if (deque.size() == 0) shuffle();
		getPlayers().nextTurn();
		seats.get(getPlayers().getCurrent().getId()).showHand();
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
		message = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " agora é sua vez.").addFile(Helper.getBytes(mount, "png"), "mount.png").complete();
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
		message = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " agora é sua vez.").addFile(Helper.getBytes(mount, "png"), "mount.png").complete();
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

	private void refresh(Object listener) {
		if (timeout != null && !timeout.isCancelled()) timeout.cancel(true);
		timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
			Main.getInfo().getAPI().removeEventListener(listener);
			ShiroInfo.getGames().remove(getId());
		}, Helper::doNothing);
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
}
