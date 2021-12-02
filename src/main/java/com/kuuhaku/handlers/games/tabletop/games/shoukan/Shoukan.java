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
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.FusionMaterial;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.Source;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.Target;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.states.GameState;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.enums.DailyTask;
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
import org.jetbrains.annotations.NotNull;

import javax.websocket.DeploymentException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
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

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class Shoukan extends GlobalGame implements Serializable {
	private final Map<Side, Hand> hands;
	private final Map<Side, Pair<Race, Race>> combos;
	private final GameChannel channel;
	private final Arena arena;
	private final SimpleMessageListener listener = new SimpleMessageListener() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private final Map<String, Message> message = new HashMap<>();
	private final List<Champion> fusions = CardDAO.getFusions();
	private final boolean team;
	private final boolean record;
	private final Map<Side, EnumSet<Achievement>> achievements = new HashMap<>();
	private final Map<Achievement, JSONObject> achData = new HashMap<>();
	private final Map<Side, Map<Race, Integer>> summoned = Map.of(
			Side.TOP, new HashMap<>(),
			Side.BOTTOM, new HashMap<>()
	);
	private final Set<PersistentEffect> persistentEffects = new HashSet<>();
	private final List<Drawable> discardBatch = new ArrayList<>();
	private final TournamentMatch tourMatch;

	private Phase phase = Phase.PLAN;
	private boolean draw = false;
	private int fusionLock = 0;
	private int spellLock = 0;
	private int effectLock = 0;
	private boolean reroll = true;
	private boolean moveLock = false;
	private int[] synthCd = {0, 0};

	private GameState oldState = null;

	public Shoukan(ShardManager handler, GameChannel channel, int bet, JSONObject custom, boolean daily, boolean ranked, boolean record, TournamentMatch match, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, custom);
		this.channel = channel;
		this.team = players.length == 4;
		this.record = record;
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

		if (custom == null) {
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));
		} else {
			if (custom.getString("arcade").equals("blackrock")) {
				Field f = switch (Helper.rng(5)) {
					case 0 -> CardDAO.getField("THE_SKY_GATES");
					case 1 -> CardDAO.getField("THE_CUBE");
					case 2 -> CardDAO.getField("GREY_AREA");
					case 3 -> CardDAO.getField("BLACK_ROCK_BATTLEFIELD");
					case 4 -> CardDAO.getField("CHARIOTS_LAND");
					case 5 -> CardDAO.getField("DEAD_MASTERS_LAIR");
					default -> throw new IllegalStateException("Unexpected value: " + Helper.rng(5));
				};
				assert f != null;
				arena.setField(f);
			}

			if (custom.has("test") && ShiroInfo.getStaff().contains(players[0].getId())) {
				for (Object o : custom.getJSONArray("test")) {
					String id = String.valueOf(o);
					Drawable d;
					d = CardDAO.getChampion(id);
					if (d == null) d = CardDAO.getEquipment(id);
					if (d == null) d = CardDAO.getField(id);
					if (d == null) continue;

					for (Hand h : hands.values())
						h.getCards().add(d.copy());
				}
			}
		}

		if (ranked)
			for (Map.Entry<Side, Hand> e : hands.entrySet()) {
				Set<Achievement> achs = e.getValue().getAcc().getAchievements();
				if (!achs.isEmpty())
					achievements.put(e.getKey(), EnumSet.complementOf(EnumSet.copyOf(achs)));
				else
					achievements.put(e.getKey(), EnumSet.allOf(Achievement.class));
			}

		setActions(
				s -> {
					for (List<SlotColumn> sides : arena.getSlots().values()) {
						for (SlotColumn slts : sides) {
							if (slts.getTop() != null)
								slts.getTop().setFlipped(false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false);
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
					if (custom == null) {
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
								slts.getTop().setFlipped(false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false);
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
		Hand h = hands.get(getCurrentSide());
		h.addMana(h.getManaPerTurn());
		if (h.getCombo().getRight() == Race.BESTIAL)
			h.addMana(1);

		AtomicBoolean shownHand = new AtomicBoolean(false);
		AtomicReference<String> previous = new AtomicReference<>("");
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
				.queue(s -> {
					this.message.put(s.getChannel().getId(), s);
					if (!s.getGuild().getId().equals(previous.get())) {
						previous.set(s.getGuild().getId());
						ShiroInfo.getShiroEvents().addHandler(s.getGuild(), listener);
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

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = hands.get(getCurrentSide());

		if (cmd.equalsIgnoreCase("reload")) {
			reportEvent(h, message.getAuthor().getName() + " recriou a mensagem do jogo.", false, false);
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
				List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());
				if (args.length == 1) {
					if (index < 0 || index >= slots.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = slots.get(index).getTop();

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

					if (applyEffect(ON_SWITCH, c, getCurrentSide(), index, new Source(c, getCurrentSide(), index)))
						return;

					setSlotChanged(getCurrentSide(), index, true);
					reportEvent(h, msg, true, false);
					return;
				}

				Drawable d = h.getCards().get(index);

				if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
					return;
				}

				if (args[1].equalsIgnoreCase("d") && args.length < 3) {
					discardBatch.add(d);
					d.setAvailable(false);

					if (makeFusion(h)) return;

					reportEvent(h, h.getUser().getName() + " descartou a carta " + d.getCard().getName() + ".", true, false);
					return;
				}

				String msg;
				if (d instanceof Equipment) {
					Equipment e = (Equipment) d.copy();

					if (e.hasEffect()) {
						if (!args[1].equalsIgnoreCase("s")) {
							channel.sendMessage("❌ | O segundo argumento precisa ser `S` se deseja jogar uma carta de magia.").queue(null, Helper::doNothing);
							return;
						} else if (spellLock > 0) {
							channel.sendMessage("❌ | Magias estão bloqueados por mais " + (fusionLock == 1 ? "turno" : "turnos") + ".").queue(null, Helper::doNothing);
							return;
						} else if (!h.isNullMode() && (h.getMana() < e.getMana())) {
							channel.sendMessage("❌ | Você não tem mana suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
							return;
						} else if (h.getHp() <= e.getBlood()) {
							channel.sendMessage("❌ | Você não tem HP suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
							return;
						} else if (args.length - 2 < e.getArgType().getArgs()) {
							channel.sendMessage(
									switch (e.getArgType()) {
										case ALLY -> "❌ | Esta magia requer um alvo aliado.";
										case ENEMY -> "❌ | Esta magia requer um alvo inimigo.";
										case BOTH -> "❌ | Esta magia requer um alvo aliado e um inimigo.";
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
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada para usar esta magia.").queue(null, Helper::doNothing);
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
									channel.sendMessage("❌ | Índice inválido, escolha uma carta inimiga para usar esta magia.").queue(null, Helper::doNothing);
									return;
								}
								int pos = Integer.parseInt(args[2]) - 1;
								List<SlotColumn> eSlots = arena.getSlots().get(getNextSide());
								Champion target = eSlots.get(pos).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos);
							}
							case BOTH -> {
								if (!StringUtils.isNumeric(args[2]) || !StringUtils.isNumeric(args[3])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada e uma inimiga para usar esta magia.").queue(null, Helper::doNothing);
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
								List<SlotColumn> eSlots = arena.getSlots().get(getNextSide());
								target = eSlots.get(pos2).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na segunda casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos2);
							}
						}

						SlotColumn free = getFirstAvailableSlot(getCurrentSide(), false);
						if (free == null && Helper.equalsAny(e.getCharm(), Charm.ENCHANTMENT, Charm.TRAP)) {
							channel.sendMessage("❌ | Não há espaço suficiente no campo para invocar essa magia.").queue(null, Helper::doNothing);
							return;
						}

						reroll = false;
						d.setAvailable(false);
						h.removeMana(e.getMana());
						h.removeHp(e.getBlood());
						e.activate(h, hands.get(getNextSide()), this, allyPos == null ? -1 : allyPos.getRight(), enemyPos == null ? -1 : enemyPos.getRight());

						if (e.canGoToGrave() && !Helper.equalsAny(e.getCharm(), Charm.ENCHANTMENT, Charm.TRAP)) {
							if (e.getTier() >= 4)
								arena.getBanned().add(e);
							else
								arena.getGraveyard().get(getCurrentSide()).add(e);
						}

						if (makeFusion(h)) return;

						String result = switch (e.getArgType()) {
							case NONE -> "%s usou %s.".formatted(
									h.getUser().getName(),
									e.isFlipped() ? "uma magia virada para baixo" : "a magia " + e.getCard().getName()
							);
							case ALLY -> {
								assert allyPos != null;

								yield "%s usou %s em %s.".formatted(
										h.getUser().getName(),
										e.isFlipped() ? "uma magia virada para baixo" : "a magia " + e.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName()
								);
							}
							case ENEMY -> {
								assert enemyPos != null;

								yield "%s usou %s em %s.".formatted(
										h.getUser().getName(),
										e.isFlipped() ? "uma magia virada para baixo" : "a magia " + e.getCard().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
							case BOTH -> {
								assert allyPos != null && enemyPos != null;

								yield "%s usou %s em %s e %s.".formatted(
										h.getUser().getName(),
										e.isFlipped() ? "uma magia virada para baixo" : "a magia " + e.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
						};

						if (!postCombat()) reportEvent(h, result, true, false);
						return;
					}

					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser o número da casa da carta à equipar este equipamento.").queue(null, Helper::doNothing);
						return;
					} else if (!h.isNullMode() && (h.getMana() < e.getMana())) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
						return;
					} else if (h.getHp() <= e.getBlood()) {
						channel.sendMessage("❌ | Você não tem HP suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}

					int dest = Integer.parseInt(args[1]) - 1;
					SlotColumn slot = slots.get(dest);

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue(null, Helper::doNothing);
						return;
					}
					int toEquip = Integer.parseInt(args[2]) - 1;

					SlotColumn target = slots.get(toEquip);

					if (target.getTop() == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					h.removeMana(e.getMana());
					h.removeHp(e.getBlood());
					slot.setBottom(e);
					Champion t = target.getTop();
					if (t.isFlipped()) {
						t.setFlipped(false);
						t.setDefending(true);
					}
					t.link(e);
					e.link(toEquip, t);
					if (applyEffect(ON_EQUIP, t, getCurrentSide(), toEquip, new Source(t, getCurrentSide(), toEquip)))
						return;

					if (e.getCharm() != null) {
						switch (e.getCharm()) {
							case TIMEWARP -> {
								if (t.hasEffect()) {
									t.getEffect(new EffectParameters(BEFORE_TURN, this, getCurrentSide(), toEquip, Duelists.of(t, toEquip, null, -1), channel));
									t.getEffect(new EffectParameters(AFTER_TURN, this, getCurrentSide(), toEquip, Duelists.of(t, toEquip, null, -1), channel));
								}
							}
							case DOUBLETAP -> {
								if (t.hasEffect())
									t.getEffect(new EffectParameters(ON_SUMMON, this, getCurrentSide(), toEquip, Duelists.of(t, toEquip, null, -1), channel));
							}
							case CLONE -> {
								SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);

								if (sc != null) {
									t.removeAtk(Math.round(t.getAltAtk() * 0.25f));
									t.removeDef(Math.round(t.getAltDef() * 0.25f));

									Champion dp = t.copy();
									dp.getBonus().removeMana(dp.getMana() / 2);
									dp.setGravelocked(true);

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
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
						return;
					} else if ((h.isNullMode() && h.getHp() <= c.getBaseStats() / 2) || h.getHp() <= c.getBlood()) {
						channel.sendMessage("❌ | Você não tem HP suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou escolha outra carta.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}
					int dest = Integer.parseInt(args[1]) - 1;

					SlotColumn slot = slots.get(dest);

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (isSlotDisabled(getCurrentSide(), dest)) {
						channel.sendMessage("❌ | Essa casa está indisponível.").queue(null, Helper::doNothing);
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
					if (!c.isFlipped() && applyEffect(ON_SUMMON, c, getCurrentSide(), dest, new Source(c, getCurrentSide(), dest)))
						return;

					summoned.get(getCurrentSide()).merge(c.getRace(), 1, Integer::sum);

					msg = h.getUser().getName() + " invocou " + (c.isFlipped() ? "uma carta virada para baixo" : c.getName() + " em posição de " + (c.isDefending() ? "defesa" : "ataque")) + ".";

					if (c.getMana() > 0) {
						if (h.isNullMode())
							h.removeHp(c.getBaseStats() / 2);
						else
							h.removeMana(c.getMana());
					}
					h.removeHp(c.getBlood());
				} else {
					Field f = (Field) d.copy();
					if (!args[1].equalsIgnoreCase("f")) {
						channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					arena.setField(f);
					msg = h.getUser().getName() + " invocou o campo " + f.getCard().getName() + ".";
				}

				if (makeFusion(h)) return;
				reportEvent(h, msg, true, false);
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

				List<SlotColumn> yourSide = arena.getSlots().get(getCurrentSide());
				List<SlotColumn> hisSide = arena.getSlots().get(getNextSide());

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = yourSide.get(is[0]).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (hisSide.parallelStream().anyMatch(s -> s.getTop() != null)) {
						channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
						return;
					}

					String stt = switch (c.getStatus()) {
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

					Hand you = hands.get(getCurrentSide());
					Hand op = hands.get(getNextSide());

					float demonFac = 1 - op.getMitigation();

					if (h.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.25f;
					if (op.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.33f;

					int yPower = Math.round(c.getFinAtk() * (getRound() < 2 ? 0.5f : 1));

					if (!c.isDecoy()) {
						op.removeHp(Math.round(yPower * demonFac));
					}

					if (op.getMana() > 0) {
						int toSteal = (int) Math.min(
								op.getMana(),
								c.getLinkedTo().parallelStream()
										.filter(e -> e.getCharm() == Charm.DRAIN)
										.count()
						);

						you.addMana(toSteal);
						op.removeMana(toSteal);
					}

					int bleed = Math.round(
							c.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.BLEEDING)
									.mapToInt(Equipment::getAtk)
									.sum() * demonFac
					);

					if (bleed > 0) op.addBleeding(bleed);

					c.setAvailable(false);

					if (!postCombat()) {
						int extra = Math.round(yPower * demonFac - yPower);
						reportEvent(h,
								"%s atacou %s causando %s de dano direto!%s%s".formatted(
										c.getName(),
										hands.get(getNextSide()).getUser().getName(),
										yPower,
										getRound() < 2 ? " (dano reduzido por ser o 1º turno)" : "",
										extra > 0
												? " (efeito de raça: dano direto aumentado em " + extra + ")"
												: extra < 0
												? " (efeito de raça: dano direto reduzido em " + extra + ")"
												: ""
								)
								, true, false);
					}
					return;
				}

				Champion yours = yourSide.get(is[0]).getTop();
				Champion his = hisSide.get(is[1]).getTop();

				if (yours == null || his == null) {
					channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
					return;
				}

				String stt = switch (yours.getStatus()) {
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

				attack(new Source(yours, getCurrentSide(), is[0]), new Target(his, getNextSide(), is[1]));
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue(null, Helper::doNothing);

			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue(null, Helper::doNothing);
			}
		}
	}

	private void reportEvent(Hand h, String msg, boolean resetTimer, boolean changeTurn) {
		for (Side s : Side.values()) {
			List<SlotColumn> slts = arena.getSlots().get(s);
			Hand hd = getHands().get(s);

			int heroIndex = isHeroInField(s);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn slot = slts.get(i);
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				if (applyEffect(GAME_TICK, c, s, i, new Source(c, s, i))) return;

				if (c.getCard().getId().equals(hd.getUser().getId()) && hd.getHero() != null && heroIndex == -1) {
					c.setHero(hd.getHero());
				} else if (i != heroIndex) {
					c.setHero(null);
				}
			}
		}

		BufferedImage bi = arena.render(this, hands);
		if (resetTimer) {
			resetTimerKeepTurn();
			applyEffect(GLOBAL_TICK, null, getCurrentSide(), -1);
		}
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

		if (atkr.isDefending()) return;
		atkr.setAvailable(false);

		/* PRE-ATTACK */
		{
			if (applyEffect(ATTACK_ASSIST, atkr.getAdjacent(Neighbor.LEFT), source.side(), source.index() - 1, source, target))
				return;
			if (applyEffect(ATTACK_ASSIST, atkr.getAdjacent(Neighbor.RIGHT), source.side(), source.index() + 1, source, target))
				return;
			if (applyEffect(ON_ATTACK, atkr, source.side(), target.index(), source, target)) return;

			if (atkr.getBonus().popFlag(Flag.SKIPCOMBAT)) {
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

			if (defr.getBonus().popFlag(Flag.SKIPCOMBAT)) {
				atkr.resetAttribs();
				defr.resetAttribs();

				if (applyEffect(POST_DEFENSE, defr, target.side(), target.index(), source, target)) return;

				reportEvent(null, "Cálculo de combate ignorado por efeito do defensor!", true, false);
				return;
			}
		}

		int yPower = Math.round((atkr.isDecoy() ? 0 : atkr.getFinAtk()) * (defr.isSleeping() ? 1.25f : 1));

		int hPower;
		if (defr.isDefending()) {
			if (defr.isFlipped()) {
				defr.setFlipped(false);
				if (applyEffect(ON_FLIP, defr, target.side(), target.index(), source, target)) return;
			}

			hPower = defr.getFinDef();
		} else {
			hPower = defr.isDecoy() ? 0 : defr.getFinAtk();
		}

		int dodge = defr.getDodge();
		boolean dodged = dodge >= 100 || (dodge > 0 && Helper.chance(dodge));

		atkr.resetAttribs();
		defr.resetAttribs();

		Hand you = hands.get(source.side());
		Hand op = hands.get(target.side());

		/* ATTACK SUCCESS */
		if (yPower > hPower) {
			if (dodged) {
				if (applyEffect(ON_MISS, atkr, source.side(), source.index(), source, target)) return;
				if (applyEffect(ON_DODGE, defr, target.side(), target.index(), source, target)) return;

				reportEvent(null, defr.getName() + " esquivou do ataque de " + atkr.getName() + "! (" + Helper.roundToString(dodge, 1) + "%)", true, false);
			} else {
				if (applyEffect(POST_ATTACK, atkr, source.side(), target.index(), source, target)) return;
				if (applyEffect(BEFORE_DEATH, defr, target.side(), target.index(), source, target)) return;

				float demonFac = 1 - op.getMitigation();
				if (you.getCombo().getRight() == Race.DEMON)
					demonFac *= 1.25f;
				if (op.getCombo().getRight() == Race.DEMON)
					demonFac *= 1.33f;

				if (atkr.isDecoy()) {
					reportEvent(null, atkr.getName() + " derrotou " + defr.getCard().getName() + "? (" + yPower + " > " + hPower + ")", true, false);
				} else if (defr.isDecoy()) {
					killCard(target.side(), target.index(), defr.getId());
					reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
				}

				boolean isHero = defr.getHero() != null;
				boolean noDmg = (defr.isDefending() && !(defr.isSleeping() || defr.isStunned()))
								|| defr.getBonus().popFlag(Flag.NODAMAGE)
								|| (getCustom() != null && getCustom().getBoolean("semdano"));

				int dmg;
				if (isHero || !noDmg) {
					dmg = Math.round((atkr.getBonus().hasFlag(Flag.ALLDAMAGE) ? yPower : yPower - hPower) * demonFac);
				} else {
					dmg = Math.round(
							atkr.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.PIERCING)
									.mapToInt(Equipment::getAtk)
									.sum() * demonFac
					);
				}

				if (op.getMana() > 0) {
					int toSteal = (int) Math.min(
							op.getMana(),
							atkr.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.DRAIN)
									.count()
					);

					you.addMana(toSteal);
					op.removeMana(toSteal);
				}

				Hero h = defr.getHero();
				if (h != null) {
					int aux = dmg - h.getHp();
					h.setHp(h.getHp() - dmg);
					dmg = aux;
				}

				if (h == null || h.getHp() == 0) {
					int bleed = Math.round(
							atkr.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.BLEEDING)
									.mapToInt(Equipment::getAtk)
									.sum() * demonFac
					);

					if (bleed > 0) op.addBleeding(bleed);
					op.removeHp(dmg);
					killCard(target.side(), target.index(), defr.getId());

					Hero y = atkr.getHero();
					if (y != null) {
						y.addXp(1);
						if (y.getPerks().contains(Perk.VAMPIRE)) {
							y.setHp(y.getHp() + Math.round((y.getMaxHp() - y.getHp()) * 0.1f));
						}
					}
				}

				if (applyEffect(AFTER_DEATH, defr, target.side(), target.index(), source, target)) return;

				if (!postCombat()) {
					int extra = Math.round(dmg * demonFac - dmg);
					String msg = "%s derrotou %s! (%d > %d)%s%s".formatted(
							atkr.getName(),
							defr.getCard().getName(),
							yPower,
							hPower,
							extra > 0
									? " (efeito de raça: dano direto aumentado em " + extra + ")"
									: extra < 0
									? " (efeito de raça: dano direto reduzido em " + extra + ")"
									: "",
							defr.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
					);

					reportEvent(null, msg, true, false);
				} else return;
			}
		}

		/* ATTACK FAILED */
		else if (yPower < hPower) {
			if (applyEffect(ON_SUICIDE, atkr, source.side(), target.index(), source, target)) return;
			if (applyEffect(POST_DEFENSE, defr, target.side(), target.index(), source, target)) return;

			float demonFac = 1 - you.getMitigation();
			if (op.getCombo().getRight() == Race.DEMON)
				demonFac *= 1.25f;
			if (you.getCombo().getRight() == Race.DEMON)
				demonFac *= 1.33f;

			if (atkr.isDecoy()) {
				killCard(source.side(), source.index(), atkr.getId());
				reportEvent(null, atkr.getName() + " não conseguiu derrotar " + defr.getCard().getName() + "? (" + yPower + " < " + hPower + ")", true, false);
			} else if (defr.isDecoy()) {
				reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
			}

			boolean isHero = atkr.getHero() != null;
			boolean noDmg = atkr.getBonus().popFlag(Flag.NODAMAGE)
							|| (getCustom() != null && getCustom().getBoolean("semdano"));

			int dmg;
			if (isHero || !noDmg) {
				dmg = Math.round((defr.getBonus().hasFlag(Flag.ALLDAMAGE) ? hPower : hPower - yPower) * demonFac);
			} else {
				dmg = Math.round(
						defr.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.PIERCING)
								.mapToInt(Equipment::getAtk)
								.sum() * demonFac
				);
			}

			if (you.getMana() > 0) {
				int toSteal = (int) Math.min(
						you.getMana(),
						defr.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);

				op.addMana(toSteal);
				you.removeMana(toSteal);
			}

			Hero h = atkr.getHero();
			if (isHero) {
				int aux = dmg - h.getHp();
				h.setHp(h.getHp() - dmg);
				dmg = aux;
			}

			if (h == null || h.getHp() == 0) {
				int bleed = Math.round(
						defr.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.BLEEDING)
								.mapToInt(Equipment::getAtk)
								.sum() * demonFac
				);

				if (bleed > 0) you.addBleeding(bleed);
				you.removeHp(dmg);
				killCard(source.side(), source.index(), atkr.getId());

				Hero y = defr.getHero();
				if (y != null) {
					y.addXp(1);
					if (y.getPerks().contains(Perk.VAMPIRE)) {
						y.setHp(y.getHp() + Math.round((y.getMaxHp() - y.getHp()) * 0.1f));
					}
				}
			}

			if (!postCombat()) {
				int extra = Math.round(dmg * demonFac - dmg);
				String msg = "%s não conseguiu derrotar %s! (%d < %d)%s%s".formatted(
						atkr.getName(),
						defr.getCard().getName(),
						yPower,
						hPower,
						extra > 0
								? " (efeito de raça: dano direto aumentado em " + extra + ")"
								: extra < 0
								? " (efeito de raça: dano direto reduzido em " + extra + ")"
								: "",
						defr.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
				);

				reportEvent(null, msg, true, false);
			} else return;
		}

		/* ATTACK CLASHED */
		else {
			if (applyEffect(ON_SUICIDE, atkr, source.side(), target.index(), source, target)) return;
			if (applyEffect(BEFORE_DEATH, defr, target.side(), target.index(), source, target)) return;

			if (atkr.isDecoy() && defr.isDecoy()) {
				killCard(source.side(), source.index(), atkr.getId());
				killCard(target.side(), target.index(), defr.getId());
				reportEvent(null, "As duas cartas eram iscas!", true, false);
			} else if (atkr.isDecoy()) {
				killCard(source.side(), source.index(), atkr.getId());
				reportEvent(null, "Ambas as cartas foram destruídas? (" + yPower + " = " + hPower + ")", true, false);
			} else if (defr.isDecoy()) {
				killCard(target.side(), target.index(), defr.getId());
				reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
			}

			if (op.getMana() > 0 || you.getMana() > 0) {
				int yToSteal = (int) Math.min(
						op.getMana(),
						atkr.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);
				int hToSteal = (int) Math.min(
						you.getMana(),
						defr.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);


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

			boolean isHero = atkr.getHero() != null;

			Hero h = atkr.getHero();
			if (isHero) {
				h.setHp(h.getHp() - hPower);
			}

			if (h == null || h.getHp() == 0) {
				float hDmg = defr.getLinkedTo().parallelStream()
						.filter(e -> e.getCharm() == Charm.PIERCING)
						.mapToInt(Equipment::getAtk)
						.sum();

				int bleed = defr.getLinkedTo().parallelStream()
						.filter(e -> e.getCharm() == Charm.BLEEDING)
						.mapToInt(Equipment::getAtk)
						.sum();

				if (bleed > 0) you.addBleeding(bleed);
				you.removeHp(Math.round(hDmg));

				killCard(source.side(), source.index(), atkr.getId());

				Hero y = defr.getHero();
				if (y != null) {
					y.addXp(1);
					if (y.getPerks().contains(Perk.VAMPIRE)) {
						y.setHp(y.getHp() + Math.round((y.getMaxHp() - y.getHp()) * 0.1f));
					}
				}
			}

			isHero = defr.getHero() != null;

			h = defr.getHero();
			if (isHero) {
				h.setHp(h.getHp() - hPower);
			} else {
				float yDmg = atkr.getLinkedTo().parallelStream()
						.filter(e -> e.getCharm() == Charm.PIERCING)
						.mapToInt(Equipment::getAtk)
						.sum();

				int bleed = atkr.getLinkedTo().parallelStream()
						.filter(e -> e.getCharm() == Charm.BLEEDING)
						.mapToInt(Equipment::getAtk)
						.sum();

				if (bleed > 0) op.addBleeding(bleed);
				op.removeHp(Math.round(yDmg));
			}

			if (h == null || h.getHp() == 0) {
				killCard(target.side(), target.index(), defr.getId());

				Hero y = atkr.getHero();
				if (y != null) {
					y.addXp(1);
					if (y.getPerks().contains(Perk.VAMPIRE)) {
						y.setHp(y.getHp() + Math.round((y.getMaxHp() - y.getHp()) * 0.1f));
					}
				}
			}

			if (applyEffect(AFTER_DEATH, atkr, source.side(), source.index(), source, target)) return;
			if (applyEffect(AFTER_DEATH, defr, target.side(), target.index(), source, target)) return;

			if (!postCombat()) {
				String msg = "Ambas as cartas foram destruídas! (%d = %d)%s".formatted(
						yPower,
						hPower,
						defr.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
				);

				reportEvent(null, msg, true, false);
			} else return;
		}

		if (applyEffect(POST_ATTACK_ASSIST, atkr.getAdjacent(Neighbor.LEFT), source.side(), source.index() - 1, source, target))
			return;
		if (applyEffect(POST_ATTACK_ASSIST, atkr.getAdjacent(Neighbor.RIGHT), source.side(), source.index() + 1, source, target))
			return;
		if (applyEffect(POST_DEFENSE_ASSIST, defr.getAdjacent(Neighbor.LEFT), target.side(), target.index() - 1, source, target))
			return;
		if (applyEffect(POST_DEFENSE_ASSIST, defr.getAdjacent(Neighbor.RIGHT), target.side(), target.index() + 1, source, target))
			return;

		postCombat();
	}

	private boolean makeFusion(Hand h) {
		if (fusionLock > 0) return false;
		List<SlotColumn> slts = arena.getSlots().get(getCurrentSide());

		List<String> champsInField = slts.parallelStream()
				.map(SlotColumn::getTop)
				.map(c -> c == null || c.isSealed() ? null : c.getCard().getId())
				.collect(Collectors.toList());

		List<String> equipsInField = slts.parallelStream()
				.map(SlotColumn::getBottom)
				.map(dr -> dr == null ? null : dr.getCard().getId())
				.collect(Collectors.toList());

		String field = getArena().getField() != null ? getArena().getField().getCard().getId() : "DEFAULT";

		Champion fusion = fusions
				.parallelStream()
				.filter(f ->
						f.getRequiredCards().size() > 0 &&
						!f.canFuse(champsInField, equipsInField, field).isEmpty() &&
						((h.isNullMode() && h.getHp() > f.getBaseStats() / 2) || h.getMana() >= f.getMana()) &&
						h.getHp() > f.getBlood()
				)
				.findFirst()
				.map(Champion::copy)
				.orElse(null);

		if (fusion != null) {
			for (FusionMaterial material : fusion.canFuse(champsInField, equipsInField, field)) {
				banCard(h.getSide(), material.index(), material.equipment());
			}

			SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);
			if (sc != null) {
				sc.setTop(fusion);
				if (applyEffect(ON_SUMMON, fusion, getCurrentSide(), sc.getIndex(), new Source(fusion, getCurrentSide(), sc.getIndex())))
					return true;

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
			if (target == null || target.getId() != id || target.getBonus().popFlag(Flag.NODEATH)) return;
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
			if (e != null && e.getLinkedTo().getLeft() == index) {
				unequipCard(side, slt.getIndex());
			}
		}

		if (applyEffect(ON_DESTROY, target, side, index)) return;

		getSlot(side, index).setTop(null);
		if (target.canGoToGrave())
			arena.getGraveyard().get(side).add(target);
	}

	public void destroyCard(Side side, int index, Side caster, int source) {
		Champion target = getSlot(side, index).getTop();
		if (target == null || target.getBonus().popFlag(Flag.NODEATH)) return;

		double chance = 100;
		Champion activator = null;
		if (caster != null && source > -1) {
			activator = getSlot(caster, source).getTop();
			if (activator != null) {
				int sourceMana = activator.getMana() + (activator.isFusion() ? 5 : 0);
				int targetMana = target.getMana() + (target.isFusion() ? 5 : 0);

				chance -= target.getDodge() * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getSpecialData().getEnum(Charm.class, "charm");
			if (Helper.equalsAny(charm, Charm.SHIELD, Charm.MIRROR) || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			for (Equipment e : target.getLinkedTo()) {
				if (Helper.equalsAny(e.getCharm(), Charm.SHIELD, Charm.MIRROR)) {
					unequipCard(side, e.getIndex());

					if (e.getCharm() == Charm.MIRROR && activator != null) {
						destroyCard(caster, source, side, index);
					}
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
				if (e != null && e.getLinkedTo().getLeft() == index) {
					unequipCard(side, slt.getIndex());
				}
			}

			if (applyEffect(ON_DESTROY, target, side, index)) return;

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
		if (target == null || target.getBonus().popFlag(Flag.NOCONVERT)) return;

		double chance = 100;
		Champion activator = null;
		if (source > -1) {
			activator = getSlot(caster, source).getTop();
			if (activator != null) {
				int sourceMana = activator.getMana() + (activator.isFusion() ? 5 : 0);
				int targetMana = target.getMana() + (target.isFusion() ? 5 : 0);

				chance -= target.getDodge() * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getSpecialData().getEnum(Charm.class, "charm");
			if (Helper.equalsAny(charm, Charm.SHIELD, Charm.MIRROR) || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			for (Equipment e : target.getLinkedTo()) {
				if (Helper.equalsAny(e.getCharm(), Charm.SHIELD, Charm.MIRROR)) {
					unequipCard(side, e.getIndex());

					if (e.getCharm() == Charm.MIRROR && activator != null) {
						convertCard(caster, source, side, index);
					}
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
				if (e != null && e.getLinkedTo().getLeft() == index) {
					unequipCard(side, slt.getIndex());
				}
			}

			if (applyEffect(ON_DESTROY, target, side, index)) return;

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
		if (target == null || target.getBonus().popFlag(Flag.NOCONVERT)) return;

		double chance = 100;
		Champion activator = getSlot(caster, source).getTop();
		if (activator == null || activator.getBonus().popFlag(Flag.NOCONVERT)) return;

		int sourceMana = activator.getMana() + (activator.isFusion() ? 5 : 0);
		int targetMana = target.getMana() + (target.isFusion() ? 5 : 0);

		chance -= target.getDodge() * 0.75;
		if (sourceMana < targetMana)
			chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getSpecialData().getEnum(Charm.class, "charm");
			if (Helper.equalsAny(charm, Charm.SHIELD, Charm.MIRROR) || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			for (Equipment e : target.getLinkedTo()) {
				if (Helper.equalsAny(e.getCharm(), Charm.SHIELD, Charm.MIRROR)) {
					unequipCard(side, e.getIndex());
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
				if (e != null && e.getLinkedTo().getLeft() == index) {
					unequipCard(side, slt.getIndex());
				}
			}

			charm = activator.getBonus().getSpecialData().getEnum(Charm.class, "charm");
			if (Helper.equalsAny(charm, Charm.SHIELD, Charm.MIRROR) || (activator.getHero() != null && activator.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			for (Equipment e : activator.getLinkedTo()) {
				if (Helper.equalsAny(e.getCharm(), Charm.SHIELD, Charm.MIRROR)) {
					unequipCard(caster, e.getIndex());
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
				if (e != null && e.getLinkedTo().getLeft() == source) {
					unequipCard(caster, slt.getIndex());
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
				int sourceMana = activator.getMana() + (activator.isFusion() ? 5 : 0);
				int targetMana = target.getMana() + (target.isFusion() ? 5 : 0);

				chance -= target.getDodge() * 0.75;
				if (sourceMana < targetMana)
					chance -= 25 - Helper.clamp(sourceMana * 25 / targetMana, 0, 25);
			}
		}

		if (chance >= 100 || (chance > 0 && Helper.chance(chance))) {
			Charm charm = target.getBonus().getSpecialData().getEnum(Charm.class, "charm");
			if (Helper.equalsAny(charm, Charm.SHIELD, Charm.MIRROR) || (target.getHero() != null && target.getHero().getPerks().contains(Perk.MINDSHIELD))) {
				return;
			}

			for (Equipment e : target.getLinkedTo()) {
				if (Helper.equalsAny(e.getCharm(), Charm.SHIELD, Charm.MIRROR)) {
					unequipCard(side, e.getIndex());

					if (e.getCharm() == Charm.MIRROR && activator != null) {
						captureCard(caster, source, side, index, withFusion);
					}
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
				if (e != null && e.getLinkedTo().getLeft() == index) {
					unequipCard(side, slt.getIndex());
				}
			}

			if (applyEffect(ON_DESTROY, target, side, index)) return;

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

			Champion link = target.getLinkedTo().getRight();
			if (link != null)
				link.unlink(target);

			slts.get(index).setBottom(null);
			arena.getBanned().add(target);
		} else {
			Champion target = slts.get(index).getTop();
			if (target == null) return;

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
				if (e != null && e.getLinkedTo().getLeft() == index) {
					banCard(side, slt.getIndex(), true);
				}
			}

			if (applyEffect(ON_DESTROY, target, side, index)) return;

			getSlot(side, index).setTop(null);
			if (target.canGoToGrave())
				arena.getBanned().add(target);
		}
	}

	public void unequipCard(Side side, int index) {
		Equipment target = getSlot(side, index).getBottom();
		if (target == null) return;

		Champion link = target.getLinkedTo().getRight();
		if (link != null)
			link.unlink(target);

		applyEffect(ON_DESTROY, target, side, index, Duelists.of());

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

	public Side getSideById(String id) {
		return hands.values().parallelStream()
				.filter(h -> h.getUser().getId().equals(id) || (h instanceof TeamHand th && th.getUsers().parallelStream().anyMatch(u -> u.getId().equals(id))))
				.map(Hand::getSide)
				.findFirst().orElse(null);
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, Pair<Race, Race>> getCombos() {
		return combos;
	}

	public SlotColumn getFirstAvailableSlot(Side s, boolean top) {
		List<SlotColumn> get = arena.getSlots().get(s);
		for (int i = 0; i < get.size(); i++) {
			SlotColumn slot = get.get(i);
			if (top ? (slot.getTop() == null && !isSlotDisabled(s, i)) : slot.getBottom() == null)
				return slot;
		}
		return null;
	}

	public boolean lastTick() {
		for (Side s : Side.values()) {
			Hand h = hands.get(s);
			Hand op = hands.get(s.getOther());
			List<SlotColumn> slts = arena.getSlots().get(s);

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
				if (lastTick()) return false;

				if (getCustom() == null) {
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
							slts.getTop().setFlipped(false);

						if (slts.getBottom() != null)
							slts.getBottom().setFlipped(false);
					}
				}

				close();
				finished = true;
				channel.sendMessage(msg)
						.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
						.queue(ms ->
								this.message.compute(ms.getChannel().getId(), (id, m) -> {
									if (m != null) m.delete().queue(null, Helper::doNothing);
									return ms;
								})
						);
				break;
			}
		}

		return finished;
	}

	@Override
	public Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons() {
		ThrowingConsumer<ButtonWrapper> skip = wrapper -> {
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

			for (Drawable d : discardBatch) {
				d.setAvailable(true);
			}
			if (team && h.get().getCombo().getLeft() == Race.BESTIAL) {
				h.get().getDeque().addAll(
						discardBatch.stream()
								.filter(d -> {
									if (d instanceof Champion c) return c.canGoToGrave();
									else if (d instanceof Equipment e) return !e.isEffectOnly();
									else return true;
								})
								.collect(Collectors.toList())
				);
				Collections.shuffle(h.get().getDeque());
			} else {
				arena.getGraveyard().get(getCurrentSide()).addAll(
						discardBatch.stream()
								.filter(d -> {
									if (d instanceof Champion c) return c.canGoToGrave();
									else if (d instanceof Equipment e) return e.canGoToGrave();
									else return true;
								})
								.collect(Collectors.toList())
				);
			}
			discardBatch.clear();

			if (getRound() > 0) reroll = false;
			resetTimer(this);

			phase = Phase.PLAN;
			h.set(hands.get(getCurrentSide()));
			h.get().decreaseSuppression();
			h.get().decreaseLockTime();
			h.get().decreaseNullTime();
			if (synthCd[getCurrentSide() == Side.TOP ? 1 : 0] > 0) {
				synthCd[getCurrentSide() == Side.TOP ? 1 : 0]--;
			}
			slots = arena.getSlots().get(getCurrentSide());

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
					if (draw) {
						String msg = "Declaro empate! (" + getRound() + " turnos)";

						for (List<SlotColumn> sides : arena.getSlots().values()) {
							for (SlotColumn slts : sides) {
								if (slts.getTop() != null)
									slts.getTop().setFlipped(false);

								if (slts.getBottom() != null)
									slts.getBottom().setFlipped(false);
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
					} else draw = true;
				}
			}

			if (h.get().getBleeding() > 0) {
				h.get().removeHp(h.get().getBleeding() / 10);
				h.get().decreaseBleeding();
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

			switch (h.get().getCombo().getRight()) {
				case BESTIAL -> {
					if (getRound() <= 1)
						h.get().addMana(1);
				}
				case ELF -> {
					if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
						h.get().addMana(1);
				}
			}

			if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
			for (int i = 0; i < slots.size(); i++) {
				SlotColumn sc = slots.get(i);

				Champion c = sc.getTop();
				if (c != null) {
					if (c.isStasis()) c.reduceStasis();
					else if (c.isStunned()) c.reduceStun();
					else if (c.isSleeping()) c.reduceSleep();

					if (applyEffect(BEFORE_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
						|| makeFusion(h.get())
					) return;
				}

				Equipment e = sc.getBottom();
				if (e != null) {
					Champion link = getSlot(getCurrentSide(), e.getLinkedTo().getLeft()).getTop();
					if (link == null || !link.equals(e.getLinkedTo().getRight())) {
						unequipCard(getCurrentSide(), e.getIndex());
					} else if (link.getLinkedTo().stream().noneMatch(eq -> eq.equals(e) && eq.getIndex() == e.getIndex())) {
						link.link(e);
					}
				}

				if (sc.isUnavailable()) {
					sc.setUnavailable(-1);
				}
			}

			String msg = u.getName() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention() + " (turno " + getRound() + ")";

			reportEvent(h.get(), msg, false, true);
			oldState = new GameState(this);
		};

		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		if (getRound() < 1 || phase == Phase.ATTACK)
			buttons.put(Helper.parseEmoji("▶️"), skip);
		else {
			buttons.put(Helper.parseEmoji("▶️"), wrapper -> {
				phase = Phase.ATTACK;
				draw = false;
				reroll = false;
				reportEvent(null, "**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate", true, false);
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
					if (getCustom() == null) {
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
								slts.getTop().setFlipped(false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false);
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

				int toDraw = Math.min(remaining, h.getDeque().size());
				if (toDraw == 0) {
					if (getCustom() == null) {
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
								slts.getTop().setFlipped(false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false);
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

					List<Drawable> grv = arena.getGraveyard().get(getCurrentSide());
					if (grv.size() < 3) {
						channel.sendMessage("❌ | Você não possui almas suficiente para sintetizar.").queue(null, Helper::doNothing);
						return;
					}
					grv = grv.subList(0, 3);

					Hand h = hands.get(getCurrentSide());

					int score = grv.stream()
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

					arena.getBanned().addAll(grv);
					grv.clear();
					synthCd[getCurrentSide() == Side.TOP ? 1 : 0] = 5;
					reportEvent(h, getCurrent().getName() + " sacrificou 3 almas para sintetizar um evogear.", true, false);
				});
			}
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
		if (hands.get(getCurrentSide()).getHp() < hands.get(getCurrentSide()).getBaseHp() / 3 && hands.get(getCurrentSide()).getDestinyDeck().size() > 0 && phase == Phase.PLAN)
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
								slts.getTop().setFlipped(false);

							if (slts.getBottom() != null)
								slts.getBottom().setFlipped(false);
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

					for (Drawable d : discardBatch) {
						d.setAvailable(true);
					}
					if (team && h.get().getCombo().getLeft() == Race.BESTIAL) {
						h.get().getDeque().addAll(
								discardBatch.stream()
										.map(Drawable::copy)
										.collect(Collectors.toList())
						);
						Collections.shuffle(h.get().getDeque());
					} else {
						arena.getGraveyard().get(getCurrentSide()).addAll(
								discardBatch.stream()
										.map(Drawable::copy)
										.collect(Collectors.toList())
						);
					}
					discardBatch.clear();

					if (getRound() > 0) reroll = false;
					resetTimer(this);

					phase = Phase.PLAN;
					h.set(hands.get(getCurrentSide()));
					h.get().decreaseSuppression();
					h.get().decreaseLockTime();
					h.get().decreaseNullTime();
					if (synthCd[getCurrentSide() == Side.TOP ? 1 : 0] > 0) {
						synthCd[getCurrentSide() == Side.TOP ? 1 : 0]--;
					}
					slots = arena.getSlots().get(getCurrentSide());

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
							if (draw) {
								String msg = "Declaro empate! (" + getRound() + " turnos)";

								for (List<SlotColumn> sides : arena.getSlots().values()) {
									for (SlotColumn slts : sides) {
										if (slts.getTop() != null)
											slts.getTop().setFlipped(false);

										if (slts.getBottom() != null)
											slts.getBottom().setFlipped(false);
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
							} else draw = true;
						}
					}

					if (h.get().getBleeding() > 0) {
						h.get().removeHp(h.get().getBleeding() / 10);
						h.get().decreaseBleeding();
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

					switch (h.get().getCombo().getRight()) {
						case BESTIAL -> {
							if (getRound() <= 1)
								h.get().addMana(1);
						}
						case ELF -> {
							if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
								h.get().addMana(1);
						}
					}

					if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						SlotColumn sc = slots.get(i);

						Champion c = sc.getTop();
						if (c != null) {
							if (c.isStasis()) c.reduceStasis();
							else if (c.isStunned()) c.reduceStun();
							else if (c.isSleeping()) c.reduceSleep();

							if (applyEffect(BEFORE_TURN, c, getCurrentSide(), i, new Source(c, getCurrentSide(), i))
								|| makeFusion(h.get())
							) return;
						}

						Equipment e = sc.getBottom();
						if (e != null) {
							Champion link = getSlot(getCurrentSide(), e.getLinkedTo().getLeft()).getTop();
							if (link == null || !link.equals(e.getLinkedTo().getRight())) {
								unequipCard(getCurrentSide(), e.getIndex());
							} else if (link.getLinkedTo().stream().noneMatch(eq -> eq.equals(e) && eq.getIndex() == e.getIndex())) {
								link.link(e);
							}
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
		if (getCustom() != null || getRound() > 8)
			buttons.put(Helper.parseEmoji("\uD83C\uDFF3️"), wrapper -> {
				if (getCustom() == null) {
					getHistory().setWinner(getNextSide());
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				}

				String msg = getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)";

				for (List<SlotColumn> sides : arena.getSlots().values()) {
					for (SlotColumn slts : sides) {
						if (slts.getTop() != null)
							slts.getTop().setFlipped(false);

						if (slts.getBottom() != null)
							slts.getBottom().setFlipped(false);
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
			});

		return buttons;
	}

	private void recordLast() {
		Hand top = hands.get(Side.TOP);
		Hand bot = hands.get(Side.BOTTOM);
		getHistory().getRound(getRound() + 1).setSide(getCurrentSide());
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

	public List<Drawable> getDiscardBatch() {
		return discardBatch;
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

	@SuppressWarnings("SuspiciousMethodCalls")
	public void removePersistentEffect(Drawable card) {
		persistentEffects.remove(card);
	}

	public boolean applyPersistentEffects(EffectTrigger trigger, Side to, int index) {
		if (persistentEffects.size() > 0) {
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

			return postCombat();
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
		boolean lastTick = trigger == ON_WIN || trigger == ON_LOSE;

		if (trigger.isIndividual()) {
			applyPersistentEffects(trigger, side, index);

			if (!lastTick && postCombat()) return true;
		}

		if (activator == null) return false;
		if (effectLock == 0) {
			if (activator.hasEffect()) {
				boolean activate = true;

				if (duelists.getAttacker() != null) {
					Champion c = duelists.getAttacker();

					if (c.isDuelling() && !c.getNemesis().equals(duelists.getDefender())) activate = false;
				}

				if (duelists.getDefender() != null) {
					Champion c = duelists.getDefender();

					if (c.getBonus().popFlag(Flag.NOEFFECT)) activate = false;
					else if (c.isDuelling() && !c.getNemesis().equals(duelists.getAttacker())) activate = false;
				}

				if (activate)
					activator.getEffect(new EffectParameters(trigger, this, side, index, duelists, channel));
			}

			for (Equipment e : List.copyOf(activator.getLinkedTo())) {
				if (e.hasEffect()) applyEffect(trigger, e, side, index, duelists);
			}

			return !lastTick && postCombat();
		}

		return false;
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, Side side, int index, Duelists duelists) {
		if (effectLock == 0) {
			if (activator.hasEffect()) {
				activator.getEffect(new EffectParameters(trigger, this, side, index, duelists, channel));
			}
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
							.setImageUrl(ShiroInfo.RESOURCES_URL + "/shoukan/gifs/" + gif + ".gif")
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

		nc.setDefending(from.isDefending());
		nc.setFlipped(from.isFlipped());

		int index;
		if (Helper.equalsAny(ep.getTrigger(), ATTACK_ASSIST, POST_ATTACK_ASSIST, DEFENSE_ASSIST, POST_DEFENSE_ASSIST)) {
			index = ep.getIndex();
		} else {
			index = ep.getTrigger().isDefensive()
					? ep.getDuelists().getDefenderPos()
					: ep.getDuelists().getAttackerPos();
		}

		banCard(ep.getSide(), index, false);
		arena.getSlots().get(ep.getSide()).get(index).setTop(nc);
		applyEffect(ON_SUMMON, nc, ep.getSide(), index, ep.getDuelists());
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

	public Phase getPhase() {
		return phase;
	}

	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	public SlotColumn getSlot(Side s, int index) {
		return arena.getSlots().get(s).get(index);
	}

	@Override
	public void resetTimer(Shoukan shkn) {
		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> a.isInvalid(this, e.getKey(), false));
		}

		getCurrRound().setSide(getCurrentSide());
		decreaseFLockTime();
		decreaseSLockTime();
		decreaseELockTime();

		if (team) ((TeamHand) hands.get(getCurrentSide())).next();
		super.resetTimer(shkn);
	}

	@Override
	public void close() {
		if (!isOpen()) return;
		listener.close();
		recordLast();
		super.close();

		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> a.isInvalid(this, e.getKey(), true));
		}

		if (!draw && getCustom() == null) {
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
						acc.getAchievements().addAll(achs);

						if (!achs.isEmpty())
							acc.addGem(achs.stream().mapToInt(Achievement::getValue).sum());

						AccountDAO.saveAccount(acc);

						if (h.getHero() != null && tourMatch == null) {
							Hero hr = KawaiponDAO.getHero(h.getAcc().getUid());

							if (hr != null) {
								if (isRanked() && Helper.chance(5)) {
									h.sendDM(":bulb: | Durante esta batalha " + hr.getName() + " obteve 2 pontos bônus de atributo devido à experiência de combate. GG!");
									hr.addBonusPoints(2);
								}

								hr.setHp(h.getHero().getHp());
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
					acc.getAchievements().addAll(achs);

					if (!achs.isEmpty())
						acc.addGem(achs.stream().mapToInt(Achievement::getValue).sum());

					AccountDAO.saveAccount(acc);

					if (h.getHero() != null && tourMatch == null) {
						Hero hr = KawaiponDAO.getHero(h.getAcc().getUid());

						if (hr != null) {
							if (isRanked() && Helper.chance(5)) {
								h.sendDM(":bulb: | Durante esta batalha " + hr.getName() + " obteve 2 pontos bônus de atributo devido à experiência de combate. GG!");
								hr.addBonusPoints(2);
							}

							hr.setHp(h.getHero().getHp());
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
				if (t.getPhase(tourMatch.phase()).isLast())
					t.setTPResult(winner);
				else
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
														} catch (URISyntaxException | DeploymentException | IOException | InterruptedException ignore) {
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
