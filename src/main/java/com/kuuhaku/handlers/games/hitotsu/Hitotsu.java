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
import com.kuuhaku.handlers.games.tabletop.entity.Tabletop;
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
	private final Map<User, Hand> hands = new HashMap<>();
	private final List<KawaiponCard> available = new ArrayList<>();
	private final LinkedList<KawaiponCard> played = new LinkedList<>();
	private final GameDeque<KawaiponCard> deque = new GameDeque<>(this);
	private BufferedImage mount = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	private Future<?> timeout;
	private Message message = null;

	public Hitotsu(TextChannel table, String id, User... players) {
		super(table, null, id, players);
		Kawaipon kp1 = KawaiponDAO.getKawaipon(getPlayers().getUserSequence().getFirst().getId());
		Kawaipon kp2 = KawaiponDAO.getKawaipon(getPlayers().getUserSequence().getLast().getId());

		Set<KawaiponCard> uniques = new HashSet<>() {{
			addAll(kp1.getCards());
			addAll(kp2.getCards());
		}};
		available.addAll(uniques);
		Collections.shuffle(available);

		deque.addAll(available);

		this.hands.put(getPlayers().getUserSequence().getFirst(), new Hand(getPlayers().getUserSequence().getFirst(), deque));
		this.hands.put(getPlayers().getUserSequence().getLast(), new Hand(getPlayers().getUserSequence().getLast(), deque));
	}

	@Override
	public void execute(int bet) {
		next();
		message = getTable().sendMessage(getPlayers().getUserSequence().getFirst().getAsMention() + " você começa!").complete();
		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			{
				timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
					Main.getInfo().getAPI().removeEventListener(this);
					ShiroInfo.getGames().remove(getId());
				}, Helper::doNothing);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				User u = event.getAuthor();
				TextChannel chn = event.getChannel();
				Message m = event.getMessage();

				if (chn.getId().equals(getTable().getId()) && u.getId().equals(getPlayers().getUserSequence().getFirst().getId())) {
					try {
						if (m.getContentRaw().split(" ").length > 1) {
							String[] indexes = m.getContentRaw().split(" ");
							boolean valid = true;
							for (String i : indexes) {
								if (!StringUtils.isNumeric(i)) {
									valid = false;
									break;
								}
							}

							if (!valid) return;

							Integer[] idx = Arrays.stream(indexes).map(Integer::parseInt).toArray(Integer[]::new);
							valid = checkChain(idx);

							if (!valid) {
								getTable().sendMessage(":x: | Corrente inválida, para usar uma corrente digite os índices das cartas separados por espaço (apenas cartas do __**mesmo anime**__ são válidas para corrente).").queue();
								return;
							}

							int chainMax = Collections.max(List.of(idx));
							for (int i : idx) {
								if (handle(i, chainMax)) {
									Main.getInfo().getAPI().removeEventListener(this);
									ShiroInfo.getGames().remove(getId());
									getTable().sendMessage("Não restam mais cartas para " + getPlayers().getWinner().getAsMention() + ", temos um vencedor!!").queue();
									timeout.cancel(true);
								} 
							}
							timeout.cancel(true);
							timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
							}, Helper::doNothing);
						} else if (StringUtils.isNumeric(m.getContentRaw())) {
							if (handle(Integer.parseInt(m.getContentRaw()), Integer.parseInt(m.getContentRaw()))) {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
								getTable().sendMessage("Não restam mais cartas para " + getPlayers().getWinner().getAsMention() + ", temos um vencedor!!").queue();
								timeout.cancel(true);
							}
							timeout.cancel(true);
							timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
							}, Helper::doNothing);
						} else if (Helper.equalsAny(m.getContentRaw(), "comprar", "buy")) {
							hands.get(getPlayers().getUserSequence().getFirst()).draw(getDeque());
							if (message != null) message.delete().queue();
							message = getTable().sendMessage(getPlayers().getUserSequence().getFirst().getAsMention() + " passou a vez, agora é você " + getPlayers().getUserSequence().getLast().getAsMention() + ".")
									.addFile(Helper.getBytes(mount, "png"), "mount.png")
									.complete();
							next();
							timeout.cancel(true);
							timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
							}, Helper::doNothing);
						} else if (Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender")) {
							Main.getInfo().getAPI().removeEventListener(this);
							ShiroInfo.getGames().remove(getId());
							getTable().sendMessage(getPlayers().getUserSequence().getFirst().getAsMention() + " desistiu!").queue();
							timeout.cancel(true);
						} else if (Helper.equalsAny(m.getContentRaw(), "lista", "cartas", "list", "cards")) {
							EmbedBuilder eb = new EmbedBuilder();
							StringBuilder sb = new StringBuilder();
							List<KawaiponCard> cards = hands.get(getPlayers().getUserSequence().getFirst()).getCards();

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

							getPlayers().getUserSequence().getFirst().openPrivateChannel().complete().sendMessage(eb.build()).queue();
						}
					} catch (IllegalCardException e) {
						getTable().sendMessage(":x: | Você só pode jogar uma carta que seja do mesmo anime ou da mesma raridade.").queue();
					} catch (IndexOutOfBoundsException e) {
						getTable().sendMessage(":x: | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão..").queue();
					}
				}
			}
		});
	}

	public boolean handle(int card, int chainMax) throws IllegalCardException {
		Hand hand = hands.get(getPlayers().getUserSequence().getFirst());
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
			CardEffect.getEffect(c.getCard().getRarity()).accept(this, hands.get(getPlayers().getUserSequence().getLast()));

		hands.get(getPlayers().getUserSequence().getFirst()).getCards().add(card, null);
		if (card == chainMax) {
			hands.forEach((usr, hnd) -> hnd.getCards().removeIf(cd -> cd == null));
			getPlayers().setWinner(hands.values().stream().filter(h -> h.getCards().size() == 0).map(Hand::getUser).findFirst().orElse(null));
			if (getPlayers().getWinner() != null) return true;

			if (deque.size() == 0) shuffle();
			next();
			putAndShow(c);
		} else justPut(c);
		return false;
	}

	public void next() {
		getPlayers().nextTurn();
		hands.get(getPlayers().getUserSequence().getFirst()).showHand();
	}

	public void putAndShow(KawaiponCard c) {
		BufferedImage card = c.getCard().drawCard(c.isFoil());
		Graphics2D g2d = mount.createGraphics();
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Math.random() * 90 - 45);
		g2d.dispose();

		if (message != null) message.delete().queue();
		message = getTable().sendMessage(getPlayers().getUserSequence().getFirst().getAsMention() + " agora é sua vez.").addFile(Helper.getBytes(mount, "png"), "mount.png").complete();
	}

	public void justPut(KawaiponCard c) {
		BufferedImage card = c.getCard().drawCard(c.isFoil());
		Graphics2D g2d = mount.createGraphics();
		g2d.translate((mount.getWidth() / 2) - (card.getWidth() / 2), (mount.getHeight() / 2) - (card.getHeight() / 2));
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Helper.drawRotated(g2d, card, card.getWidth() / 2, card.getHeight() / 2, Math.random() * 90 - 45);
		g2d.dispose();
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

	private boolean checkChain(Integer[] cards) {
		List<KawaiponCard> hand = getHands().get(getPlayers().getUserSequence().getFirst()).getCards();
		KawaiponCard aux = hand.get(cards[0]);
		boolean valid = true;
		for (int i : cards) {
			if (!hand.get(i).getCard().getAnime().equals(aux.getCard().getAnime())) {
				valid = false;
				break;
			} 
		} 

		return valid;
	}

	public Map<User, Hand> getHands() {
		return hands;
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
