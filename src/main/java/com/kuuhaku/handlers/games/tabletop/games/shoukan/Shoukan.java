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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.framework.Player;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.states.GameState;
import com.kuuhaku.model.annotations.ExecTime;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.StaffType;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.model.records.TournamentMatch;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.websocket.DeploymentException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm.*;
import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class Shoukan extends GlobalGame {
	private final Map<Side, Hand> hands;
	private final Map<Side, Triple<Race, Boolean, Race>> combos;
	private final GameChannel channel;
	private final Arena arena;
	private final Rules rules;
	private final SimpleMessageListener listener;
	private final Map<String, Message> message = new HashMap<>();
	private final List<Champion> fusions = CardDAO.getFusions();
	private final boolean team;
	private final boolean record = false;
	private final Map<Side, EnumSet<Achievement>> achievements = new HashMap<>();
	private final Map<Achievement, JSONObject> achData = new HashMap<>();
	private final Map<Side, Map<Race, Integer>> summoned = Map.of(
			Side.TOP, new HashMap<>(),
			Side.BOTTOM, new HashMap<>()
	);
	private final Set<PersistentEffect> persistentEffects = new HashSet<>();
	private final TournamentMatch tourMatch;

	private Phase phase = Phase.PLAN;
	private boolean forfeit = true;
	private boolean draw = false;
	private int fusionLock = 0;
	private int spellLock = 0;
	private int effectLock = 0;
	private boolean reroll = true;
	private boolean moveLock = false;
	private final int[] synthCd = {0, 0};
	private final int[] undyingCd = {0, 0};

	private GameState oldState = null;

	@ExecTime
	public Shoukan(ShardManager handler, GameChannel channel, int bet, Rules rules, boolean daily, boolean ranked, boolean record, TournamentMatch match, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, new JSONObject(rules));
		this.rules = rules;
		this.channel = channel;
		this.listener = new SimpleMessageListener(channel) {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (canInteract(event)) play(event);
			}
		};
		this.team = players.length == 4;
		this.tourMatch = match;

		if (team) {
			List<Deck> kps = daily ?
					Collections.nCopies(4, Helper.getDailyDeck()) :
					List.of(
							KawaiponDAO.getDeck(players[2].getId()),
							KawaiponDAO.getDeck(players[0].getId()),
							KawaiponDAO.getDeck(players[3].getId()),
							KawaiponDAO.getDeck(players[1].getId())
					);

			this.hands = Map.of(
					Side.TOP, new TeamHand(this, List.of(players[2], players[0]), kps.subList(0, 2), Side.TOP),
					Side.BOTTOM, new TeamHand(this, List.of(players[3], players[1]), kps.subList(2, 4), Side.BOTTOM)
			);
		} else {
			Deck p1 = daily ? Helper.getDailyDeck() : KawaiponDAO.getDeck(players[0].getId());
			Deck p2 = daily ? Helper.getDailyDeck() : KawaiponDAO.getDeck(players[1].getId());

			this.hands = Map.of(
					Side.TOP, new Hand(this, players[0], p1, Side.TOP),
					Side.BOTTOM, new Hand(this, players[1], p2, Side.BOTTOM)
			);
		}

		this.arena = new Arena(this);
		this.combos = Map.of(
				Side.TOP, hands.get(Side.TOP).getCombo(),
				Side.BOTTOM, hands.get(Side.BOTTOM).getCombo()
		);

		if (rules.official() || tourMatch != null) {
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));
		} else {
			if (rules.arcade() == ArcadeMode.BLACKROCK) {
				Field f = CardDAO.getField(switch (Helper.rng(5)) {
					case 0 -> "THE_SKY_GATES";
					case 1 -> "THE_CUBE";
					case 2 -> "GREY_AREA";
					case 3 -> "BLACK_ROCK_BATTLEFIELD";
					case 4 -> "CHARIOTS_LAND";
					case 5 -> "DEAD_MASTERS_LAIR";
					default -> throw new IllegalStateException("Unexpected value: " + Helper.rng(5));
				});
				assert f != null;
				arena.setField(f);
			}

			if (StaffDAO.getUser(players[0].getId()).getType().isAllowed(StaffType.TESTER)) {
				for (Object o : rules.test()) {
					String id = String.valueOf(o).toUpperCase(Locale.ROOT);
					CardType type = CardDAO.identifyType(id);

					Drawable d = switch (type) {
						case SENSHI -> CardDAO.getChampion(id);
						case EVOGEAR -> CardDAO.getEquipment(id);
						case FIELD -> CardDAO.getField(id);
						default -> null;
					};
					if (d == null) continue;

					for (Hand h : hands.values()) {
						h.getCards().add(d.copy());
					}
				}
			}
		}

		if (ranked) {
			for (Map.Entry<Side, Hand> e : hands.entrySet()) {
				Set<Achievement> achs = e.getValue().getAcc().getAchievements();
				if (!achs.isEmpty())
					achievements.put(e.getKey(), EnumSet.complementOf(EnumSet.copyOf(achs)));
				else
					achievements.put(e.getKey(), EnumSet.allOf(Achievement.class));
			}
		}

		setActions(
				s -> {
					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return msg;
									})
							);
				},
				s -> {
					if (rules.official() || tourMatch != null) {
						if (ranked) {
							MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(getCurrent().getId());
							mmr.block(30, ChronoUnit.MINUTES);
							MatchMakingRatingDAO.saveMMR(mmr);
						}

						setWo();
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
					}

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return msg;
									})
							);
				}
		);
	}

	@Override
	public void start() {
		resetTimerKeepTurn();
		Hand h = hands.get(getCurrentSide());
		h.addMana(h.getManaPerTurn());
		if (!h.isSuppressed()) {
			if (h.getCombo().getLeft() == Race.DIVINITY) {
				h.addMana((int) Math.round(4 - h.getAvgCost()));
			}
		}

		AtomicBoolean shownHand = new AtomicBoolean(false);
		AtomicReference<String> previous = new AtomicReference<>("");
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
				.queue(s -> {
					this.message.put(s.getChannel().getId(), s);
					if (!s.getGuild().getId().equals(previous.get())) {
						previous.set(s.getGuild().getId());
						ShiroInfo.getShiroEvents().addHandler(s.getGuild(), listener);
						Main.getInfo().setGameInProgress(listener.mutex, getBoard().getPlayers().stream().map(Player::getId).toArray(String[]::new));
						Main.getInfo().getGameSlot().put(s.getChannel().getId(), listener.mutex);
					}
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					if (!shownHand.get()) {
						shownHand.set(true);
						h.showHand();
					}
				});

		oldState = new GameState(this);
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> channel.getChannels().parallelStream().anyMatch(g -> g != null && e.getChannel().getId().equals(g.getId()));

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> {
					String[] args = e.getMessage().getContentRaw().split(",");
					return (args.length > 0 && StringUtils.isNumeric(args[0])) || e.getMessage().getContentRaw().equalsIgnoreCase("reload");
				})
				.and(e -> isOpen())
				.and(e -> !moveLock)
				.test(evt);
	}

	public String summonCard(Champion card, Mode mode, int index) {
		Hand hand = hands.get(getCurrentSide());
		if (index > -1) {
			index--;
		}

		/* CHECK COST */
		{
			if (!hand.isNullMode() && (hand.getMana() < card.getMana())) {
				channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			} else if (card.getBlood() > 0 && hand.getHp() <= card.getBlood()) {
				channel.sendMessage("❌ | Você não tem HP suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			}
		}

		Champion copy = card.copy();
		/* CHECK ARGUMENTS */
		{
			if (index == -1) {
				channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
				return null;
			}
		}

		SlotColumn slot = getSlot(getCurrentSide(), index);
		if (slot.getTop() != null) {
			channel.sendMessage("❌ | Já existe um campeão nessa casa.").queue(null, Helper::doNothing);
			return null;
		} else if (slot.isUnavailable()) {
			channel.sendMessage("❌ | Essa casa está indisponível.").queue(null, Helper::doNothing);
			return null;
		}

		reroll = false;
		card.setAvailable(false);
		if (hand.isNullMode())
			hand.removeHp(card.getBaseStats() / 2);
		else
			hand.removeMana(card.getMana());
		hand.removeHp(card.getBlood());

		copy.setDefending(mode == Mode.DEFENSE || mode == Mode.FLIPPED, false);
		copy.setFlipped(mode == Mode.FLIPPED, false);
		slot.setTop(copy);

		summoned.get(getCurrentSide()).merge(copy.getRace(), 1, Integer::sum);

		return switch (mode) {
			case ATTACK -> "%s invocou %s em posição de ataque.".formatted(
					hand.getUser().getName(),
					copy.getName()
			);
			case DEFENSE -> "%s invocou %s em posição de defesa.".formatted(
					hand.getUser().getName(),
					copy.getName()
			);
			case FLIPPED -> "%s invocou uma carta virada para baixo.".formatted(
					hand.getUser().getName()
			);
			default -> null;
		};
	}

	public String summonCard(Equipment card, Integer[] targets) {
		Hand hand = hands.get(getCurrentSide());
		Arguments type = card.getArgType();
		int dest = Helper.getOr(targets[0], 0) - 1;
		int pos = Helper.getOr(targets[1], 0) - 1;

		/* CHECK COST */
		{
			if (!hand.isNullMode() && (hand.getMana() < card.getMana())) {
				channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			} else if (card.getBlood() > 0 && hand.getHp() <= card.getBlood()) {
				channel.sendMessage("❌ | Você não tem HP suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			}
		}

		Equipment copy = card.copy();
		/* CHECK ARGUMENTS */
		{
			if (dest == -1) {
				channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
				return null;
			}
		}

		SlotColumn slot;
		Pair<Champion, Integer> target;

		if (type == Arguments.ENEMY) {
			if (pos == -1) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta inimiga para equipar este evogear.").queue(null, Helper::doNothing);
				return null;
			}

			Champion t = getSlot(getNextSide(), pos).getTop();
			if (t == null) {
				channel.sendMessage("❌ | Não existe um campeão nessa casa.").queue(null, Helper::doNothing);
				return null;
			}

			target = Pair.of(t, pos);
			slot = getSlot(getNextSide(), dest);
		} else {
			if (pos == -1) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada para equipar este evogear.").queue(null, Helper::doNothing);
				return null;
			}

			Champion t = getSlot(getCurrentSide(), pos).getTop();
			if (t == null) {
				channel.sendMessage("❌ | Não existe um campeão nessa casa.").queue(null, Helper::doNothing);
				return null;
			}

			target = Pair.of(t, pos);
			slot = getSlot(getCurrentSide(), dest);
		}

		if (slot.getBottom() != null) {
			channel.sendMessage("❌ | Já existe um evogear nessa casa.").queue(null, Helper::doNothing);
			return null;
		} else if (slot.isUnavailable()) {
			channel.sendMessage("❌ | Essa casa está indisponível.").queue(null, Helper::doNothing);
			return null;
		}

		reroll = false;
		card.setAvailable(false);
		hand.removeMana(card.getMana());
		hand.removeHp(card.getBlood());

		copy.setFlipped(card.getCharms().contains(TRAP), false);
		slot.setBottom(copy);

		Champion t = target.getLeft();
		t.link(copy);

		if (copy.hasEffect()) {
			if (type == Arguments.ALLY) {
				copy.activate(hand, hands.get(getCurrentSide()), this, pos, -1);
			} else {
				copy.activate(hand, hands.get(getNextSide()), this, -1, pos);
			}
		}

		if (!copy.getCharms().isEmpty()) {
			if (Helper.containsAny(copy.getCharms(), TIMEWARP, DOUBLETAP)) {
				int uses = (int) Helper.getFibonacci(copy.getTier());

				activations:
				for (Charm charm : copy.getCharms()) {
					for (int i = 0; i < uses; i++) {
						if (!getSlot(getCurrentSide(), target.getRight()).getTop().equals(t)) {
							break activations;
						}

						switch (charm) {
							case TIMEWARP -> {
								if (t.hasEffect()) {
									t.getEffect(new EffectParameters(BEFORE_TURN, this, getCurrentSide(), pos, Duelists.of(t, pos, null, -1), channel));
									t.getEffect(new EffectParameters(AFTER_TURN, this, getCurrentSide(), pos, Duelists.of(t, pos, null, -1), channel));
								}
							}
							case DOUBLETAP -> {
								if (t.hasEffect()) {
									t.getEffect(new EffectParameters(ON_SUMMON, this, getCurrentSide(), pos, Duelists.of(t, pos, null, -1), channel));
								}
							}
						}

						t.unlockTrigger();
					}
				}
			}

			if (copy.getCharms().contains(CLONE)) {
				SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);

				if (sc != null) {
					t.removeAtk(Math.round(t.getAltAtk() * (1 - copy.getTier() * 0.25f)));
					t.removeDef(Math.round(t.getAltDef() * (1 - copy.getTier() * 0.25f)));

					Champion dp = t.copy();
					dp.getBonus().removeMana(dp.getMana() / 2);
					dp.setGravelocked(true);

					sc.setTop(dp);
				}
			}
		}

		return "%s equipou %s em %s.".formatted(
				hand.getUser().getName(),
				copy.isFlipped() ? "um evogear virado para baixo" : copy.getCard().getName(),
				t.isFlipped() ? "um campeão virado para baixo" : t.getName()
		);
	}

	public String castSpell(Equipment card, Integer[] targets) {
		Hand hand = hands.get(getCurrentSide());
		Arguments type = card.getArgType();
		int pos1 = Helper.getOr(targets[0], 0) - 1;
		int pos2 = Helper.getOr(targets[1], 0) - 1;

		/* CHECK COST */
		{
			if (!hand.isNullMode() && (hand.getMana() < card.getMana())) {
				channel.sendMessage("❌ | Você não tem mana suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			} else if (card.getBlood() > 0 && hand.getHp() <= card.getBlood()) {
				channel.sendMessage("❌ | Você não tem HP suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
				return null;
			}
		}

		Equipment copy = card.copy();
		/* CHECK ARGUMENTS */
		{
			if (spellLock > 0) {
				channel.sendMessage("❌ | Magias estão bloqueadas por mais " + spellLock + (spellLock == 1 ? " turno." : " turnos.")).queue(null, Helper::doNothing);
				return null;
			} else if (targets.length < type.getArgCount()) {
				channel.sendMessage(
						switch (type) {
							case ALLY -> "❌ | Esta magia requer um alvo aliado.";
							case ENEMY -> "❌ | Esta magia requer um alvo inimigo.";
							case BOTH -> "❌ | Esta magia requer um alvo aliado e um inimigo.";
							default -> "";
						}
				).queue(null, Helper::doNothing);
				return null;
			}
		}

		Pair<Champion, Integer> allyPos = null;
		Pair<Champion, Integer> enemyPos = null;

		switch (type) {
			case ALLY, ALLY_SLOT -> {
				if (pos1 == -1) {
					channel.sendMessage("❌ | Índice inválido, escolha um campeão aliado para usar esta magia.").queue(null, Helper::doNothing);
					return null;
				}

				Champion target = getSlot(getCurrentSide(), pos1).getTop();
				if (target == null && type != Arguments.ALLY_SLOT) {
					channel.sendMessage("❌ | Não existe um campeão no alvo aliado.").queue(null, Helper::doNothing);
					return null;
				}

				allyPos = Pair.of(target, pos1);
			}
			case ENEMY, ENEMY_SLOT -> {
				if (pos1 == -1) {
					channel.sendMessage("❌ | Índice inválido, escolha um campeão inimigo para usar esta magia.").queue(null, Helper::doNothing);
					return null;
				}

				Champion target = getSlot(getNextSide(), pos1).getTop();
				if (target == null && type != Arguments.ENEMY_SLOT) {
					channel.sendMessage("❌ | Não existe um campeão no alvo inimigo.").queue(null, Helper::doNothing);
					return null;
				}

				enemyPos = Pair.of(target, pos1);
			}
			case BOTH, BOTH_SLOT -> {
				if (pos1 == -1 || pos2 == -1) {
					channel.sendMessage("❌ | Índice inválido, escolha um campeão aliado e um inimigo para usar esta magia.").queue(null, Helper::doNothing);
					return null;
				}

				Champion target = getSlot(getCurrentSide(), pos1).getTop();
				if (target == null && type != Arguments.BOTH_SLOT) {
					channel.sendMessage("❌ | Não existe um campeão no alvo aliado.").queue(null, Helper::doNothing);
					return null;
				}

				allyPos = Pair.of(target, pos1);

				target = getSlot(getNextSide(), pos2).getTop();
				if (target == null) {
					channel.sendMessage("❌ | Não existe um campeão no alvo inimigo.").queue(null, Helper::doNothing);
					return null;
				}

				enemyPos = Pair.of(target, pos2);
			}
		}

		reroll = false;
		card.setAvailable(false);
		hand.removeMana(card.getMana());
		hand.removeHp(card.getBlood());

		switch (type) {
			case NONE -> copy.activate(hand, hands.get(getNextSide()), this, -1, -1);
			case ALLY, ALLY_SLOT -> copy.activate(hand, hands.get(getNextSide()), this, pos1, -1);
			case ENEMY, ENEMY_SLOT -> copy.activate(hand, hands.get(getNextSide()), this, -1, pos1);
			case BOTH, BOTH_SLOT -> copy.activate(hand, hands.get(getNextSide()), this, pos1, pos2);
		}

		if (card.canGoToGrave()) {
			if (card.getTier() >= 4)
				arena.getBanned().add(copy);
			else
				arena.getGraveyard().get(getCurrentSide()).add(copy);
		}

		return switch (type) {
			case NONE -> "%s usou a magia %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName()
			);
			case ALLY -> "%s usou a magia %s em %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					allyPos.getLeft().isFlipped() ? "um campeão virado para baixo" : allyPos.getLeft().getName()
			);
			case ALLY_SLOT -> "%s usou a magia %s na posição %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					allyPos.getRight() + 1
			);
			case ENEMY -> "%s usou a magia %s em %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					enemyPos.getLeft().isFlipped() ? "um campeão virado para baixo" : enemyPos.getLeft().getName()
			);
			case ENEMY_SLOT -> "%s usou a magia %s na posição %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					enemyPos.getRight() + 1
			);
			case BOTH -> "%s usou a magia %s em %s e %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					allyPos.getLeft().isFlipped() ? "um campeão virado para baixo" : allyPos.getLeft().getName(),
					enemyPos.getLeft().isFlipped() ? "um campeão virado para baixo" : enemyPos.getLeft().getName()
			);
			case BOTH_SLOT -> "%s usou a magia %s nas posições %s e %s.".formatted(
					hand.getUser().getName(),
					card.getCard().getName(),
					allyPos.getRight() + 1,
					enemyPos.getRight() + 1
			);
		};
	}

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = hands.get(getCurrentSide());

		if (cmd.equalsIgnoreCase("reload")) {
			reportEvent(h, message.getAuthor().getName() + " recriou a mensagem do jogo.", false, false);
			return;
		}

		String[] args = cmd.replace(" ", "").split(",");
		int index;
		try {
			index = Integer.parseInt(args[0]) - 1;
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão.").queue(null, Helper::doNothing);
			return;
		}

		if (phase == Phase.PLAN) {
			if (args.length == 1) {
				if (!Helper.between(index, 0, 5)) {
					channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
					return;
				}

				Champion c = getSlot(getCurrentSide(), index).getTop();

				if (c == null) {
					channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
					return;
				} else if (isSlotChanged(getCurrentSide(), index)) {
					channel.sendMessage("❌ | Você já mudou a postura dessa carta neste turno.").queue(null, Helper::doNothing);
					return;
				}

				String stt = switch (c.getStatus()) {
					case STASIS -> "❌ | Essa carta está inalvejável.";
					case STUNNED -> "❌ | Essa carta está atordoada.";
					case SLEEPING -> "❌ | Essa carta está dormindo.";
					default -> null;
				};

				if (stt != null) {
					channel.sendMessage(stt).queue(null, Helper::doNothing);
					return;
				}

				String msg;
				if (c.isFlipped()) {
					c.setFlipped(false);
					c.setDefending(true);
					msg = "Carta virada para cima em modo de defesa.";
				} else if (c.isDefending()) {
					c.setDefending(false);
					msg = "Carta trocada para modo de ataque.";
				} else {
					c.setDefending(true);
					msg = "Carta trocada para modo de defesa.";
				}

				setSlotChanged(getCurrentSide(), index, true);
				reportEvent(h, msg, true, false);
			} else {
				Drawable d = Helper.safeGet(h.getCards(), index);
				if (d == null) {
					channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue(null, Helper::doNothing);
					return;
				} else if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
					return;
				}

				if (args[1].equalsIgnoreCase("d") && args.length == 2) {
					h.getDiscardBatch().add(d.copy());
					d.setAvailable(false);

					if (makeFusion(h)) return;

					reportEvent(h, h.getUser().getName() + " descartou a carta " + d.getCard().getName() + ".", true, false);
					return;
				}

				String msg = null;
				if (d instanceof Equipment e) {
					boolean cast = args[1].equalsIgnoreCase("s");

					if (e.isSpell() && !cast) {
						channel.sendMessage("❌ | O segundo argumento deve ser `S` para usar magias.").queue(null, Helper::doNothing);
						return;
					} else if (!e.isSpell() && cast) {
						channel.sendMessage("❌ | O segundo argumento não deve ser `S` para usar equipamentos.").queue(null, Helper::doNothing);
						return;
					}

					if (cast) {
						msg = castSpell(e, new Integer[]{
								Helper.castGet(args, 2, Integer::parseInt),
								Helper.castGet(args, 3, Integer::parseInt)
						});
					} else {
						msg = summonCard(e, new Integer[]{
								Helper.castGet(args, 1, Integer::parseInt),
								Helper.castGet(args, 2, Integer::parseInt)
						});
					}
				} else if (d instanceof Champion c) {
					Mode mode = switch (args[1].toUpperCase(Locale.ROOT)) {
						case "A" -> Mode.ATTACK;
						case "D" -> Mode.DEFENSE;
						case "B" -> Mode.FLIPPED;
						default -> Mode.NONE;
					};

					if (mode == Mode.NONE) {
						channel.sendMessage("❌ | O segundo argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
						return;
					}

					msg = summonCard(c, mode, Helper.getOr(Helper.castGet(args, 2, Integer::parseInt), -1));
				} else if (d instanceof Field f) {
					Field copy = f.copy();
					if (!args[1].equalsIgnoreCase("f")) {
						channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					arena.setField(copy);
					msg = h.getUser().getName() + " invocou o campo " + copy.getCard().getName() + ".";
				}

				if (!makeFusion(h) && msg != null) {
					reportEvent(h, msg, true, false);
				}
			}
		} else {
			int target = Helper.getOr(Helper.castGet(args, 1, Integer::parseInt), 0) - 1;
			if (!Helper.between(index, 0, 5)) {
				channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
				return;
			} else if (args.length > 1 && !Helper.between(target, 0, 5)) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue(null, Helper::doNothing);
				return;
			}

			Champion ally = getSlot(getCurrentSide(), index).getTop();
			if (ally == null) {
				channel.sendMessage("❌ | Não existe um aliado nessa casa.").queue(null, Helper::doNothing);
				return;
			}

			String stt = switch (ally.getStatus()) {
				case FLIPPED -> "❌ | Você não pode atacar com cartas viradas para baixo.";
				case STASIS -> "❌ | Essa carta está inalvejável.";
				case STUNNED -> "❌ | Essa carta está atordoada.";
				case SLEEPING -> "❌ | Essa carta está dormindo.";
				case DEFENDING -> "❌ | Você não pode atacar com cartas em modo de defesa.";
				case UNAVAILABLE -> "❌ | Essa carta já atacou neste turno.";
				default -> null;
			};

			if (stt != null) {
				channel.sendMessage(stt).queue(null, Helper::doNothing);
				return;
			}

			if (target == -1) {
				if (arena.getSlots().get(getNextSide()).parallelStream().anyMatch(s -> s.getTop() != null)) {
					channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
					return;
				}

				Hand you = hands.get(getCurrentSide());
				Hand op = hands.get(getNextSide());

				int before = op.getHp();
				float fac = getRound() < 2 ? 0.5f : 1;

				if (op.getMana() > 0) {
					int toSteal = Math.min(op.getMana(), ally.getManaDrain());

					you.addMana(toSteal);
					op.removeMana(toSteal);
				}

				int power = ally.getFinAtk();
				op.removeHp(Math.round(power * fac));
				op.addBleeding(Math.round(ally.getBldAtk() * fac));
				if (undyingCd[you.getSide() == Side.TOP ? 1 : 0] == 5) {
					you.addHp((before - op.getHp()) / 10);
				}

				ally.setAvailable(false);
				int extra = Math.round(power * fac - power);
				reportEvent(h,
						"%s atacou diretamente, causando %s de dano!%s%s".formatted(
								ally.getName(),
								before - op.getHp(),
								getRound() < 2 ? " (dano reduzido por ser o 1º turno)" : "",
								extra > 0
										? " (dano direto aumentado em " + extra + ")"
										: extra < 0
										? " (dano direto reduzido em " + extra + ")"
										: ""
						)
						, true, false);
			} else {
				Champion enemy = getSlot(getNextSide(), target).getTop();
				if (enemy == null) {
					channel.sendMessage("❌ | Não existe um inimigo nessa casa.").queue(null, Helper::doNothing);
					return;
				}

				attack(new Source(ally, getCurrentSide(), index), new Target(enemy, getNextSide(), target));
			}
		}
	}

	private void reportEvent(Hand h, String msg, boolean resetTimer, boolean changeTurn) {
		Side[] sides = {getCurrentSide(), getNextSide()};
		for (int i = 0; i < 5; i++) {
			for (Side s : sides) {
				SlotColumn slot = arena.getSlots().get(s).get(i);
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				if (applyEffect(GAME_TICK, c, s, i, new Source(c, s, i))) return;

				Hand hd = getHands().get(s);
				int heroIndex = isHeroInField(s);
				if (heroIndex == -1 && hd.getHero() != null && c.getCard().getId().equals(hd.getUser().getId()) && c.getCard().getName().equals(hd.getHero().getName())) {
					c.setHero(hd.getHero());

					if (hd.getHero().getPerks().contains(Perk.TABULA_RASA)) {
						Iterator<Equipment> it = hd.getHero().getInventory().iterator();
						while (it.hasNext()) {
							Equipment e = it.next();
							SlotColumn sc = getFirstAvailableSlot(s, false);
							if (sc == null) break;

							sc.setBottom(e);
							c.link(e);
							it.remove();
						}
					}
				} else if (i != heroIndex) {
					c.setHero(null);
				}

				c.unlockTrigger();
			}
		}

		if (resetTimer) {
			resetTimerKeepTurn();
			applyEffect(GLOBAL_TICK, (Champion) null, getCurrentSide(), -1);
		}
		BufferedImage bi = arena.render(this, hands);
		AtomicBoolean shownHand = new AtomicBoolean(false);
		moveLock = true;
		channel.sendMessage(msg)
				.addFile(Helper.writeAndGet(bi, String.valueOf(this.hashCode()), "jpg"))
				.queue(s -> {
					this.message.compute(s.getChannel().getId(), (id, m) -> {
						if (m != null) m.delete().queue(null, Helper::doNothing);
						return s;
					});
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					moveLock = false;
					if (!shownHand.get() && h != null) {
						shownHand.set(true);
						h.showHand();
					}

					if (changeTurn && h != null) {
						for (int i = 0; i < 5; i++) {
							setSlotChanged(h.getSide(), i, false);
						}
					}
				});

		if (record) {
			try {
				getFrames().add(Helper.compress(Helper.atob(getArena().addHands(bi, hands.values()), "jpg")));
			} catch (IOException ignore) {
			}
		}
	}

	public void attack(Source source, Target target) {
		Champion atkr = source.card();
		Champion defr = target.card();

		if (defr.isStasis()) {
			channel.sendMessage("❌ | Você não pode atacar cartas inalvejáveis.").queue();
			return;
		} else if (atkr.isDuelling() && !atkr.getNemesis().equals(defr)) {
			channel.sendMessage("❌ | " + source + " só pode atacar " + atkr.getNemesis().getName() + " (DUELO).").queue();
			return;
		} else if (defr.isDuelling() && !defr.getNemesis().equals(atkr)) {
			channel.sendMessage("❌ | " + target + " só pode ser atacado por " + defr.getNemesis().getName() + " (DUELO).").queue();
			return;
		}

		if (atkr.isDefending() || !atkr.isAvailable()) return;
		atkr.setAvailable(false);

		/* PRE-ATTACK */
		{
			if (applyEffect(ATTACK_ASSIST, atkr.getAdjacent(Neighbor.LEFT), source.side(), source.index() - 1, source, target))
				return;
			if (applyEffect(ATTACK_ASSIST, atkr.getAdjacent(Neighbor.RIGHT), source.side(), source.index() + 1, source, target))
				return;
			if (applyEffect(ON_ATTACK, atkr, source.side(), source.index(), source, target)) return;

			if (atkr.getBonus().popFlag(atkr, Flag.SKIPCOMBAT)) {
				atkr.resetAttribs();
				defr.resetAttribs();

				if (applyEffect(POST_ATTACK, atkr, source.side(), target.index(), source, target)) return;

				reportEvent(null, "Cálculo de combate ignorado por efeito do atacante!", true, false);
				return;
			}
		}

		/* PRE-DEFENSE */
		{
			if (applyEffect(DEFENSE_ASSIST, defr.getAdjacent(Neighbor.LEFT), target.side(), target.index() - 1, source, target))
				return;
			if (applyEffect(DEFENSE_ASSIST, defr.getAdjacent(Neighbor.RIGHT), target.side(), target.index() + 1, source, target))
				return;
			if (applyEffect(ON_DEFEND, defr, target.side(), target.index(), source, target)) return;

			if (defr.getBonus().popFlag(defr, Flag.SKIPCOMBAT)) {
				if (defr.isFlipped()) {
					defr.setFlipped(false);
				}

				atkr.resetAttribs();
				defr.resetAttribs();

				if (applyEffect(POST_DEFENSE, defr, target.side(), target.index(), source, target)) return;

				reportEvent(null, "Cálculo de combate ignorado por efeito do defensor!", true, false);
				return;
			}
		}

		defr.setFlipped(false);

		float fac = defr.isSleeping() ? 1.25f : 1;

		int yPower = Math.round(atkr.getFinAtk() * fac);
		int hPower;
		if (defr.isDefending()) {
			if (defr.isFlipped()) {
				defr.setFlipped(false);
			}

			hPower = defr.getFinDef();
		} else {
			hPower = defr.getFinAtk();
		}

		int dodge = defr.getDodge(false);
		int block = defr.getBlock(false);
		boolean dodged = dodge >= 100 || (dodge > 0 && Helper.chance(dodge));
		boolean blocked = block >= 100 || (block > 0 && Helper.chance(block));

		atkr.resetAttribs();
		defr.resetAttribs();

		Hand you = hands.get(source.side());
		Hand op = hands.get(target.side());

		int yBefore = you.getHp();
		int hBefore = op.getHp();

		/* ATTACK SUCCESS */
		proc:
		if (yPower > hPower && !blocked) {
			if (dodged) {
				if (applyEffect(ON_MISS, atkr, source.side(), source.index(), source, target)) return;
				if (applyEffect(ON_DODGE, defr, target.side(), target.index(), source, target)) return;

				reportEvent(null, defr.getName() + " esquivou do ataque de " + atkr.getName() + "! (" + Helper.roundToString(dodge, 1) + "%)", true, false);
				break proc;
			}

			if (applyEffect(POST_ATTACK, atkr, source.side(), target.index(), source, target)) return;
			if (applyEffect(ON_DEATH, defr, target.side(), target.index(), source, target)) return;

			boolean applyDamage = !(defr.getBonus().popFlag(defr, Flag.NODAMAGE) || rules.noDamage());
			boolean noDmg = (defr.getHero() == null && defr.isDefending()) && !(defr.isSleeping() || defr.isStunned());

			int dmg;
			if (noDmg) {
				dmg = Math.round(atkr.getPenAtk() * fac);
			} else {
				dmg = Math.round((atkr.getBonus().popFlag(atkr, Flag.ALLDAMAGE) ? yPower : yPower - hPower) * fac);
			}

			if (op.getMana() > 0) {
				int toSteal = Math.min(op.getMana(), atkr.getManaDrain());

				you.addMana(toSteal);
				op.removeMana(toSteal);
			}

			Hero h = defr.getHero();
			if (h != null) {
				dmg = h.bufferDamage(dmg);
				if (defr.isDefending()) {
					dmg = 0;
				}
			}

			if (h == null || h.getHitpoints() <= 0) {
				op.addBleeding(Math.round(atkr.getBldAtk() * fac));

				if (applyDamage) {
					op.removeHp(dmg);
				}

				if (atkr.getHero() != null && atkr.getHero().getPerks().contains(Perk.REAPER)) {
					defr.setSealed(true);
				}
				killCard(target.side(), target.index(), defr.getId());

				if (h != null) {
					h.addXp(1);
					if (h.getPerks().contains(Perk.VAMPIRE)) {
						h.setHitpoints(h.getHitpoints() + Math.round((h.getMaxHp() - h.getHitpoints()) * 0.1f));
					}
				}
			}

			int extra = Math.round(dmg * fac - dmg);
			String msg;
			if (applyDamage && hBefore - op.getHp() != 0) {
				msg = "%s derrotou %s (%d > %d), causando %s de dano!%s".formatted(
						atkr.getName(),
						defr.getName(),
						yPower,
						hPower,
						hBefore - op.getHp(),
						extra > 0
								? " (dano direto aumentado em " + extra + ")"
								: extra < 0
								? " (dano direto reduzido em " + extra + ")"
								: ""
				);
			} else {
				msg = "%s derrotou %s (%d > %d)!".formatted(
						atkr.getName(),
						defr.getName(),
						yPower,
						hPower
				);
			}

			reportEvent(null, msg, true, false);
		}

		/* ATTACK FAILED */
		else if (yPower < hPower || blocked) {
			if (applyEffect(ON_SUICIDE, atkr, source.side(), target.index(), source, target)) return;
			if (applyEffect(POST_DEFENSE, defr, target.side(), target.index(), source, target)) return;

			boolean applyDamage = !(atkr.getBonus().popFlag(atkr, Flag.NODAMAGE) || rules.noDamage());
			int dmg = Math.round((defr.getBonus().popFlag(defr, Flag.ALLDAMAGE) ? hPower : hPower - yPower) * fac);

			if (you.getMana() > 0) {
				int toSteal = Math.min(you.getMana(), defr.getManaDrain());

				op.addMana(toSteal);
				you.removeMana(toSteal);
			}

			Hero h = atkr.getHero();
			if (h != null) {
				dmg = h.bufferDamage(dmg);
			}

			if (h == null || h.getHitpoints() <= 0) {
				you.addBleeding(Math.round(defr.getBldAtk() * fac));

				if (applyDamage) {
					you.removeHp(dmg);
				}

				if (defr.getHero() != null && defr.getHero().getPerks().contains(Perk.REAPER)) {
					atkr.setSealed(true);
				}
				killCard(source.side(), source.index(), atkr.getId());

				if (h != null) {
					h.addXp(1);
					if (h.getPerks().contains(Perk.VAMPIRE)) {
						h.setHitpoints(h.getHitpoints() + Math.round((h.getMaxHp() - h.getHitpoints()) * 0.1f));
					}
				}
			}

			int extra = Math.round(dmg * fac - dmg);
			String msg;
			if (applyDamage && yBefore - you.getHp() != 0) {
				msg = "%s não conseguiu derrotar %s (%s), sofrendo %s de dano!%s".formatted(
						atkr.getName(),
						defr.getName(),
						yPower > hPower ? "BLOQUEADO" : "%d < %d".formatted(yPower, hPower),
						yBefore - you.getHp(),
						extra > 0
								? " (dano direto aumentado em " + extra + ")"
								: extra < 0
								? " (dano direto reduzido em " + extra + ")"
								: ""
				);
			} else {
				msg = "%s não conseguiu derrotar %s (%s)".formatted(
						atkr.getName(),
						defr.getName(),
						yPower > hPower ? "BLOQUEADO" : "%d < %d".formatted(yPower, hPower)
				);
			}

			reportEvent(null, msg, true, false);
		}

		/* ATTACK CLASHED */
		else {
			if (applyEffect(ON_SUICIDE, atkr, source.side(), target.index(), source, target)) return;
			if (applyEffect(ON_DEATH, defr, target.side(), target.index(), source, target)) return;

			boolean noDmg = (defr.getHero() == null && defr.isDefending()) && !(defr.isSleeping() || defr.isStunned());

			int dmg;
			if (noDmg) {
				dmg = Math.round(atkr.getPenAtk() * fac);
			} else {
				dmg = Math.round((atkr.getBonus().popFlag(atkr, Flag.ALLDAMAGE) ? yPower : 0) * fac);
			}

			if (op.getMana() > 0 || you.getMana() > 0) {
				int yToSteal = Math.min(op.getMana(), atkr.getManaDrain());
				int hToSteal = Math.min(you.getMana(), defr.getManaDrain());

				if (op.getMana() > 0) {
					int toSteal = Math.max(0, yToSteal - hToSteal);

					you.addMana(toSteal);
					op.removeMana(toSteal);
				}
				if (you.getMana() > 0) {
					int toSteal = Math.max(0, hToSteal - yToSteal);

					op.addMana(toSteal);
					you.removeMana(toSteal);
				}
			}

			Hero h = defr.getHero();
			if (h != null) {
				dmg = h.bufferDamage(dmg);
				if (defr.isDefending()) {
					dmg = 0;
				}
			}

			if (h == null || h.getHitpoints() <= 0) {
				op.addBleeding(Math.round(atkr.getBldAtk() * fac));

				if (!defr.getBonus().popFlag(defr, Flag.NODAMAGE) || rules.noDamage()) {
					op.removeHp(dmg);
				}

				if (atkr.getHero() != null && atkr.getHero().getPerks().contains(Perk.REAPER)) {
					defr.setSealed(true);
				}
				killCard(target.side(), target.index(), defr.getId());

				if (h != null) {
					h.addXp(1);
					if (h.getPerks().contains(Perk.VAMPIRE)) {
						h.setHitpoints(h.getHitpoints() + Math.round((h.getMaxHp() - h.getHitpoints()) * 0.1f));
					}
				}
			}

			dmg = Math.round((defr.getBonus().popFlag(defr, Flag.ALLDAMAGE) ? hPower : 0) * fac);

			h = atkr.getHero();
			if (h != null) {
				dmg = h.bufferDamage(dmg);
			}

			if (h == null || h.getHitpoints() <= 0) {
				you.addBleeding(Math.round(defr.getBldAtk() * fac));

				if (!atkr.getBonus().popFlag(atkr, Flag.NODAMAGE) || rules.noDamage()) {
					you.removeHp(dmg);
				}

				if (defr.getHero() != null && defr.getHero().getPerks().contains(Perk.REAPER)) {
					atkr.setSealed(true);
				}
				killCard(source.side(), source.index(), atkr.getId());

				if (h != null) {
					h.addXp(1);
					if (h.getPerks().contains(Perk.VAMPIRE)) {
						h.setHitpoints(h.getHitpoints() + Math.round((h.getMaxHp() - h.getHitpoints()) * 0.1f));
					}
				}
			}

			String msg = "Ambas as cartas foram destruídas! (%d = %d)".formatted(
					yPower,
					hPower
			);

			reportEvent(null, msg, true, false);
		}

		if (applyEffect(POST_ATTACK_ASSIST, atkr.getAdjacent(Neighbor.LEFT), source.side(), source.index() - 1, source, target))
			return;
		if (applyEffect(POST_ATTACK_ASSIST, atkr.getAdjacent(Neighbor.RIGHT), source.side(), source.index() + 1, source, target))
			return;
		if (applyEffect(POST_DEFENSE_ASSIST, defr.getAdjacent(Neighbor.LEFT), target.side(), target.index() - 1, source, target))
			return;
		if (applyEffect(POST_DEFENSE_ASSIST, defr.getAdjacent(Neighbor.RIGHT), target.side(), target.index() + 1, source, target))
			return;

		if (undyingCd[you.getSide() == Side.TOP ? 1 : 0] == 5) {
			you.addHp((hBefore - op.getHp()) / 10);
		} else if (undyingCd[you.getSide() == Side.TOP ? 0 : 1] == 5) {
			you.addHp((yBefore - you.getHp()) / 10);
		}
		postCombat();
	}

	private boolean makeFusion(Hand h) {
		if (fusionLock > 0) return false;
		List<SlotColumn> slts = arena.getSlots().get(getCurrentSide());

		List<String> champsInField = slts.parallelStream()
				.map(SlotColumn::getTop)
				.map(c -> c == null || c.isSealed() ? null : c.getCard().getId())
				.toList();

		List<String> equipsInField = slts.parallelStream()
				.map(SlotColumn::getBottom)
				.map(dr -> dr == null ? null : dr.getCard().getId())
				.toList();

		String field = getArena().getField() != null ? getArena().getField().getCard().getId() : "DEFAULT";

		Champion fusion = fusions
				.parallelStream()
				.filter(f ->
						f.getRequiredCards().size() > 0 &&
								!f.canFuse(champsInField, equipsInField, field).isEmpty() &&
								((h.isNullMode() && h.getHp() > f.getBaseStats() / 2) || h.getMana() >= f.getMana()) &&
								(f.getBlood() == 0 || h.getHp() > f.getBlood())
				)
				.findFirst()
				.map(Champion::copy)
				.orElse(null);

		if (fusion != null) {
			List<FusionMaterial> mats = fusion.canFuse(champsInField, equipsInField, field);
			for (FusionMaterial material : mats) {
				banCard(h.getSide(), material.index(), material.equipment());
			}

			SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);
			if (sc != null) {
				sc.setTop(fusion);

				if (fusion.getMana() > 0) {
					if (h.isNullMode())
						h.removeHp(fusion.getBaseStats() / 2);
					else
						h.removeMana(fusion.getMana());
				}
				h.removeHp(fusion.getBlood());
			}

			return makeFusion(h);
		}

		return false;
	}

	public void killCard(Side side, int index, int id) {
		Champion target = getSlot(side, index).getTop();
		if (id > -1) {
			if (target == null || target.getId() != id || target.getBonus().popFlag(target, Flag.NODEATH)) return;
		} else {
			if (target == null) return;
		}

		List<SlotColumn> slts = getArena().getSlots().get(side);
		for (SlotColumn slt : slts) {
			Champion c = slt.getTop();
			if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
				killCard(side, slt.getIndex(), c.getId());
			} else if (c != null) {
				c.getBonus().setAtk(index, 0);
				c.getBonus().setDef(index, 0);
				c.getBonus().setDodge(index, 0);
			}

			Equipment e = slt.getBottom();
			if (e != null) {
				CardLink cl = e.getLinkedTo();
				if (cl != null && cl.getIndex() == index) {
					unequipCard(side, slt.getIndex());
				}
			}
		}

		getSlot(side, index).setTop(null);
		if (target.canGoToGrave())
			arena.getGraveyard().get(side).add(target);
	}

	public void destroyCard(Side side, int index, Side caster, int source) {
		Champion target = getSlot(side, index).getTop();
		if (target == null || target.getBonus().popFlag(target, Flag.NODEATH)) return;

		double chance = 100;
		Champion activator = null;
		if (caster != null && source > -1) {
			activator = getSlot(caster, source).getTop();
			if (activator != null) {
				int sourceMana = activator.getMana(5);
				int targetMana = target.getMana(5);

				chance -= target.getDodge(false) * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getCharm();
			if (charm == SHIELD || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			List<CardLink> aux = List.copyOf(target.getLinkedTo());
			for (CardLink cl : aux) {
				Equipment e = cl.asEquipment();

				if (e.getCharms().contains(MIRROR) && activator != null) {
					destroyCard(caster, source, side, index);
				}

				if (e.getCharms().contains(SHIELD)) {
					int uses = e.getBonus().getSpecialData().getInt("uses") + 1;
					if (uses >= Helper.getFibonacci(e.getTier())) {
						unequipCard(side, e.getIndex());
					} else {
						e.getBonus().putProp("uses", uses);
					}
					return;
				}
			}

			List<SlotColumn> slts = getArena().getSlots().get(side);
			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
					killCard(side, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(index, 0);
					c.getBonus().setDef(index, 0);
					c.getBonus().setDodge(index, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			getSlot(side, index).setTop(null);
			if (target.canGoToGrave())
				arena.getGraveyard().get(side).add(target);
		} else {
			channel.sendMessage("Efeito de " + activator.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void destroyCard(Side side, int index) {
		destroyCard(side, index, null, -1);
	}

	public void convertCard(Side side, int index, Side caster, int source) {
		if (caster == null) caster = side.getOther();

		Champion target = getSlot(side, index).getTop();
		if (target == null || target.getBonus().popFlag(target, Flag.NOCONVERT)) return;

		double chance = 100;
		Champion activator = null;
		if (source > -1) {
			activator = getSlot(caster, source).getTop();
			if (activator != null) {
				int sourceMana = activator.getMana(5);
				int targetMana = target.getMana(5);

				chance -= target.getDodge(false) * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getCharm();
			if (charm == SHIELD || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			List<CardLink> aux = List.copyOf(target.getLinkedTo());
			for (CardLink cl : aux) {
				Equipment e = cl.asEquipment();

				if (e.getCharms().contains(MIRROR) && activator != null) {
					convertCard(caster, source, side, index);
				}

				if (e.getCharms().contains(SHIELD)) {
					int uses = e.getBonus().getSpecialData().getInt("uses") + 1;
					if (uses >= Helper.getFibonacci(e.getTier())) {
						unequipCard(side, e.getIndex());
					} else {
						e.getBonus().putProp("uses", uses);
					}
					return;
				}
			}

			List<SlotColumn> slts = getArena().getSlots().get(side);
			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
					killCard(side, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(index, 0);
					c.getBonus().setDef(index, 0);
					c.getBonus().setDodge(index, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			getSlot(side, index).setTop(null);
			SlotColumn sc = getFirstAvailableSlot(caster, true);
			if (sc == null) {
				if (target.canGoToGrave())
					arena.getGraveyard().get(caster).add(target);
			} else {
				sc.setTop(target);
			}
		} else {
			channel.sendMessage("Efeito de " + activator.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void convertCard(Side side, int index) {
		convertCard(side, index, null, -1);
	}

	public void switchCards(Side side, int index, Side caster, int source) {
		Champion target = getSlot(side, index).getTop();
		if (target == null || target.getBonus().popFlag(target, Flag.NOCONVERT)) return;

		double chance = 100;
		Champion activator = getSlot(caster, source).getTop();
		if (activator == null || activator.getBonus().popFlag(activator, Flag.NOCONVERT)) return;

		int sourceMana = activator.getMana(5);
		int targetMana = target.getMana(5);

		chance -= target.getDodge(false) * 0.75;
		if (sourceMana < targetMana)
			chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getCharm();
			if (charm == SHIELD || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			List<CardLink> aux = List.copyOf(target.getLinkedTo());
			for (CardLink cl : aux) {
				Equipment e = cl.asEquipment();

				if (e.getCharms().contains(SHIELD)) {
					int uses = e.getBonus().getSpecialData().getInt("uses") + 1;
					if (uses >= Helper.getFibonacci(e.getTier())) {
						unequipCard(side, e.getIndex());
					} else {
						e.getBonus().putProp("uses", uses);
					}
					return;
				}
			}

			List<SlotColumn> slts = getArena().getSlots().get(side);
			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
					killCard(side, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(index, 0);
					c.getBonus().setDef(index, 0);
					c.getBonus().setDodge(index, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			charm = activator.getBonus().getCharm();
			if (charm == SHIELD || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			aux = List.copyOf(target.getLinkedTo());
			for (CardLink cl : aux) {
				Equipment e = cl.asEquipment();

				if (e.getCharms().contains(SHIELD)) {
					int uses = e.getBonus().getSpecialData().getInt("uses") + 1;
					if (uses >= Helper.getFibonacci(e.getTier())) {
						unequipCard(side, e.getIndex());
					} else {
						e.getBonus().putProp("uses", uses);
					}
					return;
				}
			}

			slts = getArena().getSlots().get(caster);
			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == source) {
					killCard(caster, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(source, 0);
					c.getBonus().setDef(source, 0);
					c.getBonus().setDodge(source, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			getSlot(side, index).setTop(activator);
			getSlot(caster, source).setTop(target);
		} else {
			channel.sendMessage("Efeito de " + activator.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void captureCard(Side side, int index, Side caster, int source, boolean withFusion) {
		Champion target = getSlot(side, index).getTop();
		if (target == null) return;

		double chance = 100;
		Champion activator = null;
		if (caster != null && source > -1) {
			activator = getSlot(caster, source).getTop();
			if (activator != null) {
				int sourceMana = activator.getMana(5);
				int targetMana = target.getMana(5);

				chance -= target.getDodge(false) * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getCharm();
			if (charm == SHIELD || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			List<CardLink> aux = List.copyOf(target.getLinkedTo());
			for (CardLink cl : aux) {
				Equipment e = cl.asEquipment();

				if (e.getCharms().contains(MIRROR) && activator != null) {
					captureCard(caster, source, side, index, withFusion);
				}

				if (e.getCharms().contains(SHIELD)) {
					int uses = e.getBonus().getSpecialData().getInt("uses") + 1;
					if (uses >= Helper.getFibonacci(e.getTier())) {
						unequipCard(side, e.getIndex());
					} else {
						e.getBonus().putProp("uses", uses);
					}
					return;
				}
			}

			List<SlotColumn> slts = getArena().getSlots().get(side);
			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
					killCard(side, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(index, 0);
					c.getBonus().setDef(index, 0);
					c.getBonus().setDodge(index, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			target.reset();
			getSlot(side, index).setTop(null);
			if (!target.isFusion() || withFusion)
				hands.get(side.getOther()).getCards().add(target);
		} else {
			channel.sendMessage("Efeito de " + activator.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void captureCard(Side side, int index, boolean withFusion) {
		captureCard(side, index, null, -1, withFusion);
	}

	public void banCard(Side side, int index, boolean equipment) {
		List<SlotColumn> slts = getArena().getSlots().get(side);
		if (equipment) {
			Equipment target = slts.get(index).getBottom();
			if (target == null) return;

			CardLink link = target.getLinkedTo();
			if (link != null)
				link.asChampion().unlink(target);

			slts.get(index).setBottom(null);
			arena.getBanned().add(target);
		} else {
			Champion target = slts.get(index).getTop();
			if (target == null || target.getBonus().popFlag(target, Flag.NOBAN)) return;

			for (SlotColumn slt : slts) {
				Champion c = slt.getTop();
				if (c != null && c.getBonus().getSpecialData().getInt("original", -1) == index) {
					killCard(side, slt.getIndex(), c.getId());
				} else if (c != null) {
					c.getBonus().setAtk(index, 0);
					c.getBonus().setDef(index, 0);
					c.getBonus().setDodge(index, 0);
				}

				Equipment e = slt.getBottom();
				if (e != null) {
					CardLink cl = e.getLinkedTo();
					if (cl != null && cl.getIndex() == index) {
						unequipCard(side, slt.getIndex());
					}
				}
			}

			getSlot(side, index).setTop(null);
			if (target.canGoToGrave())
				arena.getBanned().add(target);
		}
	}

	public void unequipCard(Side side, int index) {
		Equipment target = getSlot(side, index).getBottom();
		if (target == null) return;

		if (target.getLinkedTo() != null) {
			target.getLinkedTo().asChampion().unlink(target);
		}

		getSlot(side, index).setBottom(null);
		if (target.canGoToGrave()) {
			if (target.getTier() >= 4)
				arena.getBanned().add(target);
			else
				arena.getGraveyard().get(side).add(target);
		}
	}

	public Arena getArena() {
		return arena;
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return arena.getSlots();
	}

	public Side getSideById(String id) {
		return hands.values().parallelStream()
				.filter(h -> h.getUser().getId().equals(id) || (h instanceof TeamHand th && th.getUsers().parallelStream().anyMatch(u -> u.getId().equals(id))))
				.map(Hand::getSide)
				.findFirst().orElse(null);
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, Triple<Race, Boolean, Race>> getCombos() {
		return combos;
	}

	public Rules getRules() {
		return rules;
	}

	public SlotColumn getFirstAvailableSlot(Side s, boolean top) {
		List<SlotColumn> slots = arena.getSlots().get(s);
		for (SlotColumn slt : slots) {
			if (!slt.isUnavailable() && (top ? slt.getTop() == null : slt.getBottom() == null))
				return slt;
		}

		return null;
	}

	public boolean lastTick() {
		for (Side s : Side.values()) {
			Hand h = hands.get(s);
			Hand op = hands.get(s.getOther());
			List<SlotColumn> slts = arena.getSlots().get(s);

			applyEffect(h.getHp() <= 0 ? ON_GLOBAL_LOSE : ON_GLOBAL_WIN, (Champion) null, s, -1);
			for (SlotColumn slt : slts) {
				if (slt.getTop() == null) continue;

				applyEffect(h.getHp() <= 0 ? ON_LOSE : ON_WIN, slt.getTop(), s, slt.getIndex());
			}

			if (h.getHp() > 0 && op.getHp() > 0) return true;
		}

		return false;
	}

	public boolean postCombat() {
		if (!isOpen()) return true;

		boolean finished = false;
		for (Hand h : hands.values()) {
			Hand op = hands.get(h.getSide().getOther());

			if (h.getHp() <= 0) {
				int i = h.getSide() == Side.TOP ? 1 : 0;

				if (lastTick() || undyingCd[i] == 5) continue;
				else if (combos.get(h.getSide()).getLeft() == Race.UNDEAD) {
					if (undyingCd[i] == 0) {
						undyingCd[i] = 5;
						continue;
					}
				}

				if (rules.official()) {
					getHistory().setWinner(op.getSide());
					getBoard().awardWinner(this, op.getUser().getId());
				}

				String msg;
				if (team)
					msg = ((TeamHand) op).getMentions() + " zeraram os pontos de vida de " + ((TeamHand) h).getMentions() + ", temos os vencedores! (" + getRound() + " turnos)";
				else
					msg = op.getUser().getAsMention() + " zerou os pontos de vida de " + h.getUser().getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)";

				for (List<SlotColumn> sides : arena.getSlots().values()) {
					for (SlotColumn slts : sides) {
						if (slts.getTop() != null)
							slts.getTop().setFlipped(false, false);

						if (slts.getBottom() != null)
							slts.getBottom().setFlipped(false, false);
					}
				}

				close();
				channel.sendMessage(msg)
						.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
						.queue(ms ->
								this.message.compute(ms.getChannel().getId(), (id, m) -> {
									if (m != null) m.delete().queue(null, Helper::doNothing);
									return ms;
								})
						);
				finished = true;
				break;
			}
		}

		return finished;
	}

	@Override
	public Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons() {
		ThrowingConsumer<ButtonWrapper> skip = skipTurn();

		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		if (getRound() < 1 || phase == Phase.COMBAT)
			buttons.put(Helper.parseEmoji("▶️"), skip);
		else {
			buttons.put(Helper.parseEmoji("▶️"), wrapper -> {
				phase = Phase.COMBAT;
				draw = false;
				reroll = false;
				applyEffect(COMBAT_STAGE, (Champion) null, getCurrentSide(), -1);
				reportEvent(null, "**FASE DE COMBATE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate", true, false);
			});
			buttons.put(Helper.parseEmoji("⏩"), wrapper -> {
				draw = false;
				reroll = false;
				skip.accept(wrapper);
			});
		}
		if (phase == Phase.PLAN) {
			buttons.put(Helper.parseEmoji("\uD83D\uDCE4"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());

				int remaining = h.getMaxCards() - h.getCardCount();
				if (remaining <= 0) {
					channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver " + h.getMaxCards() + " ou mais na sua mão.").queue(null, Helper::doNothing);
					return;
				}

				if (!h.manualDraw()) {
					if (rules.official()) {
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
					}

					String msg;
					if (team)
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + ((TeamHand) hands.get(getNextSide())).getMentions() + " venceram! (" + getRound() + " turnos)";
					else
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + hands.get(getNextSide()).getUser().getAsMention() + " venceu! (" + getRound() + " turnos)";

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return mm;
									})
							);

					return;
				}

				remaining = h.getMaxCards() - h.getCardCount();
				reportEvent(h, getCurrent().getName() + " puxou uma carta. (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")", true, false);
			});
			buttons.put(Helper.parseEmoji("\uD83D\uDCE6"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());

				int remaining = h.getMaxCards() - h.getCardCount();
				if (remaining <= 0) {
					channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver " + h.getMaxCards() + " ou mais na sua mão.").queue(null, Helper::doNothing);
					return;
				}

				int toDraw = Math.min(remaining, h.getRealDeque().size());
				if (toDraw == 0) {
					if (rules.official()) {
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
					}

					String msg;
					if (team)
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + ((TeamHand) hands.get(getNextSide())).getMentions() + " venceram! (" + getRound() + " turnos)";
					else
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + hands.get(getNextSide()).getUser().getAsMention() + " venceu! (" + getRound() + " turnos)";

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return mm;
									})
							);

					return;
				}

				for (int i = 0; i < toDraw; i++) {
					h.manualDraw();
				}

				if (toDraw == 1)
					reportEvent(h, getCurrent().getName() + " puxou uma carta.", true, false);
				else
					reportEvent(h, getCurrent().getName() + " puxou " + toDraw + " cartas.", true, false);
			});
			if (combos.get(getCurrentSide()).getLeft() == Race.SPIRIT && synthCd[getCurrentSide() == Side.TOP ? 1 : 0] == 0) {
				buttons.put(Helper.parseEmoji("\uD83C\uDF00"), wrapper -> {
					if (phase != Phase.PLAN) {
						channel.sendMessage("❌ | Você só pode sintetizar cartas na fase de planejamento.").queue(null, Helper::doNothing);
						return;
					}

					Hand h = hands.get(getCurrentSide());
					List<Drawable> dsc = h.getDiscardBatch();
					if (dsc.size() < 3) {
						channel.sendMessage("❌ | Você não possui almas suficiente para sintetizar.").queue(null, Helper::doNothing);
						return;
					}
					dsc = dsc.subList(0, 3);

					int score = dsc.stream()
							.mapToInt(c -> switch (c.getCard().getRarity()) {
										case FIELD -> 5;
										case EQUIPMENT -> ((Equipment) c).getTier();
										case COMMON, UNCOMMON, RARE, ULTRA_RARE, LEGENDARY -> c.getCard().getRarity().getIndex();
										default -> 0;
									}
							)
							.sum();
					int max = 15;
					double base = (max - score) / 0.75 / (max - 3);

					double t3 = Math.max(0, 0.65 - base);
					double t4 = Math.max(0, (t3 * 15) / 65 - 0.05);
					double t1 = Math.max(0, base - t4 * 10);
					double t2 = Math.max(0, 0.85 - Math.abs(0.105 - t1 / 3) * 5 - t3);

					List<Equipment> pool = CardDAO.getAllAvailableEquipments();

					List<Equipment> chosenTier = Helper.getRandom(pool.stream()
							.collect(Collectors.groupingBy(Equipment::getTier))
							.entrySet()
							.stream()
							.map(e -> org.apache.commons.math3.util.Pair.create(e.getValue(), switch (e.getKey()) {
										case 1 -> t1;
										case 2 -> t2;
										case 3 -> t3;
										case 4 -> t4;
										default -> 0d;
									})
							).toList()
					);

					h.getCards().add(Helper.getRandomEntry(chosenTier));

					arena.getBanned().addAll(dsc);
					dsc.clear();
					synthCd[getCurrentSide() == Side.TOP ? 1 : 0] = 3;
					reportEvent(h, getCurrent().getName() + " sacrificou 3 almas para sintetizar um evogear.", true, false);
				});
			}
			buttons.put(Helper.parseEmoji("\uD83D\uDD0D"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode inspecionar o campo na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				List<SlotColumn> slts = arena.getSlots().get(getCurrentSide());
				if (slts.stream().map(slt -> Arrays.asList(slt.getTop(), slt.getBottom())).flatMap(List::stream).noneMatch(Objects::nonNull)) {
					channel.sendMessage("❌ | Não há nenhuma carta no seu campo.").queue(null, Helper::doNothing);
					return;
				}

				wrapper.getHook()
						.setEphemeral(true)
						.sendFile(Helper.writeAndGet(arena.renderSide(getCurrentSide()), String.valueOf(this.hashCode()), "png"))
						.queue();
			});
		}
		if (reroll && getRound() == 1 && phase == Phase.PLAN)
			buttons.put(Helper.parseEmoji("\uD83D\uDD04"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());
				h.redrawHand();

				reroll = false;
				reportEvent(h, getCurrent().getName() + " rolou novamente as cartas na mão!", true, false);
			});
		if (hands.get(getCurrentSide()).getHp() < hands.get(getCurrentSide()).getBaseHp() / 3f && hands.get(getCurrentSide()).getDestinyDeck().size() > 0 && phase == Phase.PLAN)
			buttons.put(Helper.parseEmoji("\uD83E\uDDE7"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());
				h.destinyDraw();

				reportEvent(h, getCurrent().getName() + " executou um saque do destino!", true, false);
			});
		if (phase == Phase.PLAN && tourMatch == null)
			buttons.put(Helper.parseEmoji("\uD83E\uDD1D"), wrapper -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode pedir empate na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				if (draw) {
					String msg = "Por acordo mútuo, declaro empate! (" + getRound() + " turnos)";

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return mm;
									})
							);
				} else {
					User u = getCurrent();

					AtomicReference<Hand> h = new AtomicReference<>(hands.get(getCurrentSide()));
					h.get().getCards().removeIf(d -> !d.isAvailable());
					List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());

					if (applyPersistentEffects(AFTER_TURN, getCurrentSide(), -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						Champion c = slots.get(i).getTop();
						if (c != null) {
							c.setAvailable(!c.isStunned() && !c.isSleeping());
							c.resetAttribs();
							if (applyEffect(AFTER_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
									|| makeFusion(h.get())
							) return;
						}
					}

					arena.getGraveyard().get(getCurrentSide()).addAll(
							h.get().getDiscardBatch().stream()
									.filter(d -> {
										if (d instanceof Champion c) return c.canGoToGrave();
										else if (d instanceof Equipment e) return e.canGoToGrave();
										else return true;
									}).toList()
					);
					h.get().getDiscardBatch().clear();

					if (getRound() > 0) reroll = false;
					resetTimer(this);
					if (postCombat()) return;

					phase = Phase.PLAN;
					applyEffect(PLAN_STAGE, (Champion) null, getCurrentSide(), -1);

					h.set(hands.get(getCurrentSide()));
					h.get().decreaseSuppression();
					h.get().decreaseLockTime();
					h.get().decreaseNullTime();
					h.get().decreaseBlindTime();
					slots = arena.getSlots().get(getCurrentSide());

					if (getRound() == 10 || getRound() == 11) {
						if (h.get().getCombo().getRight() == Race.CREATURE) {
							h.get().setMaxCards(h.get().getMaxCards() + 1);
						}
					}

					if (getRound() >= 75) {
							if (Helper.equalsAny(getRound(), 75, 100, 125)) {
								if (getRound() == 75) {
									channel.sendMessage(":warning: | ALERTA: Morte-súbita I ativada, os jogadores perderão 10% do HP atual a cada turno!").queue();
								} else if (getRound() == 100) {
									channel.sendMessage(":warning: | ALERTA: Morte-súbita II ativada, os jogadores perderão 25% do HP atual a cada turno!").queue();
								} else {
									channel.sendMessage(":warning: | ALERTA: Morte-súbita III ativada, se a partida não acabar neste turno será declarado empate!").queue();
								}
							}

							if (Helper.between(getRound(), 75, 126)) {
								h.get().removeHp((int) Math.ceil(h.get().getHp() * (getRound() >= 100 ? 0.25 : 0.10)));
								if (postCombat()) return;
							} else {
								draw = true;
								String msg = "Declaro empate! (" + getRound() + " turnos)";

								for (List<SlotColumn> sides : arena.getSlots().values()) {
									for (SlotColumn slts : sides) {
										if (slts.getTop() != null)
											slts.getTop().setFlipped(false, false);

										if (slts.getBottom() != null)
											slts.getBottom().setFlipped(false, false);
									}
								}

								close();
								channel.sendMessage(msg)
										.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
										.queue(mm ->
												this.message.compute(mm.getChannel().getId(), (id, m) -> {
													if (m != null) m.delete().queue(null, Helper::doNothing);
													return mm;
												})
										);
								return;
							}
						}

					if (h.get().getBleeding() > 0) {
						h.get().removeHp(h.get().getBleeding() / (h.get().getCombo().getLeft() == Race.HUMAN ? 10 : 5));
						h.get().decreaseBleeding();
					} else if (h.get().getRegeneration() > 0) {
						h.get().addHp(h.get().getRegeneration() / 2);
						h.get().decreaseRegeneration();
					}

					int mpt = h.get().getManaPerTurn();
					if (h.get().getCombo().getLeft() == Race.DEMON) {
						Hand op = hands.get(getNextSide());
						mpt += Math.max(0f, op.getBaseHp() - op.getHp()) / op.getBaseHp() * 5;
						if (h.get().getHp() < h.get().getBaseHp() / 3f) {
							h.get().addHp(Math.round((h.get().getBaseHp() - h.get().getHp()) * 0.1f));
						}
					}
					h.get().addMana(mpt);

					if (!h.get().isSuppressed()) {
						if (h.get().getCombo().getRight() == Race.ELF) {
							int turns = getRound() - (h.get().getSide() == Side.TOP ? 1 : 0);

							if (getRound() > 1) {
								if (turns % 5 == 0) {
									h.get().addMana(2);
								} else if (turns % 2 == 0) {
									h.get().addMana(1);
								}
							}
						}

						if (h.get().getCombo().getLeft() == Race.DIVINITY) {
							h.get().addMana((int) Math.round(4 - h.get().getAvgCost()));
						}
					}

					if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						SlotColumn sc = slots.get(i);

						Equipment e = sc.getBottom();
						if (e != null) {
							if (e.getLinkedTo() == null) {
								unequipCard(getCurrentSide(), e.getIndex());
							} else {
								Champion link = getSlot(getCurrentSide(), e.getLinkedTo().getIndex()).getTop();

								if (link == null || !link.equals(e.getLinkedTo().linked())) {
									unequipCard(getCurrentSide(), e.getIndex());
								} else {
									e.getLinkedTo().sync();
								}
							}
						}

						Champion c = sc.getTop();
						if (c != null) {
							if (c.isStasis()) c.reduceStasis();
							else if (c.isStunned()) c.reduceStun();
							else if (c.isSleeping()) c.reduceSleep();

							c.getLinkedTo().removeIf(CardLink::isInvalid);

							if (applyEffect(BEFORE_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
									|| makeFusion(h.get())
							) return;
						}

						if (sc.isUnavailable()) {
							sc.setUnavailable(-1);
						}
					}

					draw = true;
					reportEvent(h.get(), u.getName() + " deseja um acordo de empate, " + getCurrent().getAsMention() + " agora é sua vez, clique em \uD83E\uDD1D caso queira aceitar ou continue jogando normalmente.", false, true);
					oldState = new GameState(this);
				}
			});
		if (phase == Phase.PLAN && (!rules.official() || getRound() > 8)) {
			buttons.put(Helper.parseEmoji("\uD83C\uDFF3️"), wrapper -> {
				if (forfeit) {
					if (rules.official()) {
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
					}

					String msg = getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)";

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return mm;
									})
							);
				} else {
					forfeit = true;
					wrapper.getHook()
							.setEphemeral(true)
							.sendMessage("Pressione novamente para desistir.")
							.queue();
				}
			});
		}

		return buttons;
	}

	public ThrowingConsumer<ButtonWrapper> skipTurn() {
		return w -> {
			User u = getCurrent();

			AtomicReference<Hand> h = new AtomicReference<>(hands.get(getCurrentSide()));
			h.get().getCards().removeIf(d -> !d.isAvailable());
			List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());

			if (applyPersistentEffects(AFTER_TURN, getCurrentSide(), -1)) return;
			for (int i = 0; i < slots.size(); i++) {
				Champion c = slots.get(i).getTop();
				if (c != null) {
					c.setAvailable(!c.isStunned() && !c.isSleeping());
					c.resetAttribs();
					if (applyEffect(AFTER_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
							|| makeFusion(h.get())
					) return;
				}
			}

			arena.getGraveyard().get(getCurrentSide()).addAll(
					h.get().getDiscardBatch().stream()
							.filter(d -> {
								if (d instanceof Champion c) return c.canGoToGrave();
								else if (d instanceof Equipment e) return e.canGoToGrave();
								else return true;
							}).toList()
			);
			h.get().getDiscardBatch().clear();

			if (getRound() > 0) reroll = false;
			resetTimer(this);
			if (postCombat()) return;

			phase = Phase.PLAN;
			applyEffect(PLAN_STAGE, (Champion) null, getCurrentSide(), -1);

			h.set(hands.get(getCurrentSide()));
			h.get().decreaseSuppression();
			h.get().decreaseLockTime();
			h.get().decreaseNullTime();
			h.get().decreaseBlindTime();
			slots = arena.getSlots().get(getCurrentSide());

			if (getRound() == 10 || getRound() == 11) {
				if (h.get().getCombo().getRight() == Race.CREATURE) {
					h.get().setMaxCards(h.get().getMaxCards() + 1);
				}
			}

			if (getRound() >= 75) {
				if (Helper.equalsAny(getRound(), 75, 100, 125)) {
					if (getRound() == 75) {
						channel.sendMessage(":warning: | ALERTA: Morte-súbita I ativada, os jogadores perderão 10% do HP atual a cada turno!").queue();
					} else if (getRound() == 100) {
						channel.sendMessage(":warning: | ALERTA: Morte-súbita II ativada, os jogadores perderão 25% do HP atual a cada turno!").queue();
					} else {
						channel.sendMessage(":warning: | ALERTA: Morte-súbita III ativada, se a partida não acabar neste turno será declarado empate!").queue();
					}
				}

				if (Helper.between(getRound(), 75, 126)) {
					h.get().removeHp((int) Math.ceil(h.get().getHp() * (getRound() >= 100 ? 0.25 : 0.10)));
					if (postCombat()) return;
				} else {
					draw = true;
					String msg = "Declaro empate! (" + getRound() + " turnos)";

					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false, false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false, false);
						}
					}

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null) m.delete().queue(null, Helper::doNothing);
										return mm;
									})
							);
					return;
				}
			}

			if (h.get().getBleeding() > 0) {
				h.get().removeHp(h.get().getBleeding() / (h.get().getCombo().getLeft() == Race.HUMAN ? 10 : 5));
				h.get().decreaseBleeding();
			} else if (h.get().getRegeneration() > 0) {
				h.get().addHp(h.get().getRegeneration() / 2);
				h.get().decreaseRegeneration();
			}

			int mpt = h.get().getManaPerTurn();
			if (h.get().getCombo().getLeft() == Race.DEMON) {
				Hand op = hands.get(getNextSide());
				mpt += Math.max(0f, op.getBaseHp() - op.getHp()) / op.getBaseHp() * 5;
				if (h.get().getHp() < h.get().getBaseHp() / 3f) {
					h.get().addHp(Math.round((h.get().getBaseHp() - h.get().getHp()) * 0.1f));
				}
			}
			h.get().addMana(mpt);

			if (!h.get().isSuppressed()) {
				if (h.get().getCombo().getRight() == Race.ELF) {
					int turns = getRound() - (h.get().getSide() == Side.TOP ? 1 : 0);

					if (getRound() > 1) {
						if (turns % 5 == 0) {
							h.get().addMana(2);
						} else if (turns % 2 == 0) {
							h.get().addMana(1);
						}
					}
				}

				if (h.get().getCombo().getLeft() == Race.DIVINITY) {
					h.get().addMana((int) Math.round(4 - h.get().getAvgCost()));
				}
			}

			if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
			for (int i = 0; i < slots.size(); i++) {
				SlotColumn sc = slots.get(i);

				Equipment e = sc.getBottom();
				if (e != null) {
					if (e.getLinkedTo() == null) {
						unequipCard(getCurrentSide(), e.getIndex());
					} else {
						Champion link = getSlot(getCurrentSide(), e.getLinkedTo().getIndex()).getTop();

						if (link == null || !link.equals(e.getLinkedTo().linked())) {
							unequipCard(getCurrentSide(), e.getIndex());
						} else {
							e.getLinkedTo().sync();
						}
					}
				}

				Champion c = sc.getTop();
				if (c != null) {
					if (c.isStasis()) c.reduceStasis();
					else if (c.isStunned()) c.reduceStun();
					else if (c.isSleeping()) c.reduceSleep();

					c.getLinkedTo().removeIf(CardLink::isInvalid);

					if (applyEffect(BEFORE_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
							|| makeFusion(h.get())
					) return;
				}

				if (sc.isUnavailable()) {
					sc.setUnavailable(-1);
				}
			}

			String msg = u.getName() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention() + " (turno " + getRound() + ")";
			reportEvent(h.get(), msg, false, true);
			oldState = new GameState(this);
		};
	}

	public Champion getChampionFromGrave(Side s) {
		LinkedList<Drawable> grv = getArena().getGraveyard().get(s);
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Champion)
				return (Champion) grv.remove(i);

		return null;
	}

	public Equipment getEquipmentFromGrave(Side s) {
		LinkedList<Drawable> grv = getArena().getGraveyard().get(s);
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Equipment)
				return (Equipment) grv.remove(i);

		return null;
	}

	public Champion getChampionFromBanned() {
		LinkedList<Drawable> grv = getArena().getBanned();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Champion)
				return (Champion) grv.remove(i);

		return null;
	}

	public Equipment getEquipmentFromBanned() {
		LinkedList<Drawable> grv = getArena().getBanned();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Equipment)
				return (Equipment) grv.remove(i);

		return null;
	}

	public int getFusionLock() {
		return fusionLock;
	}

	public void setFusionLock(int fusionLock) {
		this.fusionLock = fusionLock;
	}

	public void addFLockTime(int time) {
		fusionLock += time;
	}

	public void decreaseFLockTime() {
		fusionLock = Math.max(0, fusionLock - 1);
	}

	public int getSpellLock() {
		return spellLock;
	}

	public void setSpellLock(int spellLock) {
		this.spellLock = spellLock;
	}

	public void addSLockTime(int time) {
		spellLock += time;
	}

	public void decreaseSLockTime() {
		spellLock = Math.max(0, spellLock - 1);
	}

	public int getEffectLock() {
		return effectLock;
	}

	public void setEffectLock(int effectLock) {
		this.effectLock = effectLock;
	}

	public void addELockTime(int time) {
		effectLock += time;
	}

	public void decreaseELockTime() {
		effectLock = Math.max(0, effectLock - 1);
	}

	public boolean isSlotDisabled(Side side, int slot) {
		return arena.getSlots().get(side).get(slot).isUnavailable();
	}

	public void disableSlot(Side side, int slot, int time) {
		arena.getSlots().get(side).get(slot).setUnavailable(time);
	}

	public boolean isSlotChanged(Side side, int slot) {
		return arena.getSlots().get(side).get(slot).isChanged();
	}

	public void setSlotChanged(Side side, int slot, boolean changed) {
		arena.getSlots().get(side).get(slot).setChanged(changed);
	}

	public Set<PersistentEffect> getPersistentEffects() {
		return persistentEffects;
	}

	public void addPersistentEffect(PersistentEffect pe) {
		if (pe == null) return;

		Set<PersistentEffect> aux = Set.copyOf(persistentEffects);
		for (PersistentEffect curr : aux) {
			if (curr.equals(pe)) {
				float bias = Helper.prcnt(pe.getTurns(), Math.max(1, curr.getTurns())) * Helper.prcnt(pe.getLimit(), Math.max(1, curr.getLimit()));

				if (bias > 1) {
					persistentEffects.remove(curr);
					persistentEffects.add(pe);
				}

				return;
			}
		}

		persistentEffects.add(pe);
	}

	public void removePersistentEffect(Drawable card) {
		persistentEffects.removeIf(pe -> pe.getCard().equals(card));
	}

	public void removePersistentEffect(String source) {
		persistentEffects.removeIf(pe -> pe.getSource().equals(source));
	}

	public boolean applyPersistentEffects(EffectTrigger trigger, Side to, int index) {
		if (persistentEffects.size() > 0) {
			boolean lastTick = Helper.equalsAny(trigger, ON_WIN, ON_LOSE, ON_GLOBAL_WIN, ON_GLOBAL_LOSE);

			Set<PersistentEffect> efs = Set.copyOf(persistentEffects).stream()
					.peek(e -> {
						if (!e.isExpired()) {
							if (e.getTarget() == null || e.getTarget() == to) {
								if (trigger == AFTER_TURN) {
									e.decreaseTurn();
								}

								if (e.getTriggers().contains(trigger)) {
									e.activate(to, index);
								}
							}

							if (e.isExpired()) {
								channel.sendMessage(":timer: | O efeito " + e.getSource() + " expirou!").queue();
							}
						}
					})
					.filter(e -> !e.isExpired())
					.collect(Collectors.toSet());

			Helper.replaceContent(efs, persistentEffects);

			return !lastTick && postCombat();
		}

		return false;
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, Side side, int index) {
		return applyEffect(trigger, activator, side, index, Duelists.of());
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, Side side, int index, Source source) {
		return applyEffect(trigger, activator, side, index, Duelists.of(source));
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, Side side, int index, Target target) {
		return applyEffect(trigger, activator, side, index, Duelists.of(target));
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, Side side, int index, Source source, Target target) {
		return applyEffect(trigger, activator, side, index, Duelists.of(source, target));
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, Side side, int index, Duelists duelists) {
		boolean lastTick = Helper.equalsAny(trigger, ON_WIN, ON_LOSE, ON_GLOBAL_WIN, ON_GLOBAL_LOSE);

		if (trigger.shouldTriggerPE()) {
			applyPersistentEffects(trigger, side, index);

			if (!lastTick && postCombat()) return true;
		}

		if (activator == null) return false;
		if (effectLock == 0) {
			EffectParameters ep = new EffectParameters(trigger, this, side, index, duelists, channel);

			if (activator.hasEffect()) {
				boolean activate = true;

				if (duelists.getAttacker() != null) {
					Champion c = duelists.getAttacker();

					if (c.getBonus().popFlag(Flag.NOEFFECT)) activate = false;
					else if (c.isDuelling() && !c.getNemesis().equals(duelists.getDefender())) activate = false;
				}

				if (duelists.getDefender() != null) {
					Champion c = duelists.getDefender();

					if (c.getBonus().popFlag(Flag.NOEFFECT)) activate = false;
					else if (c.isDuelling() && !c.getNemesis().equals(duelists.getAttacker())) activate = false;
				}

				if (activate && !activator.isStunned())
					activator.getEffect(ep);
			}

			if (activator.isCursed()) {
				activator.getCurse(ep);
			}

			activator.triggerSpells(ep);

			return !lastTick && postCombat();
		}

		return false;
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index) {
		applyEffect(trigger, activator, side, index, Duelists.of());
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index, Source source) {
		applyEffect(trigger, activator, side, index, Duelists.of(source));
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index, Target target) {
		applyEffect(trigger, activator, side, index, Duelists.of(target));
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index, Source source, Target target) {
		applyEffect(trigger, activator, side, index, Duelists.of(source, target));
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index, Duelists duelists) {
		if (effectLock == 0) {
			if (activator.hasEffect()) {
				activator.getEffect(new EffectParameters(trigger, this, side, index, duelists, channel));
			}
		}
	}

	public void applyEffect(EffectTrigger trigger, Drawable activator, Side side, int index) {
		applyEffect(trigger, activator, side, index, Duelists.of());
	}

	public void applyEffect(EffectTrigger trigger, Drawable activator, Side side, int index, Source source) {
		applyEffect(trigger, activator, side, index, Duelists.of(source));
	}

	public void applyEffect(EffectTrigger trigger, Drawable activator, Side side, int index, Target target) {
		applyEffect(trigger, activator, side, index, Duelists.of(target));
	}

	public void applyEffect(EffectTrigger trigger, Drawable activator, Side side, int index, Source source, Target target) {
		applyEffect(trigger, activator, side, index, Duelists.of(source, target));
	}

	public void applyEffect(EffectTrigger trigger, Drawable activator, Side side, int index, Duelists duelists) {
		if (activator instanceof Champion c) {
			applyEffect(trigger, c, side, index, duelists);
		} else if (activator instanceof Equipment e) {
			applyEffect(trigger, e, side, index, duelists);
		}
	}

	public void sendWebhookMessage(String message, String gif, Drawable d) {
		for (TextChannel channel : channel.getChannels()) {
			try {
				Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro");
				Card c = d.getCard();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder()
						.setContent(message)
						.setAvatarUrl("https://api.%s/card?name=%s&anime=%s".formatted(System.getenv("SERVER_URL"), c.getId(), c.getAnime().getName()))
						.setUsername(c.getName());

				if (gif != null) {
					wmb.addEmbeds(new WebhookEmbedBuilder()
							.setImageUrl(ShiroInfo.GIFS_URL + "/" + gif + ".gif")
							.build());
				}

				try {
					if (wh == null) return;
					WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
					wc.send(wmb.build()).get();
				} catch (InterruptedException | ExecutionException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException | NullPointerException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}
	}

	public int getAttributeSum(Side s, boolean attacking) {
		List<SlotColumn> slts = arena.getSlots().get(s);

		return slts.parallelStream()
				.map(SlotColumn::getTop)
				.filter(Objects::nonNull)
				.mapToInt(c -> attacking
						? c.isDefending() ? 0 : c.getFinAtk()
						: c.isDefending() ? c.getFinDef() : c.getFinAtk()
				).sum();
	}

	public boolean isInGraveyard(Side s, String id) {
		return arena.getGraveyard().get(s).parallelStream()
				.map(d -> d.getCard().getId())
				.anyMatch(id::equalsIgnoreCase);
	}

	public Champion evolveTo(Champion from, String to, EffectParameters ep) {
		if (fusionLock > 0) return from;

		Hand h = hands.get(ep.getSide());
		Champion nc = CardDAO.getChampion(to);
		if (nc == null) return from;

		if (h.isNullMode() && h.getHp() <= nc.getBaseStats() / 2) return from;
		else if (h.getMana() < nc.getMana() || h.getHp() <= nc.getBlood()) return from;

		if (nc.getMana() > 0) {
			if (h.isNullMode())
				h.removeHp(nc.getBaseStats() / 2);
			else
				h.removeMana(nc.getMana());
		}
		h.removeHp(nc.getBlood());

		nc.setDefending(from.isDefending(), false);

		int i = from.getIndex();
		banCard(ep.getSide(), i, false);
		arena.getSlots().get(ep.getSide()).get(i).setTop(nc);
		return nc;
	}

	public int isHeroInField(Side s) {
		Hand h = getHands().get(s);
		if (h.getHero() == null) return -1;

		for (SlotColumn sc : arena.getSlots().get(s)) {
			if (sc.getTop() != null && Objects.equals(sc.getTop().getHero(), h.getHero()))
				return sc.getIndex();
		}

		return -1;
	}

	public Side getCurrentSide() {
		return getRound() % 2 == 0 ? Side.BOTTOM : Side.TOP;
	}

	public Side getNextSide() {
		return getCurrentSide().getOther();
	}

	public List<Champion> getFusions() {
		return List.copyOf(fusions);
	}

	public boolean isTeam() {
		return team;
	}

	public Map<Achievement, JSONObject> getAchData() {
		return achData;
	}

	public boolean isReroll() {
		return reroll;
	}

	public void setReroll(boolean reroll) {
		this.reroll = reroll;
	}

	public GameState getOldState() {
		return oldState;
	}

	public void setState(GameState oldState) {
		this.oldState = oldState;
	}

	public Phase getPhase() {
		return phase;
	}

	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	public SlotColumn getSlot(Side s, int index) {
		return Helper.safeGet(arena.getSlots().get(s), index);
	}

	@Override
	public void resetTimer(Shoukan shkn) {
		forfeit = false;

		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> a.isInvalid(this, e.getKey(), false));
		}

		decreaseFLockTime();
		decreaseSLockTime();
		decreaseELockTime();

		int idx = getCurrentSide() == Side.TOP ? 1 : 0;
		if (synthCd[idx] > 0) {
			synthCd[idx]--;
		}
		if (undyingCd[idx] > 0) {
			undyingCd[idx]--;
		}

		if (team) ((TeamHand) hands.get(getCurrentSide())).next();
		super.resetTimer(shkn);
	}

	@Override
	public void close() {
		if (!isOpen()) return;

		listener.close();
		getHistory().getRound(getRound() + 1).setData(
				hands.get(getCurrentSide()),
				arena.getSlots().get(getCurrentSide())
		);
		super.close(rules.official());

		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> a.isInvalid(this, e.getKey(), true));
		}

		if (!draw && rules.official()) {
			for (Side s : Side.values()) {
				Hand h = hands.get(s);
				if (h instanceof TeamHand th) {
					for (int i = 0; i < 2; i++) {
						Account acc = AccountDAO.getAccount(h.getUser().getId());

						if (acc.hasPendingQuest()) {
							Map<DailyTask, Integer> pg = acc.getDailyProgress();
							DailyQuest dq = DailyQuest.getQuest(Long.parseLong(acc.getUid()));
							int summons = summoned.get(s).getOrDefault(dq.getChosenRace(), 0);
							pg.merge(DailyTask.RACE_TASK, summons, Integer::sum);
							acc.setDailyProgress(pg);
						}

						EnumSet<Achievement> achs = achievements.getOrDefault(s, EnumSet.noneOf(Achievement.class));
						achs.removeIf(a -> !achData.getOrDefault(a, new JSONObject()).getBoolean("completed", true));
						acc.getAchievements().addAll(achs);

						if (!achs.isEmpty())
							acc.addGem(achs.stream().mapToInt(Achievement::getValue).sum());

						AccountDAO.saveAccount(acc);

						if (h.getHero() != null && tourMatch == null) {
							Hero hr = KawaiponDAO.getHero(h.getAcc().getUid());

							if (hr != null && hr.equals(h.getHero())) {
								if (isRanked() && Helper.chance(5)) {
									h.sendDM(":bulb: | Durante esta batalha " + hr.getName() + " obteve 2 pontos bônus de atributo devido à experiência de combate. GG!");
									hr.addBonusPoints(2);
								}

								KawaiponDAO.saveHero(h.getHero());
							}
						}

						th.next();
					}
				} else {
					Account acc = AccountDAO.getAccount(h.getUser().getId());

					if (acc.hasPendingQuest()) {
						Map<DailyTask, Integer> pg = acc.getDailyProgress();
						DailyQuest dq = DailyQuest.getQuest(Long.parseLong(acc.getUid()));
						int summons = summoned.get(s).getOrDefault(dq.getChosenRace(), 0);
						pg.merge(DailyTask.RACE_TASK, summons, Integer::sum);
						acc.setDailyProgress(pg);
					}

					EnumSet<Achievement> achs = achievements.getOrDefault(s, EnumSet.noneOf(Achievement.class));
					achs.removeIf(a -> !achData.getOrDefault(a, new JSONObject()).getBoolean("completed", true));
					acc.getAchievements().addAll(achs);

					if (!achs.isEmpty())
						acc.addGem(achs.stream().mapToInt(Achievement::getValue).sum());

					AccountDAO.saveAccount(acc);

					if (h.getHero() != null && tourMatch == null) {
						Hero hr = KawaiponDAO.getHero(h.getAcc().getUid());

						if (hr != null && hr.equals(h.getHero())) {
							if (isRanked() && Helper.chance(5)) {
								h.sendDM(":bulb: | Durante esta batalha " + hr.getName() + " obteve 2 pontos bônus de atributo devido à experiência de combate. GG!");
								hr.addBonusPoints(2);
							}

							KawaiponDAO.saveHero(hr);
						}
					}
				}
			}

			if (tourMatch != null) {
				int winner = switch (getHistory().getWinner()) {
					case TOP -> tourMatch.topIndex();
					case BOTTOM -> tourMatch.botIndex();
				};

				Tournament t = TournamentDAO.getTournament(tourMatch.id());
				t.setResult(tourMatch.phase(), winner);

				TournamentDAO.save(t);
			}
		}

		if (!getFrames().isEmpty() && Main.getInfo().getEncoderClient() != null) {
			try {
				getFrames().add(Helper.compress(Helper.atob(getArena().addHands(arena.render(this, hands), hands.values()), "jpg")));
			} catch (IOException ignore) {
			}

			channel.sendMessage("Deseja baixar o replay desta partida?")
					.queue(s -> Pages.buttonize(s, Map.of(
									Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
										wrapper.getMessage().delete().queue(null, Helper::doNothing);
										wrapper.getChannel().sendMessage("<a:loading:697879726630502401> Aguardando conexão com API...")
												.flatMap(m -> {
													while (!Main.getInfo().isEncoderConnected()) {
														try {
															Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
															Thread.sleep(2000);
														} catch (URISyntaxException | DeploymentException | IOException | InterruptedException e) {
															Helper.logger(ShiroInfo.class).error(e + " | " + e.getStackTrace()[0]);
														}
													}

													return m.editMessage("<a:loading:697879726630502401> Processando replay...");
												})
												.queue(m -> {
													EmbedBuilder eb = new EmbedBuilder();
													try {
														String url = Main.getInfo().getEncoderClient()
																.requestEncoding(String.valueOf(hashCode()), getFrames())
																.get(1, TimeUnit.HOURS);

														if (url == null) throw new TimeoutException();
														eb.setColor(Color.green)
																.setTitle("Replay pronto!")
																.setDescription("[Clique aqui](" + url + ") para baixar o replay desta partida (o replay poderá ser baixado durante os próximos 30 minutos).");
													} catch (Exception e) {
														Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
														eb.setColor(Color.red)
																.setTitle("Erro!")
																.setDescription("Houve um erro ao processar o replay, meus desenvolvedores já foram notificados.");
													}

													m.editMessage(wrapper.getUser().getAsMention())
															.setEmbeds(eb.build())
															.queue(null, Helper::doNothing);
												});
									}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> hands.values().parallelStream().anyMatch(h -> h.getUser().getId().equals(u.getId()))
					));
		}
	}
}
