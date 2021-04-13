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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.ClusterAction;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.RankedQueue;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class Shoukan extends GlobalGame {
	private final Map<Side, Hand> hands;
	private final Map<Side, Pair<Race, Race>> combos;
	private final Map<Side, Clan> clans;
	private final GameChannel channel;
	private final Arena arena = new Arena();
	private final SimpleMessageListener listener = new SimpleMessageListener() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private final Map<String, Message> message = new HashMap<>();
	private final List<Champion> fusions = CardDAO.getFusions();
	private final boolean[] changed = {false, false, false, false, false};
	private final boolean daily;
	private final boolean team;
	private final Map<Side, Map<Race, Integer>> summoned = Map.of(
			Side.TOP, new HashMap<>(),
			Side.BOTTOM, new HashMap<>()
	);
	private final List<EffectOverTime> eot = new ArrayList<>();
	private Phase phase = Phase.PLAN;
	private boolean draw = false;
	private Side current = Side.BOTTOM;
	private Side next = Side.TOP;
	private int fusionLock = 0;
	private int spellLock = 0;
	private int effectLock = 0;
	private final List<Drawable> discardBatch = new ArrayList<>();
	private boolean reroll = true;
	private boolean moveLock = false;

	public Shoukan(ShardManager handler, GameChannel channel, int bet, JSONObject custom, boolean daily, boolean ranked, List<Clan> clans, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, custom);
		this.channel = channel;
		this.daily = daily;
		this.team = false;

		this.hands = Map.of(
				Side.TOP, new Hand(this, players[0], clans.get(0), Side.TOP),
				Side.BOTTOM, new Hand(this, players[1], clans.get(1), Side.BOTTOM)
		);
		this.combos = Map.of(
				Side.TOP, hands.get(Side.TOP).getCombo(),
				Side.BOTTOM, hands.get(Side.BOTTOM).getCombo()
		);
		this.clans = Map.of(
				Side.TOP, clans.get(0),
				Side.BOTTOM, clans.get(1)
		);

		if (custom == null)
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				},
				s -> {
					if (custom == null) {
						if (ranked) {
							MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(getCurrent().getId());
							mmr.block(30, ChronoUnit.MINUTES);
							MatchMakingRatingDAO.saveMMR(mmr);
						}

						getHistory().setWinner(next);
						getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
						close();
					}
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				}
		);
	}

	public Shoukan(ShardManager handler, GameChannel channel, int bet, JSONObject custom, boolean daily, boolean ranked, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, custom, players.length == 4 ? RankedQueue.DUO : RankedQueue.SOLO);
		this.channel = channel;
		this.daily = daily;
		this.team = players.length == 4;

		if (team) {
			List<Kawaipon> kps = daily ?
					Collections.nCopies(4, Helper.getDailyDeck()) :
					List.of(
							KawaiponDAO.getKawaipon(players[2].getId()),
							KawaiponDAO.getKawaipon(players[0].getId()),
							KawaiponDAO.getKawaipon(players[3].getId()),
							KawaiponDAO.getKawaipon(players[1].getId())
					);

			this.hands = Map.of(
					Side.TOP, new TeamHand(this, List.of(players[2], players[0]), kps.subList(0, 2), Side.TOP),
					Side.BOTTOM, new TeamHand(this, List.of(players[3], players[1]), kps.subList(2, 4), Side.BOTTOM)
			);
		} else {
			Kawaipon p1 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[0].getId());
			Kawaipon p2 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[1].getId());

			this.hands = Map.of(
					Side.TOP, new Hand(this, players[0], p1, Side.TOP),
					Side.BOTTOM, new Hand(this, players[1], p2, Side.BOTTOM)
			);
		}
		this.combos = Map.of(
				Side.TOP, hands.get(Side.TOP).getCombo(),
				Side.BOTTOM, hands.get(Side.BOTTOM).getCombo()
		);
		this.clans = null;

		if (custom == null)
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				},
				s -> {
					if (custom == null) {
						if (ranked) {
							MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(getCurrent().getId());
							mmr.block(30, ChronoUnit.MINUTES);
							MatchMakingRatingDAO.saveMMR(mmr);
						}

						getHistory().setWinner(next);
						getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
						close();
					}
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				}
		);
	}

	@Override
	public void start() {
		Hand h = hands.get(current);
		h.addMana(h.getManaPerTurn());
		if (combos.get(current).getRight() == Race.BESTIAL)
			h.addMana(1);

		AtomicBoolean shownHand = new AtomicBoolean(false);
		AtomicReference<String> previous = new AtomicReference<>("");
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
				.queue(s -> {
					this.message.put(s.getChannel().getId(), s);
					if (!s.getGuild().getId().equals(previous.get())) {
						previous.set(s.getGuild().getId());
						Main.getInfo().getShiroEvents().addHandler(s.getGuild(), listener);
					}
					Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					if (!shownHand.get()) {
						shownHand.set(true);
						h.showHand();
					}
				});
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> channel.getChannels().stream().anyMatch(g -> e.getChannel().getId().equals(g.getId()));

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

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = hands.get(current);

		if (cmd.equalsIgnoreCase("reload")) {
			moveLock = true;
			channel.sendMessage(message.getAuthor().getName() + " recriou a mensagem do jogo.")
					.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
					.queue(s -> {
						this.message.compute(s.getChannel().getId(), (id, m) -> {
							if (m != null)
								m.delete().queue(null, Helper::doNothing);
							return s;
						});
						Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						moveLock = false;
					});
			return;
		}

		String[] args = cmd.split(",");
		if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
			return;
		}
		int index = Integer.parseInt(args[0]) - 1;

		if (phase == Phase.PLAN) {
			try {
				List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(current);
				if (args.length == 1) {
					if (index < 0 || index >= slots.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = slots.get(index).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (changed[index]) {
						channel.sendMessage("❌ | Você já mudou a postura dessa carta neste turno.").queue(null, Helper::doNothing);
						return;
					} else if (c.getStun() > 0) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					}

					if (applyEot(ON_SWITCH, current, index)) return;
					if (applyEffect(ON_SWITCH, c, index, current, Pair.of(c, index), null)) return;

					ClusterAction act;
					if (c.isFlipped()) {
						c.setFlipped(false);
						c.setDefending(true);
						act = channel.sendMessage("Carta virada para cima em modo de defesa.");
					} else if (c.isDefending()) {
						c.setDefending(false);
						act = channel.sendMessage("Carta trocada para modo de ataque.");
					} else {
						c.setDefending(true);
						act = channel.sendMessage("Carta trocada para modo de defesa.");
					}

					changed[index] = true;
					resetTimerKeepTurn();
					moveLock = true;
					act.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
							});
					return;
				}

				Drawable d = h.getCards().get(index);
				String msg;

				if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
					return;
				}

				if (args[1].equalsIgnoreCase("d") && args.length < 3) {
					discardBatch.add(d.copy());
					d.setAvailable(false);

					if (makeFusion(h)) return;

					resetTimerKeepTurn();
					AtomicBoolean shownHand = new AtomicBoolean(false);
					moveLock = true;
					channel.sendMessage(h.getUser().getName() + " descartou a carta " + d.getCard().getName() + ".")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
								if (!shownHand.get()) {
									shownHand.set(true);
									h.showHand();
								}
							});
					return;
				}

				if (d instanceof Equipment) {
					Equipment e = (Equipment) d.copy();

					if (e.getCharm() != null && e.getCharm().equals(Charm.SPELL)) {
						if (!args[1].equalsIgnoreCase("s")) {
							channel.sendMessage("❌ | O segundo argumento precisa ser `S` se deseja jogar uma carta de feitiço.").queue(null, Helper::doNothing);
							return;
						} else if (spellLock > 0) {
							channel.sendMessage("❌ | Feitiços estão bloqueados por mais " + (fusionLock == 1 ? "turno" : "turnos") + ".").queue(null, Helper::doNothing);
							return;
						} else if (!h.isNullMode() && (h.getMana() < e.getMana())) {
							channel.sendMessage("❌ | Você não tem mana suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
							return;
						} else if (args.length - 2 < e.getArgType().getArgs()) {
							channel.sendMessage(
									switch (e.getArgType()) {
										case ALLY -> "❌ | Este feitiço requer um alvo aliado.";
										case ENEMY -> "❌ | Este feitiço requer um alvo inimigo.";
										case BOTH -> "❌ | Este feitiço requer um alvo aliado e um inimigo.";
										default -> "";
									}
							).queue(null, Helper::doNothing);
							return;
						}

						Pair<Champion, Integer> allyPos = null;
						Pair<Champion, Integer> enemyPos = null;

						switch (e.getArgType()) {
							case ALLY -> {
								if (!StringUtils.isNumeric(args[2])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos = Integer.parseInt(args[2]) - 1;
								Champion target = slots.get(pos).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
									return;
								}

								allyPos = Pair.of(target, pos);
							}
							case ENEMY -> {
								if (!StringUtils.isNumeric(args[2])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta inimiga para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos = Integer.parseInt(args[2]) - 1;
								List<SlotColumn<Champion, Equipment>> eSlots = arena.getSlots().get(next);
								Champion target = eSlots.get(pos).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos);
							}
							case BOTH -> {
								if (!StringUtils.isNumeric(args[2]) || !StringUtils.isNumeric(args[3])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada e uma inimiga para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos1 = Integer.parseInt(args[2]) - 1;
								int pos2 = Integer.parseInt(args[3]) - 1;
								Champion target = slots.get(pos1).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na primeira casa.").queue(null, Helper::doNothing);
									return;
								}

								allyPos = Pair.of(target, pos1);
								List<SlotColumn<Champion, Equipment>> eSlots = arena.getSlots().get(next);
								target = eSlots.get(pos2).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na segunda casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos2);
							}
						}

						reroll = false;
						d.setAvailable(false);
						h.removeMana(e.getMana());
						e.activate(h, hands.get(next), this, allyPos == null ? -1 : allyPos.getRight(), enemyPos == null ? -1 : enemyPos.getRight());
						arena.getGraveyard().get(current).add(e.copy());

						if (makeFusion(h)) return;

						String result = switch (e.getArgType()) {
							case NONE -> h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + ".";
							case ALLY -> {
								assert allyPos != null;
								if (allyPos.getLeft().isFlipped()) {
									allyPos.getLeft().setFlipped(false);
									allyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName()
								);
							}
							case ENEMY -> {
								assert enemyPos != null;
								if (enemyPos.getLeft().isFlipped()) {
									enemyPos.getLeft().setFlipped(false);
									enemyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
							case BOTH -> {
								assert allyPos != null && enemyPos != null;
								if (allyPos.getLeft().isFlipped()) {
									allyPos.getLeft().setFlipped(false);
									allyPos.getLeft().setDefending(true);
								}
								if (enemyPos.getLeft().isFlipped()) {
									enemyPos.getLeft().setFlipped(false);
									enemyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s e %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
						};

						if (!postCombat()) {
							resetTimerKeepTurn();
							AtomicBoolean shownHand = new AtomicBoolean(false);
							moveLock = true;
							channel.sendMessage(result)
									.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
									.queue(s -> {
										this.message.compute(s.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return s;
										});
										Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
										moveLock = false;
										if (!shownHand.get()) {
											shownHand.set(true);
											h.showHand();
										}
									});
						}
						return;
					}

					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser o número da casa da carta à equipar este equipamento.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}

					int dest = Integer.parseInt(args[1]) - 1;
					SlotColumn<Champion, Equipment> slot = slots.get(dest);

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue(null, Helper::doNothing);
						return;
					}
					int toEquip = Integer.parseInt(args[2]) - 1;

					SlotColumn<Champion, Equipment> target = slots.get(toEquip);

					if (target.getTop() == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					slot.setBottom(e);
					Champion t = target.getTop();
					if (t.isFlipped()) {
						t.setFlipped(false);
						t.setDefending(true);
					}
					t.addLinkedTo(e);
					e.setLinkedTo(Pair.of(toEquip, t));
					if (applyEot(ON_EQUIP, current, toEquip)) return;
					if (applyEffect(ON_EQUIP, t, toEquip, current, Pair.of(t, toEquip), null)) return;

					if (e.getCharm() != null) {
						switch (e.getCharm()) {
							case TIMEWARP -> {
								t.getEffect(new EffectParameters(BEFORE_TURN, this, toEquip, current, Duelists.of(t, toEquip, null, -1), channel));
								t.getEffect(new EffectParameters(AFTER_TURN, this, toEquip, current, Duelists.of(t, toEquip, null, -1), channel));
							}
							case DOUBLETAP -> t.getEffect(new EffectParameters(ON_SUMMON, this, toEquip, current, Duelists.of(t, toEquip, null, -1), channel));
							case DOPPELGANGER -> {
								SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(current, true);

								if (sc != null) {
									Champion dp = t.copy();
									dp.addRedAtk(Math.round(dp.getAltAtk() * 0.25f));
									dp.addRedDef(Math.round(dp.getAltDef() * 0.25f));
									dp.setBonus(t.getBonus().copy());
									dp.setEfctMana(-dp.getMana());
									dp.setFusion(true);

									sc.setTop(dp);
								}
							}
						}

						if (postCombat()) return;
					}

					msg = h.getUser().getName() + " equipou " + e.getCard().getName() + " em " + t.getName() + ".";
				} else if (d instanceof Champion) {
					Champion c = (Champion) d.copy();
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (!h.isNullMode() && (h.getMana() < c.getMana())) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}
					int dest = Integer.parseInt(args[1]) - 1;

					SlotColumn<Champion, Equipment> slot = slots.get(dest);

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					switch (args[2].toLowerCase(Locale.ROOT)) {
						case "a" -> {
							c.setFlipped(false);
							c.setDefending(false);
						}
						case "d" -> {
							c.setFlipped(false);
							c.setDefending(true);
						}
						case "b" -> {
							c.setFlipped(true);
							c.setDefending(true);
						}
						default -> {
							channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
							return;
						}
					}

					reroll = false;
					d.setAvailable(false);
					slot.setTop(c);
					if (applyEot(ON_SUMMON, current, dest)) return;
					if (applyEffect(ON_SUMMON, c, dest, current, Pair.of(c, dest), null)) return;

					summoned.get(current).merge(c.getRace(), 1, Integer::sum);

					msg = h.getUser().getName() + " invocou " + (c.isFlipped() ? "uma carta virada para baixo" : c.getName() + " em posição de " + (c.isDefending() ? "defesa" : "ataque")) + ".";
				} else {
					if (!args[1].equalsIgnoreCase("f")) {
						channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					Field f = (Field) d.copy();
					d.setAvailable(false);
					arena.setField(f);
					msg = h.getUser().getName() + " invocou o campo " + f.getCard().getName() + ".";
				}

				if (d instanceof Champion)
					h.removeMana(((Champion) d).getMana());

				if (makeFusion(h)) return;

				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				moveLock = true;
				channel.sendMessage(msg)
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue(null, Helper::doNothing);
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão e o segundo deve ser uma casa vazia no tabuleiro.").queue(null, Helper::doNothing);
			}
		} else {
			try {
				if (args.length > 1 && !StringUtils.isNumeric(args[1])) {
					channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue(null, Helper::doNothing);
					return;
				}

				int[] is = {index, args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1};

				List<SlotColumn<Champion, Equipment>> yourSide = arena.getSlots().get(current);
				List<SlotColumn<Champion, Equipment>> hisSide = arena.getSlots().get(next);

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = yourSide.get(is[0]).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (hisSide.stream().anyMatch(s -> s.getTop() != null)) {
						channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
						return;
					} else if (!c.isAvailable()) {
						channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
						return;
					} else if (c.isFlipped()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (c.getStun() > 0) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					} else if (c.isDefending()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
						return;
					}

					Hand enemy = hands.get(next);

					float demonFac = 1;

					if (h.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.25f;
					if (enemy.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.33f;

					int yPower = Math.round(c.getFinAtk() * (getRound() < 2 ? 0.5f : 1));

					if (!c.getCard().getId().equals("DECOY")) enemy.removeHp(yPower);
					c.setAvailable(false);

					if (!postCombat()) {
						resetTimerKeepTurn();
						moveLock = true;
						channel.sendMessage("%s atacou %s causando %s de dano direto!%s".formatted(
								c.getName(),
								hands.get(next).getUser().getName(),
								yPower,
								(getRound() < 2 ? " (dano reduzido por ser o 1º turno)" : "")
								+ (demonFac > 1 ? " (efeito de raça: +" + Helper.roundToString(demonFac, 0) + "%)" : "")
								)
						).addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(s -> {
									this.message.compute(s.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return s;
									});
									Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
									moveLock = false;
								});
					}
					return;
				}

				Champion yours = yourSide.get(is[0]).getTop();
				Champion his = hisSide.get(is[1]).getTop();

				if (yours == null || his == null) {
					channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
					return;
				} else if (!yours.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isFlipped()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
					return;
				} else if (yours.getStun() > 0) {
					channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isDefending()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
					return;
				}

				attack(current, next, is);
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue(null, Helper::doNothing);

			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue(null, Helper::doNothing);
			}
		}
	}

	public void attack(Side current, Side next, int[] is) {
		Champion yours = getArena().getSlots().get(current).get(is[0]).getTop();
		Champion his = getArena().getSlots().get(next).get(is[1]).getTop();

		if (yours.isDefending()) return;

		if (applyEot(ON_ATTACK, current, is[0])) return;
		if (is[0] > 0) {
			Champion c = arena.getSlots().get(current).get(is[0] - 1).getTop();
			if (c != null && applyEffect(ATTACK_ASSIST, c, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1])))
				return;
		}
		if (is[0] < 4) {
			Champion c = arena.getSlots().get(current).get(is[0] + 1).getTop();
			if (c != null && applyEffect(ATTACK_ASSIST, c, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1])))
				return;
		}
		if (applyEffect(ON_ATTACK, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

		if (yours.getBonus().getSpecialData().remove("skipCombat") != null || yours.getCard().getId().equals("DECOY")) {
			yours.setAvailable(false);
			yours.resetAttribs();
			if (applyEot(POST_ATTACK, current, is[0])) return;
			if (applyEffect(POST_ATTACK, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			resetTimerKeepTurn();
			moveLock = true;
			channel.sendMessage("Cálculo de combate ignorado por efeito do atacante!")
					.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
					.queue(s -> {
						this.message.compute(s.getChannel().getId(), (id, m) -> {
							if (m != null)
								m.delete().queue(null, Helper::doNothing);
							return s;
						});
						Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						moveLock = false;
					});
			return;
		}

		if (applyEot(ON_DEFEND, next, is[1])) return;
		if (is[1] > 0) {
			Champion c = arena.getSlots().get(next).get(is[1] - 1).getTop();
			if (c != null && applyEffect(DEFENSE_ASSIST, c, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1])))
				return;
		}
		if (is[1] < 4) {
			Champion c = arena.getSlots().get(next).get(is[1] + 1).getTop();
			if (c != null && applyEffect(DEFENSE_ASSIST, c, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1])))
				return;
		}
		if (applyEffect(ON_DEFEND, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

		if (his.getBonus().getSpecialData().remove("skipCombat") != null || his.getCard().getId().equals("DECOY")) {
			if (applyEot(POST_DEFENSE, next, is[1])) return;
			if (applyEffect(POST_DEFENSE, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			resetTimerKeepTurn();
			moveLock = true;
			channel.sendMessage("Cálculo de combate ignorado por efeito do defensor!")
					.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
					.queue(s -> {
						this.message.compute(s.getChannel().getId(), (id, m) -> {
							if (m != null)
								m.delete().queue(null, Helper::doNothing);
							return s;
						});
						Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						moveLock = false;
					});
			return;
		}

		int yPower;
		if (!yours.getCard().getId().equals("DECOY")) {
			yPower = yours.getFinAtk();
		} else {
			yPower = 0;
		}

		int hPower;
		if (his.isDefending()) {
			if (his.isFlipped()) {
				his.setFlipped(false);
				his.setDefending(true);
				if (applyEot(ON_FLIP, next, is[1])) return;
				if (applyEffect(ON_FLIP, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;
			}
			hPower = his.getFinDef();
		} else if (!his.getCard().getId().equals("DECOY")) {
			hPower = his.getFinAtk();
		} else {
			hPower = 0;
		}

		boolean yourDodge = yours.getDodge() > 0 && Helper.chance(yours.getDodge());
		boolean hisDodge = his.getDodge() > 0 && Helper.chance(his.getDodge());

		if (yPower > hPower || (yPower == hPower && yourDodge)) {
			yours.setAvailable(false);
			yours.resetAttribs();

			if (hisDodge) {
				if (applyEot(ON_MISS, current, is[0])) return;
				if (applyEffect(ON_MISS, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

				if (applyEot(ON_DODGE, next, is[1])) return;
				if (applyEffect(ON_DODGE, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage(his.getName() + " esquivou do ataque de " + yours.getName() + "! (" + Helper.roundToString(his.getDodge(), 1) + "%)")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			} else {
				if (applyEot(POST_ATTACK, current, is[0])) return;
				if (applyEffect(POST_ATTACK, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

				if (applyEot(ON_DEATH, next, is[1])) return;
				if (applyEffect(ON_DEATH, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

				if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
					if (his.isDefending()) {
						int apDamage = yours.getLinkedTo().stream().filter(e -> e.getCharm() == Charm.ARMORPIERCING).mapToInt(Equipment::getAtk).sum();
						Hand enemy = hands.get(next);
						enemy.removeHp(apDamage);
					} else if (!(his.isDefending() || his.getStun() > 0) && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
						Hand enemy = hands.get(next);
						if (yours.getBonus().getSpecialData().has("totalDamage"))
							enemy.removeHp(yPower);
						else {
							enemy.removeHp(Math.round(yPower - hPower));
						}
					}
				}

				if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
					killCard(next, is[1]);
					if (!postCombat()) {
						resetTimerKeepTurn();
						moveLock = true;
						channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "! (" + yPower + " > " + hPower + ")")
								.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(s -> {
									this.message.compute(s.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return s;
									});
									Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
									moveLock = false;
								});
					}
				} else if (yours.getCard().getId().equals("DECOY")) {
					resetTimerKeepTurn();
					moveLock = true;
					channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
							});
				} else {
					killCard(next, is[1]);
					resetTimerKeepTurn();
					moveLock = true;
					channel.sendMessage("Essa carta era na verdade uma isca!")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
							});
				}
			}
		} else if (yPower < hPower || hisDodge) {
			yours.setAvailable(false);
			his.resetAttribs();

			if (applyEot(ON_SUICIDE, current, is[0])) return;
			if (applyEffect(ON_SUICIDE, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			if (applyEot(POST_DEFENSE, next, is[1])) return;
			if (applyEffect(POST_DEFENSE, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (yours.getBonus().getSpecialData().remove("noDamage") == null && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
					Hand you = hands.get(current);
					you.removeHp(hPower - yPower);
				}
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(current, is[0]);
				if (!postCombat()) {
					resetTimerKeepTurn();
					moveLock = true;
					channel.sendMessage(yours.getCard().getName() + " não conseguiu derrotar " + his.getName() + "! (" + yPower + " < " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
							});
				}
			} else if (his.getCard().getId().equals("DECOY")) {
				killCard(current, is[0]);
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage(yours.getName() + " não conseguiu derrotar " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			} else {
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage("Essa carta era na verdade uma isca!")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			}
		} else {
			yours.setAvailable(false);

			if (applyEot(ON_SUICIDE, current, is[0])) return;
			if (applyEffect(ON_SUICIDE, yours, is[0], current, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			if (applyEot(ON_DEATH, next, is[1])) return;
			if (applyEffect(ON_DEATH, his, is[1], next, Pair.of(yours, is[0]), Pair.of(his, is[1]))) return;

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(next, is[1]);
				killCard(current, is[0]);

				if (!postCombat()) {
					resetTimerKeepTurn();
					moveLock = true;
					channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
							});
				}
			} else if (Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(next, is[1]);
				killCard(current, is[0]);
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage("As duas cartas na verdade eram iscas! (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			} else if (his.getCard().getId().equals("DECOY")) {
				killCard(next, is[1]);
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			} else {
				killCard(current, is[0]);
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			}
		}
	}

	public void forceAttack(int[] is) {
		attack(next, current, is);
	}

	private boolean makeFusion(Hand h) {
		if (fusionLock > 0) return false;
		List<Champion> champsInField = arena.getSlots().get(current)
				.stream()
				.map(SlotColumn::getTop)
				.collect(Collectors.toList());

		List<Equipment> equipsInField = arena.getSlots().get(current)
				.stream()
				.map(SlotColumn::getBottom)
				.collect(Collectors.toList());

		List<String> allCards = new ArrayList<>() {{
			addAll(Stream.of(champsInField, equipsInField)
					.flatMap(List::stream)
					.filter(Objects::nonNull)
					.map(dr -> dr.getCard().getId())
					.collect(Collectors.toList())
			);

			if (getArena().getField() != null)
				add(getArena().getField().getCard().getId());
		}};

		Champion aFusion = fusions
				.stream()
				.filter(f ->
						f.getRequiredCards().size() > 0 &&
						allCards.containsAll(f.getRequiredCards()) &&
						(h.isNullMode() || h.getMana() >= f.getMana())
				)
				.findFirst()
				.map(Champion::copy)
				.orElse(null);

		if (aFusion != null) {
			List<SlotColumn<Champion, Equipment>> slts = arena.getSlots().get(current);

			for (String requiredCard : aFusion.getRequiredCards()) {
				for (int i = 0; i < slts.size(); i++) {
					SlotColumn<Champion, Equipment> column = slts.get(i);
					if (column.getTop() != null && column.getTop().getCard().getId().equals(requiredCard)) {
						banishCard(current, i, false);
						break;
					} else if (column.getBottom() != null && column.getBottom().getCard().getId().equals(requiredCard)) {
						banishCard(current, i, true);
						break;
					}
				}
			}

			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> slt = slts.get(i);
				if (slt.getTop() == null) {
					aFusion.setGame(this);
					aFusion.setAcc(AccountDAO.getAccount(h.getUser().getId()));
					slt.setTop(aFusion);
					if (applyEot(ON_SUMMON, current, i)) return true;
					if (applyEffect(ON_SUMMON, aFusion, i, current, Pair.of(aFusion, i), null)) return true;

					h.removeMana(aFusion.getMana());
					break;
				}
			}

			return makeFusion(h);
		}
		return false;
	}

	public void killCard(Side to, int index) {
		Champion ch = getArena().getSlots().get(to).get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventDeath")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(to, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(to, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			arena.getGraveyard().get(to).add(ch.copy());
	}

	public void destroyCard(Side to, int target, Side from, int source) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		Champion chi = getArena().getSlots().get(from).get(source).getTop();

		double chance = Math.min((chi.isFusion() ? 5 : chi.getMana()) * 50 / (ch.isFusion() ? 5 : ch.getMana()), 50) + (50 - ch.getDodge() / 2);

		if (Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if ((eq.getCharm() == Charm.SPELLMIRROR || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLMIRROR) && chi != null) {
						destroyCard(from, source, to, target);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}

			ch.reset();
			if (!ch.isFusion())
				arena.getGraveyard().get(to).add(ch.copy());
		}
	}

	public void destroyCard(Side to, int target) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			arena.getGraveyard().get(to).add(ch.copy());
	}

	public void captureCard(Side to, int target, Side from, int source, boolean withFusion) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		Champion chi = getArena().getSlots().get(from).get(source).getTop();

		double chance = Math.min((chi.isFusion() ? 5 : chi.getMana()) * 50 / (ch.isFusion() ? 5 : ch.getMana()), 50) + (50 - ch.getDodge() / 2);

		if (Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if ((eq.getCharm() == Charm.SPELLMIRROR || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLMIRROR)) {
						captureCard(from, source, to, target, withFusion);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}

			ch.reset();
			if (!ch.isFusion() || withFusion)
				hands.get(to == Side.TOP ? Side.BOTTOM : Side.TOP).getCards().add(ch);
		}
	}

	public void captureCard(Side to, int target, boolean withFusion) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		ch.reset();
		if (!ch.isFusion() || withFusion)
			hands.get(to == Side.TOP ? Side.BOTTOM : Side.TOP).getCards().add(ch);
	}

	public void banishCard(Side side, int index, boolean equipment) {
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);
		if (equipment) {
			Equipment eq = slts.get(index).getBottom();
			if (eq == null) return;

			if (slts.get(eq.getLinkedTo().getLeft()).getTop() != null)
				slts.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
			eq.setLinkedTo(null);

			SlotColumn<Champion, Equipment> sd = slts.get(index);
			arena.getBanished().add(eq);
			sd.setBottom(null);
		} else {
			Champion ch = slts.get(index).getTop();
			if (ch == null) return;

			slts.get(index).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
					killCard(side, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
					banishCard(side, i, true);
			}

			ch.reset();
			if (!ch.isFusion())
				arena.getBanished().add(ch);
		}
	}

	public void unequipCard(Side s, int index, List<SlotColumn<Champion, Equipment>> side) {
		Equipment eq = side.get(index).getBottom();
		if (eq == null) return;

		if (side.get(eq.getLinkedTo().getLeft()).getTop() != null)
			side.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
		eq.setLinkedTo(null);

		SlotColumn<Champion, Equipment> sd = side.get(index);
		arena.getGraveyard().get(s).add(eq.copy());
		sd.setBottom(null);
	}

	public Arena getArena() {
		return arena;
	}

	public Side getSideById(String id) {
		return hands.values().stream()
				.filter(h -> h.getUser().getId().equals(id) || (h instanceof TeamHand && ((TeamHand) h).getUsers().stream().anyMatch(u -> u.getId().equals(id))))
				.map(Hand::getSide)
				.findFirst().orElse(null);
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, Pair<Race, Race>> getCombos() {
		return combos;
	}

	public SlotColumn<Champion, Equipment> getFirstAvailableSlot(Side s, boolean top) {
		for (SlotColumn<Champion, Equipment> slot : arena.getSlots().get(s)) {
			if (top ? slot.getTop() == null : slot.getBottom() == null)
				return slot;
		}
		return null;
	}

	public void convertCard(Side to, int target, Side from, int source) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		Champion chi = getArena().getSlots().get(from).get(source).getTop();

		double chance = Math.min((chi.isFusion() ? 5 : chi.getMana()) * 50 / (ch.isFusion() ? 5 : ch.getMana()), 50) + (50 - ch.getDodge() / 2);

		if (Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if ((eq.getCharm() == Charm.SPELLMIRROR || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLMIRROR) && source != -1) {
						convertCard(from, source, to, target);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(from, true);
			if (sc != null) {
				ch.clearLinkedTo();
				ch.setGame(this);
				ch.setAcc(AccountDAO.getAccount(hands.get(from).getUser().getId()));
				sc.setTop(ch);
				slts.get(target).setTop(null);
				for (int i = 0; i < slts.size(); i++) {
					SlotColumn<Champion, Equipment> sd = slts.get(i);
					if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
						killCard(to, i);

					if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
						unequipCard(to, i, slts);
				}
			}
		}
	}

	public void convertCard(Side to, int target) {
		Side from = to == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(from, true);
		if (sc != null) {
			ch.clearLinkedTo();
			ch.setGame(this);
			ch.setAcc(AccountDAO.getAccount(hands.get(from).getUser().getId()));
			sc.setTop(ch);
			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}
		}
	}

	public void switchCards(Side to, int target, Side from, int source) {
		Champion ch = getArena().getSlots().get(to).get(target).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(to);

		Champion chi = getArena().getSlots().get(from).get(source).getTop();

		double chance = Math.min((chi.isFusion() ? 5 : chi.getMana()) * 50 / (ch.isFusion() ? 5 : ch.getMana()), 50) + (50 - ch.getDodge() / 2);

		if (Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			ch.clearLinkedTo();
			ch.setGame(this);
			ch.setAcc(AccountDAO.getAccount(hands.get(to).getUser().getId()));
			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == source)
					unequipCard(to, i, slts);
			}

			List<SlotColumn<Champion, Equipment>> slots = getArena().getSlots().get(from);

			chi.clearLinkedTo();
			chi.setGame(this);
			chi.setAcc(AccountDAO.getAccount(hands.get(from).getUser().getId()));
			slots.get(source).setTop(null);
			for (int i = 0; i < slots.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slots.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == target)
					killCard(from, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(from, i, slots);
			}

			slts.get(target).setTop(chi);
			slots.get(source).setTop(ch);
		}
	}

	public void convertEquipments(Champion target, int pos, Side to, int index) {
		Side his = to == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = getArena().getSlots().get(his).get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD || ch.getBonus().getSpecialData().opt("charm") == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(to, false);
				if (sc != null) {
					ch.removeLinkedTo(eq);
					slts.get(i).setBottom(null);

					target.addLinkedTo(eq);
					eq.setLinkedTo(Pair.of(pos, target));
					eq.setGame(this);
					eq.setAcc(AccountDAO.getAccount(hands.get(to).getUser().getId()));
					sc.setBottom(eq);
				} else return;
			}
		}
	}

	public boolean postCombat() {
		AtomicBoolean finished = new AtomicBoolean(false);
		for (Map.Entry<Side, Hand> entry : hands.entrySet()) {
			Hand h = entry.getValue();
			if (!finished.get()) {
				Hand op = hands.get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);
				if (h.getHp() == 0) {
					if (getCustom() == null) {
						getHistory().setWinner(op.getSide());
						getBoard().awardWinner(this, daily, op.getUser().getId());
					}

					close();
					finished.set(true);
					if (team)
						channel.sendMessage(((TeamHand) op).getMentions() + " zeraram os pontos de vida de " + ((TeamHand) h).getMentions() + ", temos os vencedores! (" + getRound() + " turnos)")
								.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(msg ->
										this.message.compute(msg.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return msg;
										}));
					else
						channel.sendMessage(op.getUser().getAsMention() + " zerou os pontos de vida de " + h.getUser().getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)")
								.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(msg ->
										this.message.compute(msg.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return msg;
										}));
				}
			}
		}
		return finished.get();
	}

	@Override
	public Map<String, ThrowingBiConsumer<Member, Message>> getButtons() {
		Map<String, ThrowingBiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		if (getRound() < 1 || phase == Phase.ATTACK)
			buttons.put("▶️", (mb, ms) -> {
				User u = getCurrent();

				AtomicReference<Hand> h = new AtomicReference<>(hands.get(current));
				h.get().getCards().removeIf(d -> !d.isAvailable());
				List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(current);

				if (applyEot(AFTER_TURN, current, -1)) return;
				for (int i = 0; i < slots.size(); i++) {
					Champion c = slots.get(i).getTop();
					if (c != null) {
						c.setAvailable(c.getStun() == 0);

						c.resetAttribs();
						if (applyEffect(AFTER_TURN, c, i, current, Pair.of(c, i), null)
							|| makeFusion(h.get())
						) return;
					}
				}

				arena.getGraveyard().get(current).addAll(discardBatch.stream().map(Drawable::copy).collect(Collectors.toList()));
				discardBatch.clear();

				if (getRound() > 0) reroll = false;
				resetTimer(this);

				phase = Phase.PLAN;
				h.set(hands.get(current));
				h.get().decreaseSuppression();
				h.get().decreaseLockTime();
				h.get().decreaseNullTime();
				slots = arena.getSlots().get(current);

				if (applyEot(BEFORE_TURN, current, -1)) return;
				for (int i = 0; i < slots.size(); i++) {
					Champion c = slots.get(i).getTop();
					if (c != null) {
						if (c.getStun() > 0) c.reduceStun();

						if (applyEffect(BEFORE_TURN, c, i, current, Pair.of(c, i), null)
							|| makeFusion(h.get())
						) return;
					}
				}

				h.get().addMana(h.get().getManaPerTurn());
				switch (combos.get(current).getRight()) {
					case BESTIAL -> {
						if (getRound() <= 1)
							h.get().addMana(1);
					}
					case ELF -> {
						if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
							h.get().addMana(1);
					}
				}

				AtomicBoolean shownHand = new AtomicBoolean(false);
				moveLock = true;
				channel.sendMessage(u.getName() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention() + " (turno " + getRound() + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
							if (!shownHand.get()) {
								shownHand.set(true);
								h.get().showHand();
							}
							for (int i = 0; i < 5; i++) {
								changed[i] = false;
							}
						});
			});
		else
			buttons.put("▶️", (mb, ms) -> {
				phase = Phase.ATTACK;
				draw = false;
				reroll = false;
				resetTimerKeepTurn();
				moveLock = true;
				channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
						});
			});
		if (phase == Phase.PLAN)
			buttons.put("\uD83D\uDCE4", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(current);

				int remaining = h.getMaxCards() - h.getCards().size();
				if (remaining <= 0) {
					channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver " + h.getMaxCards() + " ou mais na sua mão.").queue(null, Helper::doNothing);
					return;
				}

				if (!h.manualDraw()) {
					if (getCustom() == null) {
						getHistory().setWinner(next);
						getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
					}

					close();
					if (team)
						channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas no deck, " + ((TeamHand) hands.get(next)).getMentions() + " venceram! (" + getRound() + " turnos)")
								.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(msg ->
										this.message.compute(msg.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return msg;
										}));
					else
						channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas no deck, " + hands.get(next).getUser().getAsMention() + " venceu! (" + getRound() + " turnos)")
								.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
								.queue(msg ->
										this.message.compute(msg.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return msg;
										}));
					return;
				}

				remaining = h.getMaxCards() - h.getCards().size();
				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				moveLock = true;
				channel.sendMessage(getCurrent().getName() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			});
		if (reroll && getRound() == 1 && phase == Phase.PLAN)
			buttons.put("\uD83D\uDD04", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(current);
				h.redrawHand();

				reroll = false;
				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				moveLock = true;
				channel.sendMessage(getCurrent().getName() + " rolou novamente as cartas na mão!")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			});
		if (hands.get(current).getHp() < hands.get(current).getBaseHp() / 3 && hands.get(current).getDestinyDeck().size() > 0 && phase == Phase.PLAN)
			buttons.put("\uD83E\uDDE7", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(current);
				h.destinyDraw();

				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				moveLock = true;
				channel.sendMessage(getCurrent().getName() + " executou um saque do destino!")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							moveLock = false;
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			});
		if (phase == Phase.PLAN)
			buttons.put("\uD83E\uDD1D", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode pedir empate na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				if (draw) {
					close();
					channel.sendMessage("Por acordo mútuo, declaro empate! (" + getRound() + " turnos)")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				} else {
					User u = getCurrent();

					AtomicReference<Hand> h = new AtomicReference<>(hands.get(current));
					h.get().getCards().removeIf(d -> !d.isAvailable());
					List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(current);

					if (applyEot(AFTER_TURN, current, -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						Champion c = slots.get(i).getTop();
						if (c != null) {
							c.setAvailable(c.getStun() == 0);

							c.resetAttribs();
							if (applyEffect(AFTER_TURN, c, i, current, Pair.of(c, i), null)
								|| makeFusion(h.get())
							) return;
						}
					}

					arena.getGraveyard().get(current).addAll(discardBatch.stream().map(Drawable::copy).collect(Collectors.toList()));
					discardBatch.clear();

					if (getRound() > 0) reroll = false;
					resetTimer(this);

					phase = Phase.PLAN;
					h.set(hands.get(current));
					h.get().decreaseSuppression();
					h.get().decreaseLockTime();
					h.get().decreaseNullTime();
					slots = arena.getSlots().get(current);

					if (applyEot(BEFORE_TURN, current, -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						Champion c = slots.get(i).getTop();
						if (c != null) {
							if (c.getStun() > 0) c.reduceStun();

							if (applyEffect(BEFORE_TURN, c, i, current, Pair.of(c, i), null)
								|| makeFusion(h.get())
							) return;
						}
					}

					h.get().addMana(h.get().getManaPerTurn());
					switch (combos.get(current).getRight()) {
						case BESTIAL -> {
							if (getRound() <= 1)
								h.get().addMana(1);
						}
						case ELF -> {
							if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
								h.get().addMana(1);
						}
					}

					AtomicBoolean shownHand = new AtomicBoolean(false);
					draw = true;
					moveLock = true;
					channel.sendMessage(u.getName() + " deseja um acordo de empate, " + getCurrent().getAsMention() + " agora é sua vez, clique em \uD83E\uDD1D caso queira aceitar ou continue jogando normalmente.")
							.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								moveLock = false;
								if (!shownHand.get()) {
									shownHand.set(true);
									h.get().showHand();
								}
								for (int i = 0; i < 5; i++) {
									changed[i] = false;
								}
							});
				}
			});
		if (getRound() > 8)
			buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
				if (getCustom() == null) {
					getHistory().setWinner(next);
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				}

				close();
				channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(this, hands)), "board.jpg")
						.queue(msg ->
								this.message.compute(msg.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return msg;
								}));
			});

		return buttons;
	}

	private void recordLast() {
		Hand top = hands.get(Side.TOP);
		Hand bot = hands.get(Side.BOTTOM);
		getHistory().getRound(getRound() + 1).setSide(current);
		getHistory().getRound(getRound() + 1).setScript(new JSONObject() {{
			put("top", new JSONObject() {{
				put("id", top.getUser().getId());
				put("hp", top.getHp());
				put("mana", top.getMana());
				put("champions", getArena().getSlots().get(Side.TOP)
						.stream()
						.map(SlotColumn::getTop)
						.filter(Objects::nonNull)
						.count()
				);
				put("equipments", getArena().getSlots().get(Side.TOP)
						.stream()
						.map(SlotColumn::getBottom)
						.filter(Objects::nonNull)
						.count()
				);
				put("inHand", top.getCards().size());
				put("deck", top.getDeque().size());
			}});

			put("bottom", new JSONObject() {{
				put("id", bot.getUser().getId());
				put("hp", bot.getHp());
				put("mana", bot.getMana());
				put("champions", getArena().getSlots().get(Side.BOTTOM)
						.stream()
						.map(SlotColumn::getTop)
						.filter(Objects::nonNull)
						.count()
				);
				put("equipments", getArena().getSlots().get(Side.BOTTOM)
						.stream()
						.map(SlotColumn::getBottom)
						.filter(Objects::nonNull)
						.count()
				);
				put("inHand", bot.getCards().size());
				put("deck", bot.getDeque().size());
			}});
		}});
	}

	public void banishWeaklings(int threshold) {
		for (int i = 0; i < 5; i++) {
			Champion c = getArena().getSlots().get(Side.TOP).get(i).getTop();
			if (c != null && c.getMana() <= threshold && !c.isFusion())
				banishCard(
						Side.TOP,
						i,
						false
				);

			c = getArena().getSlots().get(Side.BOTTOM).get(i).getTop();
			if (c != null && c.getMana() <= threshold && !c.isFusion())
				banishCard(
						Side.BOTTOM,
						i,
						false
				);
		}

		for (Map.Entry<Side, LinkedList<Drawable>> entry : getArena().getGraveyard().entrySet()) {
			Hand h = hands.get(entry.getKey());
			List<Champion> dead = entry.getValue().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : dead) {
				entry.getValue().remove(c);
			}

			List<Champion> inHand = h.getCards().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : inHand) {
				h.getCards().remove(c);
			}

			List<Champion> inDeck = h.getDeque().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : inDeck) {
				h.getDeque().remove(c);
			}

			getArena().getBanished().addAll(dead);
			getArena().getBanished().addAll(inHand);
			getArena().getBanished().addAll(inDeck);
		}
	}

	public void dmUser(Hand h, String message) {
		h.getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage(message))
				.queue(null, Helper::doNothing);
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

	public Champion getChampionFromBanished() {
		LinkedList<Drawable> grv = getArena().getBanished();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Champion)
				return (Champion) grv.remove(i);

		return null;
	}

	public Equipment getEquipmentFromBanished() {
		LinkedList<Drawable> grv = getArena().getBanished();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Equipment)
				return (Equipment) grv.remove(i);

		return null;
	}

	public int getFusionLock() {
		return fusionLock;
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

	public void addSLockTime(int time) {
		spellLock += time;
	}

	public void decreaseSLockTime() {
		spellLock = Math.max(0, spellLock - 1);
	}

	public int getEffectLock() {
		return effectLock;
	}

	public void addELockTime(int time) {
		effectLock += time;
	}

	public void decreaseELockTime() {
		effectLock = Math.max(0, effectLock - 1);
	}

	public List<Drawable> getDiscardBatch() {
		return discardBatch;
	}

	public Map<Side, Clan> getClans() {
		return clans;
	}

	public List<EffectOverTime> getEot() {
		return eot;
	}

	public boolean applyEot(EffectTrigger trigger, Side to, int index) {
		if (eot.size() > 0) {
			Iterator<EffectOverTime> i = eot.iterator();
			while (i.hasNext()) {
				EffectOverTime effect = i.next();
				if (effect.getTurns() <= 0) {
					channel.sendMessage(":timer: | O efeito da carta " + effect.getSource() + " expirou!").queue();
					i.remove();
					continue;
				}

				if (effect.getTarget() == null || effect.getTarget() == to) {
					if (effect.getTriggers().contains(trigger))
						effect.getEffect().accept(to, index);

					if (trigger == AFTER_TURN)
						effect.decreaseTurn();
				}
			}

			return postCombat();
		}

		return false;
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, int index, Side side, Pair<Champion, Integer> attacker, Pair<Champion, Integer> defender) {
		if (activator.hasEffect() && effectLock == 0) {
			activator.getEffect(new EffectParameters(trigger, this, index, side, Duelists.of(attacker, defender), channel));
			return postCombat();
		}

		return false;
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
					URL url = this.getClass().getClassLoader().getResource("shoukan/gifs/" + gif + ".gif");
					if (url != null) {
						File f = new File(url.getFile());
						wmb.addFile("effect.gif", f);
					}
				}

				try {
					if (wh == null) return;
					WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
					wc.send(wmb.build()).get();
				} catch (InterruptedException | ExecutionException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	@Override
	public void close() {
		if (!draw && getCustom() == null) {
			for (Side s : Side.values()) {
				Account acc = AccountDAO.getAccount(hands.get(s).getUser().getId());

				if (acc.hasPendingQuest()) {
					Map<DailyTask, Integer> pg = acc.getDailyProgress();
					DailyQuest dq = DailyQuest.getQuest(getCurrent().getIdLong());
					int summons = summoned.get(s).getOrDefault(dq.getChosenRace(), 0);
					pg.merge(DailyTask.RACE_TASK, summons, Integer::sum);
					acc.setDailyProgress(pg);
					AccountDAO.saveAccount(acc);
				}
			}
		}

		listener.close();
		recordLast();
		super.close();
	}

	@Override
	public void resetTimer(Shoukan shkn) {
		getCurrRound().setSide(current);
		decreaseFLockTime();
		decreaseSLockTime();
		decreaseELockTime();
		super.resetTimer(shkn);

		if (team) ((TeamHand) hands.get(current)).next();
		current = next;
		next = current == Side.TOP ? Side.BOTTOM : Side.TOP;
	}
}
