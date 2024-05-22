/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.game;

import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.command.misc.SynthesizeCommand;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.PhaseConstraint;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.shoukan.*;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.model.records.SelectionAction;
import com.kuuhaku.model.records.SelectionCard;
import com.kuuhaku.model.records.shoukan.*;
import com.kuuhaku.model.records.shoukan.history.Match;
import com.kuuhaku.model.records.shoukan.history.Turn;
import com.kuuhaku.model.records.shoukan.snapshot.Player;
import com.kuuhaku.model.records.shoukan.snapshot.Slot;
import com.kuuhaku.model.records.shoukan.snapshot.StateSnap;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import com.ygimenez.json.JSONUtils;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang3.ArrayUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

public class Shoukan extends GameInstance<Phase> {
	public static final String GIF_PATH = "https://raw.githubusercontent.com/OtagamerZ/ShoukanAssets/master/gifs/";
	public static final String SKIN_PATH = "https://raw.githubusercontent.com/OtagamerZ/ShoukanAssets/master/skins/";

	private final Arcade arcade;
	private final Arena arena;
	private final Map<Side, Hand> hands;
	private final Map<String, String> messages = new HashMap<>();
	private final Set<EffectOverTime> eots = new HashSet<>();
	private final Set<TriggerBind> bindings = new HashSet<>();
	private final List<Turn> turns = new TreeList<>();

	private StateSnap snapshot = null;
	private int tick;
	private Side winner;

	private byte state = 0b100;
	/*
	0xF
	  └ 0 111111
	      │││││└ singleplayer
	      ││││└─ cheats
	      │││└── restoring
	      ││└─── history
	      │└──── lock
	      └───── sending
	 */

	public Shoukan(I18N locale, Arcade arcade, User p1, User p2) {
		this(locale, arcade, p1.getId(), p2.getId());
	}

	public Shoukan(I18N locale, Arcade arcade, String p1, String p2) {
		super(locale, new String[]{p1, p2});

		this.arcade = arcade;
		this.arena = new Arena(this);

		setSingleplayer(p1.equals(p2));
		this.hands = Map.of(Side.TOP, new Hand(p1, this, Side.TOP), Side.BOTTOM, new Hand(p2, this, Side.BOTTOM));

		setTimeout(turn -> {
			if (getCurrent().selectionPending() && isLocked()) {
				reportResult(GameReport.GAME_TIMEOUT, getCurrentSide(), "str/game_wo", "<@" + getCurrent().getUid() + ">");
			}

			reportResult(GameReport.GAME_TIMEOUT, getOtherSide(), "str/game_wo", "<@" + getOther().getUid() + ">");
		}, 5, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return ((Predicate<Message>) m -> Utils.equalsAny(m.getAuthor().getId(), getPlayers())).and(m -> isSingleplayer() || getTurn() % 2 == ArrayUtils.indexOf(getPlayers(), m.getAuthor().getId()) || hands.values().stream().anyMatch(h -> h.getUid().equals(m.getAuthor().getId()) && h.selectionPending())).test(message);
	}

	@Override
	protected void begin() {
		setRestoring(false);

		for (Hand h : hands.values()) {
			for (Drawable<?> d : h.getRealDeck()) {
				trigger(Trigger.ON_DECK, d.asSource(Trigger.ON_DECK));
			}

			h.manualDraw(h.getRemainingDraws());
			h.loadArchetype();

			if (h.getOrigins().isPure(Race.BEAST)) {
				double prcnt = Calc.prcntToInt(h.getUserDeck().getEvoWeight(), 24);
				h.getRegDeg().add(Math.max(0, h.getBase().hp() * prcnt), 0);
			}
		}

		setPhase(Phase.PLAN);

		Hand curr = getCurrent();
		curr.modMP(curr.getBase().mpGain().get());

		trigger(ON_TURN_BEGIN, curr.getSide());
		reportEvent("str/game_start", false, "<@" + curr.getUid() + ">");

		takeSnapshot();
	}

	@Override
	protected void runtime(User user, String value) {
		Side current = getCurrentSide();
		Hand hand = hands.values().stream()
				.sorted(Comparator.comparing(h -> h.getSide() == current, Comparator.reverseOrder()))
				.filter(h -> h.getUid().equals(user.getId()))
				.findFirst().orElseThrow();

		Pair<Method, JSONObject> action = toAction(value.toLowerCase().replace(" ", ""), m -> (!isLocked() || (hand.selectionPending() && m.getName().startsWith("sel"))) && hand.selectionPending() == m.getName().startsWith("sel") || m.getName().startsWith("deb"));

		execAction(hand, action);
	}

	private void execAction(Hand hand, Pair<Method, JSONObject> action) {
		if (action == null) return;

		Method m = action.getFirst();
		try {
			if (isLocked() && (!m.getName().startsWith("sel") && !m.getName().startsWith("deb"))) {
				return;
			} else if (m.getName().startsWith("deb")) {
				setCheated(true);
			}

			setLocked(true);
			m.invoke(this, hand.getSide(), action.getSecond());
		} catch (Exception e) {
			if (e.getCause() instanceof StackOverflowError) {
				Constants.LOGGER.error("Fatal error at {}", m.getName(), e);
				getChannel().sendMessage(getString("error/match_termination", GameReport.STACK_OVERFLOW)).queue();
				close(GameReport.STACK_OVERFLOW);
				return;
			}

			Constants.LOGGER.error("Failed to execute method {}", m.getName(), e);
		} finally {
			setLocked(false);
		}
	}

	@PlayerAction("reload")
	private boolean reload(Side side, JSONObject args) {
		reportEvent("str/game_reload", false, getCurrent().getName());
		return true;
	}

	// DEBUG START

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_hp,(?<value>\\d+)")
	private boolean debSetHp(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int val = args.getInt("value");
			curr.setHP(val);

			reportEvent("SET_HP -> " + val, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_mp,(?<value>\\d+)")
	private boolean debSetMp(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int val = args.getInt("value");
			curr.setMP(val);

			reportEvent("SET_MP -> " + val, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_origin,(?<major>\\w+)(?:,(?<minor>[\\w,]+))?")
	private boolean debSetOrigin(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			Race major = args.getEnum(Race.class, "major", Race.NONE);

			Set<Race> minors = new HashSet<>();
			JSONArray races = new JSONArray(Arrays.asList(args.getString("minor").split(",")));
			for (int i = 0; i < races.size(); i++) {
				Race minor = races.getEnum(Race.class, i);
				if (minor != null) {
					minors.add(minor);
				}
			}

			curr.setOrigin(Origin.from(curr.getUserDeck().isVariant(), major, minors.toArray(Race[]::new)));
			reportEvent("SET_ORIGIN -> " + curr.getOrigins(), false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("add,(?<card>[\\w-]+)(?:,(?<amount>\\d+))?")
	private boolean debAddCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			String id = args.getString("card").toUpperCase();
			CardType type = Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)).stream().findFirst().orElse(CardType.KAWAIPON);

			boolean add = false;
			int amount = args.getInt("amount", 1);
			for (int i = 0; i < amount; i++) {
				Drawable<?> d = switch (type) {
					case KAWAIPON, SENSHI -> DAO.find(Senshi.class, id);
					case EVOGEAR -> DAO.find(Evogear.class, id);
					case FIELD -> DAO.find(Field.class, id);
				};

				if (d != null) {
					add = true;
					curr.getCards().add(d.copy());
				}
			}

			if (add) {
				reportEvent("ADD_CARD -> " + amount + "x " + id, false);
				return true;
			}
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("undraw(?:,(?<amount>\\d+))?")
	private boolean debUndraw(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int valid = (int) curr.getCards().parallelStream().filter(Drawable::isAvailable).count();

			int amount = Math.min(args.getInt("amount", 1), valid);
			for (int i = 0; i < amount; i++) {
				curr.getRealDeck().add(curr.getCards().removeLast(Drawable::isAvailable));
			}

			reportEvent("UNDRAW -> " + amount, false);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("next_tick")
	private boolean debApplyTick(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			reportEvent("NEXT_TICK", true);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("save_history")
	private boolean debSaveHistory(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			Match m = new Match(this, "none");
			new MatchHistory(m).save();

			reportEvent("SAVE_HISTORY", true);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("terminate")
	private boolean debTerminate(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			reportResult(GameReport.SUCCESS, null, "GAME_TERMINATE");
		}

		return false;
	}

	// DEBUG END

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),(?<mode>[adb]),(?<inField>[1-5])(?<notCombat>,nc)?")
	private boolean placeCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		int extraMp = 0;
		if (curr.getOrigins().synergy() == Race.HOMUNCULUS) {
			extraMp = curr.getDiscard().size();
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Senshi chosen) {
			if (chosen.getHPCost() >= curr.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp")).queue();
				return false;
			} else if (chosen.getMPCost() > curr.getMP() + extraMp) {
				getChannel().sendMessage(getString("error/not_enough_mp")).queue();
				return false;
			} else if (chosen.getSCCost() > curr.getDiscard().size()) {
				getChannel().sendMessage(getString("error/not_enough_sc")).queue();
				return false;
			}
		} else {
			if (args.getString("mode").equals("b") && placeProxy(curr, args)) return true;
			else if (curr.getLockTime(Lock.BLIND) > 0) {
				d.setAvailable(false);
				curr.getGraveyard().add(d.copy());
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				CardState state = switch (args.getString("mode")) {
					case "d" -> CardState.DEFENSE;
					case "b" -> CardState.FLIPPED;
					default -> CardState.ATTACK;
				};

				reportEvent("str/place_card_fail", true, curr.getName(), d, state.toString(getLocale()));
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
		if (slot.isLocked()) {
			int time = slot.getLock();

			if (time == -1) {
				getChannel().sendMessage(getString("error/slot_locked_perma")).queue();
			} else {
				getChannel().sendMessage(getString("error/slot_locked", time)).queue();
			}
			return false;
		}

		Senshi copy;
		int usedExtra = Calc.clamp(chosen.getMPCost() - curr.getMP(), 0, extraMp);
		if (args.has("notCombat")) {
			if (slot.hasBottom()) {
				getChannel().sendMessage(getString("error/slot_occupied")).queue();
				return false;
			}

			chosen.setAvailable(false);
			slot.setBottom(copy = chosen.withCopy(s -> {
				switch (args.getString("mode")) {
					case "d" -> s.setDefending(true);
					case "b" -> s.setFlipped(true);
				}

				if (curr.getOrigins().synergy() != Race.HERALD || curr.hasSummoned()) {
					curr.consumeHP(s.getHPCost());
					curr.consumeMP(s.getMPCost() - usedExtra);
				}

				List<Drawable<?>> consumed = curr.consumeSC(s.getSCCost() + usedExtra);
				if (!consumed.isEmpty()) {
					s.getStats().getData().put("consumed", consumed);
				}
			}));
		} else {
			if (slot.hasTop()) {
				getChannel().sendMessage(getString("error/slot_occupied")).queue();
				return false;
			}

			chosen.setAvailable(false);
			slot.setTop(copy = chosen.withCopy(s -> {
				switch (args.getString("mode")) {
					case "d" -> s.setDefending(true);
					case "b" -> s.setFlipped(true);
				}

				if (curr.getOrigins().synergy() != Race.HERALD || curr.hasSummoned()) {
					curr.consumeHP(s.getHPCost());
					curr.consumeMP(s.getMPCost() - usedExtra);
				}

				List<Drawable<?>> consumed = curr.consumeSC(s.getSCCost() + usedExtra);
				if (!consumed.isEmpty()) {
					s.getStats().getData().put("consumed", consumed);
				}
			}));
		}

		curr.setSummoned(true);
		curr.getData().put("last_summon", copy);
		reportEvent("str/place_card", true, curr.getName(), copy.isFlipped() ? getString("str/a_card") : copy, copy.getState().toString(getLocale()));
		return true;
	}

	private boolean placeProxy(Hand hand, JSONObject args) {
		int extraMp = 0;
		if (hand.getOrigins().synergy() == Race.HOMUNCULUS) {
			extraMp = hand.getDiscard().size();
		}

		Drawable<?> d = hand.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Evogear chosen && chosen.isSpell()) {
			if (chosen.isPassive()) {
				getChannel().sendMessage(getString("error/card_passive")).queue();
				return false;
			} else if (chosen.getHPCost() >= hand.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp")).queue();
				return false;
			} else if (chosen.getMPCost() > hand.getMP()) {
				getChannel().sendMessage(getString("error/not_enough_mp")).queue();
				return false;
			} else if (chosen.getSCCost() > hand.getDiscard().size()) {
				getChannel().sendMessage(getString("error/not_enough_sc")).queue();
				return false;
			}

			int locktime = hand.getLockTime(Lock.SPELL);
			if (locktime > 0 && !chosen.hasTrueEffect()) {
				getChannel().sendMessage(getString("error/spell_locked", locktime)).queue();
				return false;
			}
		} else {
			if (hand.getLockTime(Lock.BLIND) > 0) {
				d.setAvailable(false);
				hand.getGraveyard().add(d.copy());
				hand.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				CardState state = switch (args.getString("mode")) {
					case "d" -> CardState.DEFENSE;
					case "b" -> CardState.FLIPPED;
					default -> CardState.ATTACK;
				};

				reportEvent("str/place_card_fail", true, hand.getName(), d, state.toString(getLocale()));
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		SlotColumn slot = arena.getSlots(hand.getSide()).get(args.getInt("inField") - 1);
		if (slot.isLocked()) {
			int time = slot.getLock();

			if (time == -1) {
				getChannel().sendMessage(getString("error/slot_locked_perma")).queue();
			} else {
				getChannel().sendMessage(getString("error/slot_locked", time)).queue();
			}
			return false;
		}

		TrapSpell proxy = new TrapSpell(chosen);
		int usedExtra = Calc.clamp(chosen.getMPCost() - hand.getMP(), 0, extraMp);
		if (args.has("notCombat")) {
			if (slot.hasBottom()) {
				getChannel().sendMessage(getString("error/slot_occupied")).queue();
				return false;
			}

			if (hand.getOrigins().synergy() != Race.HERALD || hand.hasSummoned()) {
				hand.consumeHP(chosen.getHPCost());
				hand.consumeMP(chosen.getMPCost() - usedExtra);
			}

			List<Drawable<?>> consumed = hand.consumeSC(chosen.getSCCost());
			if (!consumed.isEmpty()) {
				proxy.getStats().getData().put("consumed", consumed);
			}

			chosen.setAvailable(false);
			slot.setBottom(proxy);
		} else {
			if (slot.hasTop()) {
				getChannel().sendMessage(getString("error/slot_occupied")).queue();
				return false;
			}

			if (hand.getOrigins().synergy() != Race.HERALD || hand.hasSummoned()) {
				hand.consumeHP(chosen.getHPCost());
				hand.consumeMP(chosen.getMPCost() - usedExtra);
			}

			List<Drawable<?>> consumed = hand.consumeSC(chosen.getSCCost());
			if (!consumed.isEmpty()) {
				proxy.getStats().getData().put("consumed", consumed);
			}

			chosen.setAvailable(false);
			slot.setTop(proxy);
		}

		hand.setSummoned(true);
		reportEvent("str/place_card", true, hand.getName(), proxy.isFlipped() ? getString("str/a_card") : proxy, proxy.getState().toString(getLocale()));
		return true;
	}

	public boolean activateProxy(Senshi proxy, EffectParameters ep) {
		if (!(proxy instanceof TrapSpell p)) return false;

		Evogear e = p.getOriginal();
		Hand hand = e.getHand();

		Targeting tgt = switch (e.getTargetType()) {
			case NONE -> new Targeting(hand, -1, -1);
			case ALLY -> {
				if (ep.allies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, ep.allies()[0].index(), -1);
			}
			case ENEMY -> {
				if (ep.enemies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, -1, ep.enemies()[0].index());
			}
			case BOTH -> {
				if (ep.allies().length == 0 || ep.enemies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, ep.allies()[0].index(), ep.enemies()[0].index());
			}
		};

		if (tgt == null) return false;

		if (!tgt.validate(e.getTargetType())) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + e.getTargetType()))).queue();
			return false;
		}

		if (e.execute(ep)) {
			hand.getGraveyard().add(p);
			hand.getData().put("last_spell", e);
			hand.getData().put("last_evogear", e);
			trigger(ON_SPELL, hand.getSide());
			return true;
		}

		return false;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),(?<inField>[1-5])")
	private boolean equipCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> orig = curr.getCards().get(args.getInt("inHand") - 1);
		Drawable<?> d = orig;
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Senshi s && curr.getOrigins().synergy() == Race.SHIKIGAMI) {
			d = new EquippableSenshi(s.copy());

			EquippableSenshi es = (EquippableSenshi) d;
			es.getStats().getData().put("_shiki", true);
			es.getStats().getAttrMult().set(-0.4);
		}

		if (d instanceof Evogear chosen && !chosen.isSpell()) {
			if (chosen.getHPCost() >= curr.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp")).queue();
				return false;
			} else if (chosen.getMPCost() > curr.getMP()) {
				getChannel().sendMessage(getString("error/not_enough_mp")).queue();
				return false;
			} else if (chosen.getSCCost() > curr.getDiscard().size()) {
				getChannel().sendMessage(getString("error/not_enough_sc")).queue();
				return false;
			}
		} else {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				d.setAvailable(false);
				curr.getGraveyard().add(d.copy());
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
				if (!slot.hasTop()) {
					getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
					return false;
				}

				Senshi target = slot.getTop();
				reportEvent("str/equip_card_fail", true, curr.getName(), d, target.isFlipped() ? getString("str/a_card") : target);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
		if (!slot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Evogear copy = chosen.copy();
		Senshi target = slot.getTop();
		if (target.getEquipments().stream().anyMatch(e -> chosen instanceof EquippableSenshi && e.getStats().getData().has("_shiki"))) {
			getChannel().sendMessage(getString("error/only_one_shikigami")).queue();
			return false;
		}

		curr.consumeHP(copy.getHPCost());
		curr.consumeMP(copy.getMPCost());
		List<Drawable<?>> consumed = curr.consumeSC(copy.getSCCost());
		if (!consumed.isEmpty()) {
			copy.getStats().getData().put("consumed", consumed);
		}

		if (chosen instanceof EquippableSenshi) {
			orig.setAvailable(false);
		} else {
			chosen.setAvailable(false);
		}

		target.getEquipments().add(copy);
		curr.getData().put("last_equipment", copy);
		curr.getData().put("last_evogear", copy);
		reportEvent("str/equip_card", true, curr.getName(), copy.isFlipped() ? getString("str/an_equipment") : copy, target.isFlipped() ? getString("str/a_card") : target);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+)f")
	private boolean placeField(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (!(d instanceof Field chosen)) {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				d.setAvailable(false);
				curr.getGraveyard().add(d.copy());
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);
				reportEvent("str/equip_card_fail", true, curr.getName(), d);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		chosen.setAvailable(false);
		arena.setField(chosen.copy());
		curr.getData().put("last_field", chosen);
		reportEvent("str/place_field", true, curr.getName(), chosen);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),f(?<notCombat>,nc)?")
	private boolean flipCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		boolean nc = args.has("notCombat");
		if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Senshi d = nc ? slot.getBottom() : slot.getTop();
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		} else if (d.hasSwitched()) {
			getChannel().sendMessage(getString("error/card_switched")).queue();
			return false;
		}

		if (d.isFlipped()) {
			d.setFlipped(false);
		} else {
			d.setDefending(!d.isDefending());
		}

		d.setSwitched(true);
		reportEvent("str/flip_card", true, curr.getName(), d, d.getState().toString(getLocale()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),p")
	private boolean promoteCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		if (!slot.hasBottom()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		} else if (slot.hasTop()) {
			getChannel().sendMessage(getString("error/promote_blocked")).queue();
			return false;
		}

		Senshi d = slot.getBottom();
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		slot.swap();

		reportEvent("str/promote_card", true, curr.getName(), d.isFlipped() ? getString("str/a_card") : d);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),s(?<notCombat>,nc)?")
	private boolean sacrificeCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		boolean nc = args.has("notCombat");
		if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		double mult = 0.5;
		if (curr.getOther().getOrigins().synergy() == Race.INFERNAL) {
			mult *= 2;
		}

		Senshi chosen = nc ? slot.getBottom() : slot.getTop();
		if (chosen.isFixed()) {
			getChannel().sendMessage(getString("error/card_fixed")).queue();
			return false;
		} else if ((int) (chosen.getHPCost() * mult) >= curr.getHP()) {
			getChannel().sendMessage(getString("error/not_enough_hp_sacrifice")).queue();
			return false;
		} else if ((int) (chosen.getMPCost() * mult) > curr.getMP()) {
			getChannel().sendMessage(getString("error/not_enough_mp_sacrifice")).queue();
			return false;
		}

		curr.consumeHP((int) (chosen.getHPCost() * mult));
		curr.consumeMP((int) (chosen.getMPCost() * mult));

		trigger(ON_SACRIFICE, chosen.asSource(ON_SACRIFICE));

		curr.getGraveyard().add(chosen);

		reportEvent("str/sacrifice_card", true, curr.getName(), chosen);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>\\[[1-5](,[1-5])*]),s(?<notCombat>,nc)?")
	private boolean sacrificeBatch(Side side, JSONObject args) {
		Hand curr = hands.get(side);

		int hp = 0;
		int mp = 0;

		List<Drawable<?>> cards = new ArrayList<>();
		JSONArray batch = new JSONArray(args.getString("inField"));
		for (Object o : batch) {
			int idx = ((Number) o).intValue();
			SlotColumn slot = arena.getSlots(curr.getSide()).get(idx - 1);

			boolean nc = args.has("notCombat");
			if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
				getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
				return false;
			}

			Senshi chosen = nc ? slot.getBottom() : slot.getTop();
			if (chosen.getHPCost() / 2 >= curr.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp_sacrifice")).queue();
				return false;
			} else if (chosen.getMPCost() / 2 > curr.getMP()) {
				getChannel().sendMessage(getString("error/not_enough_mp_sacrifice")).queue();
				return false;
			}

			hp += chosen.getHPCost() / 2;
			mp += chosen.getMPCost() / 2;
			cards.add(chosen);
		}

		curr.consumeHP(hp);
		curr.consumeMP(mp);

		for (Drawable<?> chosen : cards) {
			trigger(ON_SACRIFICE, chosen.asSource(ON_SACRIFICE));
		}

		curr.getGraveyard().addAll(cards);

		reportEvent("str/sacrifice_card", true, curr.getName(), Utils.properlyJoin(getString("str/and")).apply(cards.stream().map(Drawable::toString).toList()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),d")
	private boolean discardCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		curr.getDiscard().add(d);

		if (curr.getOrigins().synergy() == Race.FAMILIAR && d instanceof Senshi s) {
			for (Drawable<?> c : curr.getCards()) {
				if (c.isAvailable() && c instanceof Senshi it && it.getElement() == s.getElement()) {
					it.getStats().getMana().set(-1);
				}
			}
		}

		reportEvent("str/discard_card", true, curr.getName(), d);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\[\\d+(,\\d+)*]),d")
	private boolean discardBatch(Side side, JSONObject args) {
		Hand curr = hands.get(side);

		List<Drawable<?>> cards = new ArrayList<>();
		JSONArray batch = new JSONArray(args.getString("inHand"));
		for (Object o : batch) {
			int idx = ((Number) o).intValue();
			if (!Utils.between(idx, 1, curr.getCards().size())) {
				getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
				return false;
			}

			Drawable<?> d = curr.getCards().get(idx - 1);
			if (!d.isAvailable() || d.isManipulated()) {
				getChannel().sendMessage(getString("error/card_unavailable")).queue();
				return false;
			}

			cards.add(d);
		}

		curr.getDiscard().addAll(cards);

		if (curr.getOrigins().synergy() == Race.FAMILIAR) {
			for (Drawable<?> c : cards) {
				if (c instanceof Senshi s) {
					for (Drawable<?> d : curr.getCards()) {
						if (d.isAvailable() && d instanceof Senshi it && it.getElement() == s.getElement()) {
							it.getStats().getMana().set(-1);
						}
					}
				}
			}
		}

		reportEvent("str/discard_card", true, curr.getName(), Utils.properlyJoin(getString("str/and")).apply(cards.stream().map(Drawable::toString).toList()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+)s(?:,(?<target1>[1-5]))?(?:,(?<target2>[1-5]))?")
	private boolean activate(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Evogear chosen && chosen.isSpell()) {
			if (chosen.isPassive()) {
				getChannel().sendMessage(getString("error/card_passive")).queue();
				return false;
			} else if (chosen.getHPCost() >= curr.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp")).queue();
				return false;
			} else if (chosen.getMPCost() > curr.getMP()) {
				getChannel().sendMessage(getString("error/not_enough_mp")).queue();
				return false;
			} else if (chosen.getSCCost() > curr.getDiscard().size()) {
				getChannel().sendMessage(getString("error/not_enough_sc")).queue();
				return false;
			}

			int locktime = curr.getLockTime(Lock.SPELL);
			if (locktime > 0 && !chosen.hasTrueEffect()) {
				getChannel().sendMessage(getString("error/spell_locked", locktime)).queue();
				return false;
			}
		} else {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				d.setAvailable(false);
				curr.getGraveyard().add(d.copy());
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				reportEvent("str/activate_card_fail", true, curr.getName(), d);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		Targeting tgt = switch (chosen.getTargetType()) {
			case NONE -> new Targeting(curr, -1, -1);
			case ALLY -> new Targeting(curr, args.getInt("target1") - 1, -1);
			case ENEMY -> new Targeting(curr, -1, args.getInt("target1") - 1);
			case BOTH -> new Targeting(curr, args.getInt("target1") - 1, args.getInt("target2") - 1);
		};

		List<Drawable<?>> stack = (chosen.getTier() > 3 ? arena.getBanned() : curr.getGraveyard());
		if (!tgt.validate(chosen.getTargetType())) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + chosen.getTargetType()))).queue();
			return false;
		}

		curr.consumeHP(chosen.getHPCost());
		curr.consumeMP(chosen.getMPCost());
		List<Drawable<?>> consumed = curr.consumeSC(chosen.getSCCost());
		if (!consumed.isEmpty()) {
			chosen.getStats().getData().put("consumed", consumed);
		}

		if (!chosen.execute(chosen.toParameters(tgt))) {
			if (!chosen.isAvailable()) {
				chosen.setAvailable(false);
				reportEvent("str/effect_interrupted", true, chosen);
				return true;
			}

			return false;
		}

		if (!chosen.hasFlag(Flag.FREE_ACTION, true)) {
			chosen.setAvailable(false);
			stack.add(chosen.copy());
		}

		curr.getData().put("last_spell", chosen);
		curr.getData().put("last_evogear", chosen);
		trigger(ON_SPELL, side);
		reportEvent("str/activate_card", true, curr.getName(), chosen.getBase().getTags().contains("SECRET") ? getString("str/a_spell") : chosen);
		return true;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("(?<choices>\\d+(,\\d+)*)")
	private boolean selSelect(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!curr.selectionPending()) return false;

		SelectionAction selection = curr.getSelection();
		String[] choices = args.getString("choices").split(",");

		List<Integer> indexes = new ArrayList<>();
		for (String choice : choices) {
			int idx = Integer.parseInt(choice) - 1;
			if (!Utils.between(idx, 0, selection.cards().size() - 1)) {
				getChannel().sendMessage(getString("error/invalid_selection_index")).queue();
				return false;
			}

			indexes.add(idx);
		}

		for (Integer idx : indexes) {
			List<Integer> selIdxs = selection.indexes();
			if (selIdxs.contains(idx)) {
				selIdxs.remove(idx);
			} else {
				selIdxs.add(idx);
			}

			if (selIdxs.size() > selection.required()) {
				selIdxs.removeFirst();
			}
		}

		curr.showChoices();
		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("ok")
	private boolean selConfirm(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!curr.selectionPending()) return false;

		SelectionAction sel = curr.getSelection();
		if (sel.indexes().size() != sel.required()) {
			getChannel().sendMessage(getString("error/wrong_selection_amount", sel.required())).queue();
			return false;
		}

		List<Drawable<?>> cards = new ArrayList<>();
		for (int i : sel.indexes()) {
			cards.add(sel.cards().get(i).card());
		}

		int tick = this.tick;
		curr.getSelection().result()
				.add(t -> {
					if (tick == this.tick) {
						if (sel.source() != null) {
							reportEvent(getString("str/effect_choice", curr.getName(), sel.required(), sel.source()), true);
						} else {
							reportEvent(getString("str/effect_choice_ns", curr.getName(), sel.required()), true);
						}
					}

					return t;
				})
				.process(cards);
		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("(?<inField>[1-5])a(?:,(?<target1>[1-5]))?(?:,(?<target2>[1-5]))?")
	private boolean special(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		if (!slot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Senshi d = slot.getTop();
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		} else if (d.isFlipped()) {
			getChannel().sendMessage(getString("error/card_flipped")).queue();
			return false;
		} else if (d.getCooldown() > 0) {
			getChannel().sendMessage(getString("error/card_cooldown", d.getCooldown())).queue();
			return false;
		} else if (curr.getMP() < 1) {
			getChannel().sendMessage(getString("error/not_enough_mp")).queue();
			return false;
		} else if (!d.hasAbility()) {
			getChannel().sendMessage(getString("error/card_no_special")).queue();
			return false;
		} else if (d.isSealed()) {
			getChannel().sendMessage(getString("error/card_sealed")).queue();
			return false;
		}

		int locktime = curr.getLockTime(Lock.ABILITY);
		if (locktime > 0 && !d.hasTrueEffect()) {
			getChannel().sendMessage(getString("error/ability_locked", locktime)).queue();
			return false;
		}

		locktime = curr.getLockTime(Lock.EFFECT);
		if (locktime > 0 && !d.hasTrueEffect()) {
			getChannel().sendMessage(getString("error/effect_locked", locktime)).queue();
			return false;
		}

		TargetType type = d.getTargetType();
		Targeting tgt = switch (type) {
			case NONE -> new Targeting(curr, -1, -1);
			case ALLY -> new Targeting(curr, args.getInt("target1") - 1, -1);
			case ENEMY -> new Targeting(curr, -1, args.getInt("target1") - 1);
			case BOTH -> new Targeting(curr, args.getInt("target1") - 1, args.getInt("target2") - 1);
		};

		Senshi enemy = tgt.enemy();
		if (enemy != null) {
			if (d.getTarget() != null && !Objects.equals(d.getTarget(), enemy)) {
				getChannel().sendMessage(getString("error/card_taunted", d.getTarget(), d.getTarget().getIndex() + 1)).queue();
				return false;
			}
		}

		if (!tgt.validate(type)) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + type))).queue();
			return false;
		} else if (!trigger(ON_ACTIVATE, d.asSource(ON_ACTIVATE), tgt.targets(ON_EFFECT_TARGET))) {
			if (!d.isAvailable()) {
				reportEvent("str/effect_interrupted", true, d);
				return true;
			}

			return false;
		}

		curr.consumeMP(1);
		if (getPhase() != Phase.PLAN && !d.hasFlag(Flag.FREE_ACTION, true)) {
			d.setAvailable(false);
		}

		curr.getData().put("last_ability", d);
		trigger(ON_ABILITY, side);
		reportEvent("str/card_special", true, curr.getName(), d);
		return !curr.selectionPending();
	}

	@PhaseConstraint("COMBAT")
	@PlayerAction("(?<inField>[1-5]),self")
	private boolean selfDamage(Side side, JSONObject args) {
		Hand you = hands.get(side);
		if (you.getLockTime(Lock.TAUNT) == 0) {
			getChannel().sendMessage(getString("error/not_taunted")).queue();
			return false;
		}

		SlotColumn yourSlot = arena.getSlots(you.getSide()).get(args.getInt("inField") - 1);

		if (!yourSlot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", yourSlot.getIndex() + 1)).queue();
			return false;
		}

		Senshi ally = yourSlot.getTop();
		attack(ally, you, null, true);

		return false;
	}

	@PhaseConstraint("COMBAT")
	@PlayerAction("(?<inField>[1-5])(?:,(?<target>[1-5]))?")
	private boolean attack(Side side, JSONObject args) {
		Hand you = hands.get(side);
		SlotColumn yourSlot = arena.getSlots(you.getSide()).get(args.getInt("inField") - 1);

		if (!yourSlot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", yourSlot.getIndex() + 1)).queue();
			return false;
		}

		Senshi ally = yourSlot.getTop();
		Hand op = you.getOther();

		Senshi enemy = null;
		if (args.has("target")) {
			SlotColumn opSlot = arena.getSlots(op.getSide()).get(args.getInt("target") - 1);

			if (!opSlot.hasTop()) {
				if (!opSlot.hasBottom()) {
					getChannel().sendMessage(getString("error/missing_card", opSlot.getIndex() + 1)).queue();
					return false;
				}

				enemy = opSlot.getBottom();
			} else {
				enemy = opSlot.getTop();
			}
		}

		if (enemy == null) {
			attack(ally, op, null, true);
		} else {
			attack(ally, enemy, null, true);
		}

		return false;
	}

	public boolean attack(Senshi source, Senshi target) {
		return attack(source, target, null, false);
	}

	public boolean attack(Senshi source, Senshi target, int dmg) {
		return attack(source, target, dmg, false);
	}

	private boolean attack(Senshi source, Senshi target, Integer dmg, boolean announce) {
		if (target == null) return false;
		else if (source == null || ((announce && !source.canAttack()) || !source.isAvailable())) {
			if (announce) {
				getChannel().sendMessage(getString("error/card_cannot_attack")).queue();
			}

			return false;
		}

		Hand you = source.getHand();
		Hand op = target.getHand();
		int pHP = you.getHP();
		int eHP = op.getHP();

		if (source.getTarget() != null && !Objects.equals(source.getTarget(), target)) {
			if (announce) {
				getChannel().sendMessage(getString("error/card_taunted", source.getTarget(), source.getTarget().getIndex() + 1)).queue();
			}

			return false;
		}

		if (target.isStasis()) {
			if (announce) {
				getChannel().sendMessage(getString("error/card_untargetable")).queue();
			}

			return false;
		}

		Target t = target.asTarget(ON_DEFEND);
		int posHash = target.posHash();
		trigger(ON_ATTACK, source.asSource(ON_ATTACK), t);

		if (dmg == null) {
			dmg = source.getActiveAttr();
		}

		if (you.getOrigins().synergy() == Race.DOPPELGANGER && source.getId().equals(target.getId())) {
			dmg *= 2;
		}

		int direct = 0;
		int lifesteal = you.getBase().lifesteal() + (int) source.getStats().getLifesteal().get();
		if (you.getOrigins().synergy() == Race.VAMPIRE && you.isLowLife()) {
			lifesteal += 7;
		}

		int thorns = (op.getLockTime(Lock.CHARM) > 0 ? 20 : 0) + (int) target.getStats().getThorns().get();
		double dmgMult = 1;
		if (dmg < 0 && (getTurn() < 3 || you.getLockTime(Lock.TAUNT) > 0)) {
			dmgMult /= 2;
		}

		boolean win = false;
		String outcome = getString("str/combat_skip");
		try {
			if (posHash == target.posHash() && ((announce && source.canAttack()) || source.isAvailable())) {
				for (Evogear e : source.getEquipments()) {
					JSONArray charms = e.getCharms();

					for (Object o : charms) {
						Charm c = Charm.valueOf(String.valueOf(o));
						switch (c) {
							case PIERCING -> direct += dmg * c.getValue(e.getTier()) / 100;
							case WOUNDING -> {
								int val = (int) -(dmg * dmgMult * c.getValue(e.getTier()) / 100);
								op.getRegDeg().add(val);
							}
							case DRAIN -> {
								int toDrain = Math.min(c.getValue(e.getTier()), op.getMP());
								if (toDrain > 0) {
									you.modMP(toDrain);
									op.modMP(-toDrain);
								}
							}
							case LIFESTEAL -> lifesteal += c.getValue(e.getTier());
						}
					}
				}

				target.setFlipped(false);

				for (Evogear e : target.getEquipments()) {
					JSONArray charms = e.getCharms();

					for (Object o : charms) {
						Charm c = Charm.valueOf(String.valueOf(o));
						if (c == Charm.THORNS) {
							thorns += c.getValue(e.getTier());
						}
					}
				}

				switch (you.getOrigins().synergy()) {
					case ELEMENTAL -> {
						int water = (int) getCards(you.getSide()).parallelStream()
								.filter(s -> s.getElement() == ElementType.WATER)
								.count();

						if (water >= 4) {
							you.modMP(1);
						}
					}
					case FALLEN -> {
						if (op.getRegDeg().peek() < 0) {
							op.getRegDeg().apply(0.2);
						}
					}
				}

				boolean ignore = source.hasFlag(Flag.NO_COMBAT, true);
				if (!ignore) {
					ignore = target.getSlot().getIndex() == -1 || target.hasFlag(Flag.IGNORE_COMBAT, true);
				}

				if (!ignore) {
					if (target.isSupporting()) {
						outcome = getString("str/combat_success", dmg, 0);

						for (Senshi s : target.getNearby()) {
							s.awaken();
						}

						op.getGraveyard().add(target);

						dmg = 0;
						win = true;
					} else {
						boolean dbl = op.getOrigins().synergy() == Race.CYBERBEAST && chance(20);
						boolean unstop = source.hasFlag(Flag.UNSTOPPABLE, true);

						int enemyStats = target.getActiveAttr(dbl);
						if (source.hasFlag(Flag.IGNORE_EQUIP, true)) {
							enemyStats -= target.getActiveEquips(dbl);
						}

						if (!unstop && dmg < enemyStats) {
							outcome = getString("str/combat_defeat", dmg, enemyStats);
							trigger(ON_SUICIDE, source.asSource(ON_SUICIDE), target.asTarget(ON_BLOCK));

							for (Senshi s : source.getNearby()) {
								s.awaken();
							}

							if (announce) {
								if (!source.hasFlag(Flag.NO_DAMAGE, true)) {
									you.modHP((int) -((enemyStats - dmg) * dmgMult));
								}

								you.getGraveyard().add(source);
							}

							dmg = 0;
						} else {
							int block = target.getBlock();
							int dodge = target.getDodge();

							if (source.isBlinded(true)) {
								outcome = getString("str/combat_miss");
								trigger(ON_MISS, source.asSource(ON_MISS));

								dmg = 0;
							} else if (!unstop && !source.hasFlag(Flag.TRUE_STRIKE, true) && (target.hasFlag(Flag.TRUE_BLOCK, true) || chance(block))) {
								outcome = getString("str/combat_block", block);
								trigger(NONE, source.asSource(), target.asTarget(ON_BLOCK));

								source.setStun(1);

								dmg = 0;
							} else if (!source.hasFlag(Flag.TRUE_STRIKE, true) && (target.hasFlag(Flag.TRUE_DODGE, true) || chance(dodge))) {
								outcome = getString("str/combat_dodge", dodge);
								trigger(ON_MISS, source.asSource(ON_MISS), target.asTarget(ON_DODGE));

								dmg = 0;
							} else {
								if (unstop || dmg > enemyStats) {
									outcome = getString("str/combat_success", dmg, enemyStats);
									trigger(ON_HIT, source.asSource(ON_HIT), target.asTarget(ON_LOSE));

									if (target.isDefending() || target.hasFlag(Flag.NO_DAMAGE, true)) {
										dmg = 0;
									} else {
										dmg = Math.max(0, dmg - enemyStats);
									}

									for (Senshi s : target.getNearby()) {
										s.awaken();
									}

									if (source.isDefending() && !source.hasFlag(Flag.ALWAYS_ATTACK, true)) {
										int duration;
										if (enemyStats == 0) {
											duration = 5;
										} else {
											duration = Utils.clamp(dmg / enemyStats, 1, 5);
										}

										if (you.getOrigins().synergy() == Race.ELEMENTAL) {
											int earth = (int) getCards(you.getSide()).parallelStream()
													.filter(s -> s.getElement() == ElementType.EARTH)
													.count();

											if (earth >= 4) {
												duration *= 2;
											}
										}

										target.setStun(duration);
										dmg = 0;
									} else {
										op.getGraveyard().add(target);
									}

									win = true;
								} else {
									outcome = getString("str/combat_clash", dmg, enemyStats);
									trigger(ON_CLASH, source.asSource(ON_SUICIDE), target.asTarget(ON_LOSE));

									for (Senshi s : target.getNearby()) {
										s.awaken();
									}

									op.getGraveyard().add(target);

									for (Senshi s : source.getNearby()) {
										s.awaken();
									}

									you.getGraveyard().add(source);

									dmg = 0;
								}
							}
						}
					}
				} else {
					dmg = 0;
				}

				op.modHP((int) -((dmg + direct) * dmgMult));
				op.addChain();

				int damage = Math.max(0, eHP - op.getHP());

				if (thorns > 0) {
					you.modHP(-dmg * thorns / 100);
				}
				if (lifesteal > 0) {
					you.modHP(dmg * lifesteal / 100);
				}

				if (you.getOrigins().synergy() == Race.DAEMON) {
					you.modMP((int) (damage / op.getBase().hp() * 0.05));
				}

				for (Evogear e : source.getEquipments()) {
					JSONArray charms = e.getCharms();

					for (Object o : charms) {
						Charm c = Charm.valueOf(String.valueOf(o));
						if (c == Charm.BARRAGE) {
							if (announce) {
								for (int i = 0; i < c.getValue(e.getTier()); i++) {
									attack(source, target, dmg / 10, false);
								}
							}
						}
					}
				}
			}
		} finally {
			if (announce && source.getSlot().getIndex() != -1 && !source.hasFlag(Flag.FREE_ACTION, true)) {
				source.setAvailable(false);
			}
		}

		if (eHP != op.getHP()) {
			int val = eHP - op.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_dealt" : "str/combat_heal_op", Math.abs(val));

			double mult = (val > 0 ? dmgMult : op.getStats().getHealMult().get());
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}
		if (pHP != you.getHP()) {
			int val = pHP - you.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_taken" : "str/combat_heal_self", Math.abs(val));

			double mult = (val > 0 ? you.getStats().getDamageMult() : you.getStats().getHealMult()).get();
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}

		if (announce) {
			reportEvent("str/combat", true, source, Utils.getOr(target, op.getName()), outcome.trim());
		}

		return win;
	}

	public boolean attack(Senshi source, Hand target) {
		return attack(source, target, null, false);
	}

	public boolean attack(Senshi source, Hand target, int dmg) {
		return attack(source, target, dmg, false);
	}

	private boolean attack(Senshi source, Hand target, Integer dmg, boolean announce) {
		if (source == null || target == null || ((announce && !source.canAttack()) || !source.isAvailable())) {
			if (announce) {
				getChannel().sendMessage(getString("error/card_cannot_attack")).queue();
			}

			return false;
		}

		Hand you = source.getHand();
		int pHP = you.getHP();
		int eHP = target.getHP();

		if (!arena.isFieldEmpty(target.getSide()) && !source.hasFlag(Flag.DIRECT, true)) {
			if (announce) {
				getChannel().sendMessage(getString("error/field_not_empty")).queue();
			}

			return false;
		}

		trigger(ON_ATTACK, source.asSource(ON_ATTACK));

		if (dmg == null) {
			if (source.isDefending() && !source.hasFlag(Flag.ALWAYS_ATTACK, true)) {
				dmg = 0;
			} else {
				dmg = source.getActiveAttr();
			}
		}

		int direct = 0;
		int lifesteal = you.getBase().lifesteal() + (int) source.getStats().getLifesteal().get();
		if (you.getOrigins().synergy() == Race.VAMPIRE && you.isLowLife()) {
			lifesteal += 7;
		}

		int thorns = target.getLockTime(Lock.CHARM) > 0 ? 20 : 0;
		double dmgMult = 1;
		if (dmg < 0 && (getTurn() < 3 || you.getLockTime(Lock.TAUNT) > 0)) {
			dmgMult /= 2;
		}

		try {
			if ((announce && source.canAttack()) || source.isAvailable()) {
				for (Evogear e : source.getEquipments()) {
					JSONArray charms = e.getCharms();

					for (Object o : charms) {
						Charm c = Charm.valueOf(String.valueOf(o));
						switch (c) {
							case PIERCING -> direct += dmg * c.getValue(e.getTier()) / 100;
							case WOUNDING -> {
								int val = (int) -(dmg * dmgMult * c.getValue(e.getTier()) / 100);
								target.getRegDeg().add(val);
							}
							case DRAIN -> {
								int toDrain = Math.min(c.getValue(e.getTier()), target.getMP());
								if (toDrain > 0) {
									you.modMP(toDrain);
									target.modMP(-toDrain);
								}
							}
							case LIFESTEAL -> lifesteal += c.getValue(e.getTier());
						}
					}
				}

				switch (you.getOrigins().synergy()) {
					case ELEMENTAL -> {
						int water = (int) getCards(you.getSide()).parallelStream()
								.filter(s -> s.getElement() == ElementType.WATER)
								.count();

						if (water >= 4) {
							you.modMP(1);
						}
					}
					case FALLEN -> {
						if (target.getRegDeg().peek() < 0) {
							target.getRegDeg().apply(0.2);
						}
					}
				}

				if (!source.hasFlag(Flag.NO_COMBAT, true)) {
					for (SlotColumn sc : getSlots(target.getSide())) {
						for (Senshi card : sc.getCards()) {
							if (card instanceof TrapSpell) {
								EffectParameters params = new EffectParameters(ON_TRAP, target.getSide(), card.asSource(ON_TRAP), source.asTarget(ON_ATTACK, TargetType.ENEMY));

								if (activateProxy(card, params)) {
									source.setAvailable(false);
									getChannel().sendMessage(getString("str/trap_activation", card)).queue();
								}
							}
						}
					}

					if ((announce && source.canAttack()) || source.isAvailable()) {
						trigger(ON_DIRECT, source.asSource(ON_DIRECT));
					} else {
						dmg = 0;
					}
				}

				target.modHP((int) -((dmg + direct) * dmgMult));
				target.addChain();

				if (target.getOrigins().synergy() == Race.SUCCUBUS) {
					you.modLockTime(Lock.CHARM, 1);
				}

				int damage = Math.max(0, eHP - target.getHP());

				if (thorns > 0) {
					you.modHP(-dmg * thorns / 100);
				}
				if (lifesteal > 0) {
					you.modHP(dmg * lifesteal / 100);
				}

				if (you.getOrigins().synergy() == Race.DAEMON) {
					you.modMP((int) (damage / target.getBase().hp() * 0.05));
				}

				for (Evogear e : source.getEquipments()) {
					JSONArray charms = e.getCharms();

					for (Object o : charms) {
						Charm c = Charm.valueOf(String.valueOf(o));
						if (c == Charm.BARRAGE) {
							if (announce) {
								for (int i = 0; i < c.getValue(e.getTier()); i++) {
									attack(source, target, dmg / 10, false);
								}
							}
						}
					}
				}
			}
		} finally {
			if (announce && source.getSlot().getIndex() != -1 && !source.hasFlag(Flag.FREE_ACTION, true)) {
				source.setAvailable(false);
			}
		}

		String outcome = "";
		if (eHP != target.getHP()) {
			int val = eHP - target.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_dealt" : "str/combat_heal_op", Math.abs(val));

			double mult = (val > 0 ? dmgMult : target.getStats().getHealMult().get());
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}

		if (pHP != you.getHP()) {
			int val = pHP - you.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_taken" : "str/combat_heal_self", Math.abs(val));

			double mult = (val > 0 ? you.getStats().getDamageMult() : you.getStats().getHealMult()).get();
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}

		if (announce) {
			reportEvent("str/combat", true, source, target.getName(), outcome);
		}

		return true;
	}

	public boolean chance(double percentage) {
		return Calc.chance(percentage, 1, getRng());
	}

	public Arcade getArcade() {
		return arcade;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Hand getCurrent() {
		return hands.get(getCurrentSide());
	}

	public Side getCurrentSide() {
		return getTurn() % 2 == 0 ? Side.TOP : Side.BOTTOM;
	}

	public Hand getOther() {
		return hands.get(getOtherSide());
	}

	public Side getOtherSide() {
		return getTurn() % 2 == 1 ? Side.TOP : Side.BOTTOM;
	}

	public Arena getArena() {
		return arena;
	}

	public List<Turn> getTurns() {
		return turns;
	}

	public boolean isSingleplayer() {
		return Bit.on(state, 0);
	}

	public void setSingleplayer(boolean sp) {
		state = (byte) Bit.set(state, 0, sp);
	}

	public boolean hasCheated() {
		return isSingleplayer() || Bit.on(state, 1);
	}

	public void setCheated(boolean cheated) {
		state = (byte) Bit.set(state, 1, cheated);
	}

	public boolean isRestoring() {
		return Bit.on(state, 2);
	}

	public void setRestoring(boolean restoring) {
		state = (byte) Bit.set(state, 2, restoring);
	}

	public boolean hasHistory() {
		return Bit.on(state, 3);
	}

	public void setHistory(boolean history) {
		state = (byte) Bit.set(state, 3, history);
	}

	public boolean isLocked() {
		return Bit.on(state, 4);
	}

	public void setLocked(boolean locked) {
		state = (byte) Bit.set(state, 4, locked);
	}

	public boolean isSending() {
		return Bit.on(state, 5);
	}

	public void setSending(boolean sending) {
		state = (byte) Bit.set(state, 5, sending);
	}

	public StateSnap getSnapshot() {
		return snapshot;
	}

	public StateSnap takeSnapshot() {
		try {
			snapshot = new StateSnap(this);
		} catch (IOException e) {
			Constants.LOGGER.warn("Failed to take snapshot", e);
		}

		return snapshot;
	}

	@SuppressWarnings("unchecked")
	public void restoreSnapshot(StateSnap snap) {
		setRestoring(true);

		try {
			arena.getBanned().clear();
			JSONArray banned = new JSONArray(IO.uncompress(snap.global().banned()));
			for (Object o : banned) {
				JSONObject jo = new JSONObject(o);
				Class<Drawable<?>> klass = (Class<Drawable<?>>) Class.forName(jo.getString("KLASS"));

				arena.getBanned().add(JSONUtils.fromJSON(String.valueOf(o), klass));
			}

			arena.setField(JSONUtils.fromJSON(IO.uncompress(snap.global().field()), Field.class));

			for (Map.Entry<Side, Hand> entry : hands.entrySet()) {
				Hand h = entry.getValue();
				Player p = snap.players().get(entry.getKey());

				h.getCards().clear();
				JSONArray cards = new JSONArray(IO.uncompress(p.cards()));
				for (Object o : cards) {
					JSONObject jo = new JSONObject(o);
					Class<Drawable<?>> klass = (Class<Drawable<?>>) Class.forName(jo.getString("KLASS"));

					h.getCards().add(JSONUtils.fromJSON(jo.toString(), klass));
				}

				h.getRealDeck().clear();
				JSONArray deck = new JSONArray(IO.uncompress(p.deck()));
				for (Object o : deck) {
					JSONObject jo = new JSONObject(o);
					Class<Drawable<?>> klass = (Class<Drawable<?>>) Class.forName(jo.getString("KLASS"));

					h.getRealDeck().add(JSONUtils.fromJSON(jo.toString(), klass));
				}

				h.getGraveyard().clear();
				JSONArray graveyard = new JSONArray(IO.uncompress(p.graveyard()));
				for (Object o : graveyard) {
					JSONObject jo = new JSONObject(o);
					Class<Drawable<?>> klass = (Class<Drawable<?>>) Class.forName(jo.getString("KLASS"));

					h.getGraveyard().add(JSONUtils.fromJSON(jo.toString(), klass));
				}
			}

			for (Map.Entry<Side, List<SlotColumn>> entry : getArena().getSlots().entrySet()) {
				List<SlotColumn> slts = entry.getValue();
				List<Slot> slots = snap.slots().get(entry.getKey());

				for (int i = 0; i < slts.size(); i++) {
					SlotColumn slt = slts.get(i);
					Slot slot = slots.get(i);

					slt.setState(slot.state());
					slt.setTop(JSONUtils.fromJSON(IO.uncompress(slot.top()), Senshi.class));
					if (slt.hasTop()) {
						JSONArray equips = new JSONArray(IO.uncompress(slot.equips()));
						for (Object o : equips) {
							JSONObject jo = new JSONObject(o);

							slt.getTop().getEquipments().add(JSONUtils.fromJSON(jo.toString(), Evogear.class));
						}
					}

					slt.setBottom(JSONUtils.fromJSON(IO.uncompress(slot.bottom()), Senshi.class));
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			Constants.LOGGER.warn("Failed to restore snapshot", e);
		} finally {
			setRestoring(false);
		}
	}

	public List<Senshi> getCards() {
		return Arrays.stream(Side.values()).flatMap(s -> getCards(s).stream()).toList();

	}

	public List<Senshi> getCards(Side side) {
		return getSlots(side).stream().flatMap(slt -> slt.getCards().stream()).filter(Objects::nonNull).toList();

	}

	public List<SlotColumn> getSlots() {
		return Arrays.stream(Side.values()).flatMap(s -> getSlots(s).stream()).toList();
	}

	public List<SlotColumn> getSlots(Side side) {
		return arena.getSlots(side);
	}

	public void iterateSlots(Consumer<Senshi> act) {
		for (Side side : Side.values()) {
			iterateSlots(side, act);
		}
	}

	public void iterateSlots(Side side, Consumer<Senshi> act) {
		Map<Senshi, Integer> cards = arena.getSlots(side).stream().flatMap(slt -> slt.getCards().stream()).filter(Objects::nonNull).collect(Collectors.toMap(Function.identity(), s -> s.getSlot().hashCode()));

		for (Map.Entry<Senshi, Integer> e : cards.entrySet()) {
			Senshi card = e.getKey();
			if (card.getSlot().hashCode() == e.getValue()) {
				act.accept(card);
			}
		}
	}

	public Senshi findCard(Side side, String id) {
		for (SlotColumn slt : getSlots(side)) {
			for (Senshi s : slt.getCards()) {
				if (s != null && s.getId().equalsIgnoreCase(id)) {
					return s;
				}
			}
		}

		return null;
	}

	public Senshi findCard(Side side, Predicate<Senshi> condition) {
		for (SlotColumn slt : getSlots(side)) {
			for (Senshi s : slt.getCards()) {
				if (s != null && condition.test(s)) {
					return s;
				}
			}
		}

		return null;
	}

	public BondedList<Drawable<?>> getBanned() {
		return arena.getBanned();
	}

	public List<Evogear> getEquipments(Side side) {
		return arena.getSlots(side).stream().map(SlotColumn::getTop).filter(Objects::nonNull).flatMap(s -> s.getEquipments().stream()).toList();
	}

	public void trigger(Trigger trigger) {
		if (isRestoring()) return;

		List<Side> sides = List.of(getCurrentSide(), getOtherSide());

		for (Side side : sides) {
			trigger(trigger, side);
		}

		if (trigger == ON_TICK) {
			tick++;
		}
	}

	public void trigger(Trigger trigger, Side side) {
		if (isRestoring()) return;

		EffectParameters ep = new EffectParameters(trigger, side);

		try {
			iterateSlots(side, s -> s.execute(true, new EffectParameters(trigger, side, s.asSource(trigger))));

			Hand h = hands.get(side);
			for (EffectHolder<?> leech : h.getLeeches()) {
				leech.execute(new EffectParameters(ON_LEECH, side, leech.asSource(trigger)));
			}

			for (Drawable<?> card : h.getCards()) {
				if (card instanceof EffectHolder<?> eh && eh.isPassive()) {
					eh.execute(ep);
				}
			}
		} finally {
			triggerEOTs(ep);
		}
	}

	public boolean trigger(Trigger trigger, Source source, Target... targets) {
		if (isRestoring()) return false;

		EffectParameters ep = new EffectParameters(trigger, source.side(), source, targets);

		try {
			boolean executed = source.execute(ep);
			for (Target t : ep.targets()) {
				t.execute(ep);
			}

			return executed;
		} finally {
			triggerEOTs(ep);
		}
	}

	public Set<EffectOverTime> getEOTs() {
		return eots;
	}

	public void triggerEOTs(EffectParameters ep) {
		for (TriggerBind binding : Set.copyOf(bindings)) {
			if (binding.isBound(ep)) {
				EffectHolder<?> holder = binding.getHolder();
				if ((holder instanceof Senshi s && s.getIndex() == -1) || (holder instanceof Evogear e && e.getEquipper() == null)) {
					bindings.remove(binding);
					continue;
				}

				holder.execute(new EffectParameters(ON_DEFER_BINDING, ep.side(), new DeferredTrigger(null, ep.trigger()), ep.source(), ep.targets()));
			}
		}

		for (EffectOverTime effect : Set.copyOf(eots)) {
			if (effect.isLocked()) continue;
			else if (effect.isClosed()) {
				eots.remove(effect);
				continue;
			}

			if (effect.getSide() != null) {
				Hand h = hands.get(effect.getSide());
				if (h.getLockTime(Lock.EFFECT) > 0) {
					if (!(effect.getSource() instanceof EffectHolder<?> e) || !e.hasTrueEffect()) {
						continue;
					}
				}
			}

			Predicate<Side> checkSide = s -> effect.getSide() == null || effect.getSide() == s;
			if (checkSide.test(getCurrentSide()) && ep.trigger() == ON_TURN_BEGIN) {
				effect.decreaseTurn();
			}

			if (ep.size() == 0) {
				if (checkSide.test(ep.side()) && effect.hasTrigger(ep.trigger())) {
					effect.decreaseLimit();

					try {
						effect.getEffect().accept(effect, new EffectParameters(ep.trigger(), ep.side()));
					} catch (ActivationException ignore) {
					} catch (Exception e) {
						getChannel().sendMessage(getString("error/effect")).queue();
						Constants.LOGGER.warn("Failed to execute {} persistent effect", effect.getSource(), e);
					}

					if (effect.getSide() == null) {
						effect.lock();
					}
				}
			} else if (ep.source() != null) {
				if (checkSide.test(ep.source().side()) && effect.hasTrigger(ep.source().trigger())) {
					effect.decreaseLimit();

					try {
						effect.getEffect().accept(effect, new EffectParameters(ep.source().trigger(), ep.side(), ep.source(), ep.targets()));
					} catch (ActivationException ignore) {
					} catch (Exception e) {
						getChannel().sendMessage(getString("error/effect")).queue();
						Constants.LOGGER.warn("Failed to execute {} persistent effect", effect.getSource(), e);
					}
				}

				for (Target t : ep.targets()) {
					if (checkSide.test(t.side()) && effect.hasTrigger(t.trigger())) {
						effect.decreaseLimit();

						try {
							effect.getEffect().accept(effect, new EffectParameters(t.trigger(), ep.side(), ep.source(), ep.targets()));
						} catch (ActivationException ignore) {
						} catch (Exception e) {
							getChannel().sendMessage(getString("error/effect")).queue();
							Constants.LOGGER.warn("Failed to execute {} persistent effect", effect.getSource(), e);
						}
					}
				}
			}

			if (effect.isExpired() || isClosed()) {
				effect.close();
				eots.remove(effect);

				if (!isClosed() && !effect.isPermanent() && effect.getSource() != null) {
					getChannel().sendMessage(getString("str/effect_expiration", effect.getSource())).queue();
				}
			}
		}
	}

	public void bind(EffectHolder<?> self, EnumMap<Side, EnumSet<Trigger>> binds) {
		bindings.add(new TriggerBind(self, binds));
	}

	public void bind(EffectHolder<?> self, EnumSet<Trigger> binds) {
		bindings.add(new TriggerBind(self, binds));
	}

	private BiFunction<String, String, String> replaceMessages(Message message) {
		addButtons(message);

		return (chn, msg) -> {
			if (msg != null) {
				GuildMessageChannel channel = Main.getApp().getMessageChannelById(chn);
				if (channel != null) {
					channel.retrieveMessageById(msg).flatMap(Objects::nonNull, Message::delete).queue(null, Utils::doNothing);
				}
			}

			return message.getId();
		};
	}

	public void reportEvent(String message, boolean trigger, Object... args) {
		if (isSending() || getChannel() == null) return;

		try {
			setSending(true);

			List<RestAction<?>> acts = new ArrayList<>();
			for (GuildMessageChannel chn : getChannel().getChannels()) {
				String msg = messages.get(chn.getId());
				if (msg != null) {
					acts.add(chn.retrieveMessageById(msg).flatMap(Objects::nonNull, Message::editMessageComponents));
				}
			}

			if (!acts.isEmpty()) {
				Pages.subGet(RestAction.allOf(acts));
			}

			if (trigger) {
				resetTimer();

				trigger(ON_TICK);
				getCurrent().setRerolled(true);
				getCurrent().verifyCap();
			}

			List<Side> sides = List.of(getOtherSide(), getCurrentSide());
			for (Side side : sides) {
				Hand hand = hands.get(side);
				hand.getCards();
				hand.getRealDeck();
				hand.getGraveyard();
				hand.resetChain();
				hand.getStats().removeExpired(ValueMod::isExpired);

				if (hand.getOrigins().synergy() == Race.SUCCUBUS && hand.isLowLife()) {
					Hand op = hand.getOther();
					if (op.getLockTime(Lock.CHARM) == 0) {
						op.modLockTime(Lock.CHARM, 1);
					}
				}

				String def = hand.getDefeat();
				if (hand.getHP() == 0 || def != null) {
					trigger(ON_VICTORY, side.getOther());
					trigger(ON_DEFEAT, side);

					if (hand.getOrigins().isPure(Race.UNDEAD) && !hand.getData().getBoolean("undying")) {
						hand.modHP(hand.getBase().hp() * 0.75, true);
						hand.getData().put("undying", true);
					}

					if (hand.getOrigins().synergy() == Race.GOLEM) {
						hand.modHP(hand.getMP() * hand.getBase().hp() / 10);
						hand.consumeMP(hand.getMP());
					}

					if (def == null && hand.getHP() > 0) {
						continue;
					}

					if (def != null) {
						reportResult(GameReport.SUCCESS, hand.getOther().getSide(), "str/game_end_special", def, "<@" + hand.getOther().getUid() + ">");
					} else {
						reportResult(GameReport.SUCCESS, hand.getOther().getSide(), "str/game_end", "<@" + hand.getUid() + ">", "<@" + hand.getOther().getUid() + ">");
					}

					return;
				}

				iterateSlots(side, s -> {
					s.getBase().unlockAll();
					s.setLastInteraction(null);
					s.getStats().removeIf(ValueMod::isExpired);
					for (Evogear e : s.getEquipments()) {
						e.getStats().removeIf(ValueMod::isExpired);
					}
				});
			}

			BufferedImage img = hasHistory() ? arena.render(getLocale(), getHistory()) : arena.render(getLocale());
			byte[] bytes = IO.getBytes(img, "png", 0.5f);

			AtomicBoolean registered = new AtomicBoolean();
			getChannel().sendMessage(getString(message, args)).addFile(bytes, "game.png").queue(m -> {
				messages.compute(m.getChannel().getId(), replaceMessages(m));

				if (!registered.get()) {
					if (!message.startsWith("str/game_history")) {
						getHistory().add(new HistoryLog(m.getContentDisplay(), getCurrentSide()));
					}

					registered.set(true);
				}
			});
		} finally {
			setSending(false);
		}
	}

	public void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, Side winner, String message, Object... args) {
		if (isClosed()) return;

		turns.add(Turn.from(this));

		setRestoring(true);
		for (List<SlotColumn> slts : arena.getSlots().values()) {
			for (SlotColumn slt : slts) {
				for (Senshi card : slt.getCards()) {
					if (card != null) {
						card.setFlipped(false);
					}
				}
			}
		}
		setRestoring(false);

		BufferedImage img = hasHistory() ? arena.render(getLocale(), getHistory()) : arena.render(getLocale());
		byte[] bytes = IO.getBytes(img, "png");

		AtomicBoolean registered = new AtomicBoolean();
		getChannel().sendMessage(getString(message, args)).addFile(bytes, "game.png").queue(m -> {
			if (!registered.get()) {
				getHistory().add(new HistoryLog(m.getContentDisplay(), getCurrentSide()));
				registered.set(true);
			}
		}, Utils::doNothing);

		List<RestAction<?>> acts = new ArrayList<>();
		for (Map.Entry<String, String> tuple : messages.entrySet()) {
			if (tuple != null) {
				GuildMessageChannel channel = Main.getApp().getMessageChannelById(tuple.getKey());
				if (channel != null) {
					acts.add(channel.retrieveMessageById(tuple.getValue()).flatMap(Objects::nonNull, Message::delete));
				}
			}
		}

		if (!acts.isEmpty()) {
			Pages.subGet(RestAction.allOf(acts));
		}

		if (winner != null) {
			this.winner = winner;
		}

		if (!isSingleplayer() && arcade == null && !hasCheated() && code == GameReport.SUCCESS) {
			Match m = new Match(this, message.equals("str/game_end") ? "default" : String.valueOf(args[0]));
			new MatchHistory(m).save();
		}

		close(code);
	}

	private void addButtons(Message msg) {
		Hand curr = getCurrent();
		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		buttons.put(Utils.parseEmoji("▶"), w -> {
			if (isLocked()) return;

			if (curr.selectionPending()) {
				getChannel().sendMessage(getString("error/pending_choice")).queue();
				return;
			} else if (curr.selectionPending()) {
				getChannel().sendMessage(getString("error/pending_action")).queue();
				return;
			} else if (getPhase() == Phase.COMBAT || !curr.canAttack()) {
				if (curr.getLockTime(Lock.TAUNT) > 0) {
					List<SlotColumn> yours = getSlots(curr.getSide());
					if (yours.stream().anyMatch(sc -> sc.getTop() != null && sc.getTop().canAttack())) {
						getChannel().sendMessage(getString("error/taunt_locked", false, curr.getLockTime(Lock.TAUNT))).queue();
						return;
					}
				}

				nextTurn();
				return;
			}

			setPhase(Phase.COMBAT);
			reportEvent("str/game_combat_phase", true, true);
		});

		if (getPhase() == Phase.PLAN) {
			if (getTurn() > 1) {
				buttons.put(Utils.parseEmoji("⏩"), w -> {
					if (isLocked()) return;

					if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_choice")).queue();
						return;
					} else if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_action")).queue();
						return;
					} else if (curr.getLockTime(Lock.TAUNT) > 0) {
						List<SlotColumn> yours = getSlots(curr.getSide());
						if (yours.stream().anyMatch(sc -> sc.getTop() != null && sc.getTop().canAttack())) {
							getChannel().sendMessage(getString("error/taunt_locked", false, curr.getLockTime(Lock.TAUNT))).queue();
							return;
						}
					}

					nextTurn();
				});
			}

			if (!curr.getCards().isEmpty() && (getTurn() == 1 && !curr.hasRerolled()) || curr.getOrigins().synergy() == Race.DJINN) {
				buttons.put(Utils.parseEmoji("\uD83D\uDD04"), w -> {
					if (isLocked()) return;

					curr.rerollHand();
					reportEvent("str/hand_reroll", true, curr.getName());
				});
			}

			if (!curr.getRealDeck().isEmpty() && arcade != Arcade.DECK_ROYALE) {
				int rem = curr.getRemainingDraws();
				if (rem > 0) {
					buttons.put(Utils.parseEmoji("📤"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						curr.manualDraw();
						Objects.requireNonNull(w.getHook())
								.setEphemeral(true)
								.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png")).queue();

						reportEvent("str/draw_card", true, curr.getName(), 1, "");
					});

					if (rem > 1) {
						buttons.put(Utils.parseEmoji("📦"), w -> {
							if (isLocked()) return;

							if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_choice")).queue();
								return;
							} else if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_action")).queue();
								return;
							}

							curr.manualDraw(curr.getRemainingDraws());
							Objects.requireNonNull(w.getHook())
									.setEphemeral(true)
									.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png")).queue();

							reportEvent("str/draw_card", true, curr.getName(), rem, "s");
						});
					}
				} else if (curr.getOrigins().major() == Race.DIVINITY) {
					buttons.put(Utils.parseEmoji("1212407741046325308"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						Drawable<?> d = curr.manualDraw();
						d.setEthereal(true);
						curr.getRegDeg().add(-curr.getBase().hp() / 20);
						Objects.requireNonNull(w.getHook())
								.setEphemeral(true)
								.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png")).queue();

						reportEvent("str/draw_card", true, curr.getName(), 1, "");
					});
				}

				if (curr.canUseDestiny() && !Utils.equalsAny(curr.getOrigins().major(), Race.MACHINE, Race.MYSTICAL)) {
					buttons.put(Utils.parseEmoji("\uD83E\uDDE7"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						BondedList<Drawable<?>> deque = curr.getRealDeck();
						List<SelectionCard> cards = new ArrayList<>();
						cards.add(new SelectionCard(deque.getFirst(), false));
						if (deque.size() > 2) {
							cards.add(new SelectionCard(deque.get((deque.size() - 1) / 2), false));
						}
						if (deque.size() > 1) {
							cards.add(new SelectionCard(deque.getLast(), false));
						}

						try {
							curr.requestChoice(null, cards, ds -> {
								curr.draw(ds.getFirst());
								curr.setUsedDestiny(true);
								Objects.requireNonNull(w.getHook())
										.setEphemeral(true)
										.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png")).queue();

								reportEvent("str/destiny_draw", true, curr.getName());
							});
						} catch (ActivationException e) {
							getChannel().sendMessage(getString(e.getMessage())).queue();
						}
					});
				}
			}

			if (curr.canUseDestiny() && Utils.equalsAny(curr.getOrigins().major(), Race.MACHINE, Race.MYSTICAL)) {
				buttons.put(Utils.parseEmoji("⚡"), w -> {
					if (isLocked()) return;

					if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_choice")).queue();
						return;
					} else if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_action")).queue();
						return;
					}

					List<SelectionCard> valid = curr.getCards().stream().filter(d -> {
						if (d instanceof Evogear e && e.isAvailable()) {
							return e.isSpell() == (curr.getOrigins().major() == Race.MYSTICAL);
						}

						return false;
					}).map(d -> new SelectionCard(d, false)).toList();

					if (valid.isEmpty()) {
						getChannel().sendMessage(getString("err/empty_selection")).queue();
						return;
					}

					try {
						curr.requestChoice(null, valid, ds -> {
							((Evogear) ds.getFirst()).setFlag(Flag.EMPOWERED);
							curr.setUsedDestiny(true);

							if (curr.getOrigins().major() == Race.MACHINE) {
								reportEvent("str/martial_empower", true, curr.getName());
							} else {
								reportEvent("str/arcane_empower", true, curr.getName());
							}
						});
					} catch (ActivationException e) {
						getChannel().sendMessage(getString(e.getMessage())).queue();
					}
				});
			}

			if (curr.getOriginCooldown() == 0) {
				if (curr.getOrigins().major() == Race.SPIRIT) {
					List<SelectionCard> valid = curr.getCards().stream()
							.filter(Drawable::isAvailable).map(d -> new SelectionCard(d, false))
							.toList();

					if (valid.size() >= 5) {
						buttons.put(Utils.parseEmoji("\uD83C\uDF00"), w -> {
							if (isLocked()) return;

							if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_choice")).queue();
								return;
							} else if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_action")).queue();
								return;
							}

							try {
								curr.requestChoice(null, valid, 5, ds -> {
									List<StashedCard> material = ds.stream().map(d -> new StashedCard(null, d)).toList();

									List<SelectionCard> pool = new ArrayList<>();
									for (int j = 0; j < 3; j++) {
										pool.add(new SelectionCard(SynthesizeCommand.rollSynthesis(curr.getUser(), material), false));
									}

									try {
										curr.requestChoice(null, pool, chosen -> {
											arena.getBanned().addAll(ds);
											curr.getCards().removeAll(ds);

											Evogear d = (Evogear) chosen.getFirst();
											if (curr.getOrigins().isPure()) {
												d.setFlag(Flag.EMPOWERED);
											}

											curr.getCards().add(d);
											if (d.getTier() == 4 && d.hasFlag(Flag.EMPOWERED)) {
												curr.getAccount().setDynValue("emp_tier_4", true);
											}

											curr.setOriginCooldown(3);
											Objects.requireNonNull(w.getHook())
													.setEphemeral(true)
													.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png")).queue();

											reportEvent("str/spirit_synth", true, curr.getName());
										});
									} catch (ActivationException e) {
										getChannel().sendMessage(getString(e.getMessage())).queue();
									}
								});
							} catch (ActivationException e) {
								getChannel().sendMessage(getString(e.getMessage())).queue();
							}
						});
					}
				}
			}

			if (curr.getOrigins().synergy() == Race.ORACLE) {
				buttons.put(Utils.parseEmoji("\uD83D\uDD2E"), w -> {
					if (isLocked()) return;

					BufferedImage cards = curr.render(curr.getDeck().subList(0, Math.min(3, curr.getDeck().size())));
					Objects.requireNonNull(w.getHook())
							.setEphemeral(true)
							.sendFiles(FileUpload.fromData(IO.getBytes(cards, "png"), "hand.png")).queue();
				});
			}

			buttons.put(Utils.parseEmoji("\uD83D\uDCD1"), w -> {
				if (isLocked()) return;

				setHistory(!hasHistory());

				if (hasHistory()) {
					reportEvent("str/game_history_enable", false, curr.getName());
				} else {
					reportEvent("str/game_history_disable", false, curr.getName());
				}
			});

			buttons.put(Utils.parseEmoji("\uD83E\uDEAA"), w -> {
				if (isLocked()) return;

				if (curr.selectionPending()) {
					BufferedImage bi = curr.renderChoices();
					if (bi == null) return;

					Objects.requireNonNull(w.getHook())
							.setEphemeral(true)
							.sendFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "choices.png")).queue();

					return;
				}

				Objects.requireNonNull(w.getHook()).setEphemeral(true).sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "hand.png")).queue();
			});

			buttons.put(Utils.parseEmoji("\uD83D\uDD0D"), w -> {
				if (isLocked()) return;

				Objects.requireNonNull(w.getHook())
						.setEphemeral(true)
						.sendFiles(FileUpload.fromData(IO.getBytes(arena.renderEvogears(), "png"), "evogears.png")).queue();
			});

			if (isSingleplayer() || getTurn() > 10) {
				if (isLocked()) return;

				buttons.put(Utils.parseEmoji("🏳"), w -> {
					if (curr.isForfeit()) {
						reportResult(GameReport.SUCCESS, getOther().getSide(), "str/game_forfeit", "<@" + getCurrent().getUid() + ">");
						return;
					}

					curr.setForfeit(true);
					Objects.requireNonNull(w.getHook()).setEphemeral(true).sendMessage(getString("str/confirm_forfeit")).queue();
				});
			}
		}

		Pages.buttonize(msg, buttons, true, false, u -> u.getId().equals(curr.getUid()));
	}

	public List<SlotColumn> getOpenSlots(Side side, boolean top) {
		return getSlots(side).stream().filter(sc -> !sc.isLocked() && !(top ? sc.hasTop() : sc.hasBottom())).toList();
	}

	public boolean putAtOpenSlot(Side side, boolean top, Senshi card) {
		List<SlotColumn> slts = getOpenSlots(side, top);
		if (slts.isEmpty()) return false;

		if (top) {
			slts.getFirst().setTop(card);
		} else {
			slts.getFirst().setBottom(card);
		}

		return true;
	}

	public boolean putAtOpenSlot(Side side, Senshi card) {
		return putAtOpenSlot(side, true, card) || putAtOpenSlot(side, false, card);
	}

	public String getString(String key, Object... args) {
		try {
			String out = super.getString(key, args);
			if (out.isBlank() || out.equalsIgnoreCase(key)) {
				out = LocalizedString.get(getLocale(), key, "").formatted(args);
			}

			return Utils.getOr(out, key);
		} catch (MissingFormatArgumentException e) {
			return "";
		}
	}

	public void send(Drawable<?> source, String text) {
		send(source, text, null);
	}

	public void send(Drawable<?> source, String text, String gif) {
		if (text.isBlank() && gif == null) return;

		for (GuildMessageChannel chn : getChannel().getChannels()) {
			PseudoUser pu = new PseudoUser(source.toString(), Constants.API_ROOT + "card/" + source.getCard().getId(), chn);
			if (gif != null) {
				pu.send(null, text, new WebhookEmbedBuilder().setImageUrl(GIF_PATH + gif + ".gif").build());
			} else {
				pu.send(null, text);
			}
		}
	}

	@Override
	public void nextTurn() {
		turns.add(Turn.from(this));

		Hand curr = getCurrent();
		if (getOther().getOrigins().synergy() == Race.ELEMENTAL) {
			int fire = (int) getCards(getOther().getSide()).parallelStream()
					.filter(s -> s.getElement() == ElementType.FIRE)
					.count();

			if (fire >= 4) {
				try {
					curr.requestDiscard(1).get();
				} catch (Exception e) {
					curr.getRegDeg().add(-300);
					getChannel().sendMessage(getString("str/fire_burn")).queue();
				}
			}
		}

		curr.flushDiscard();
		trigger(ON_TURN_END, curr.getSide());

		if (arcade == Arcade.DECK_ROYALE) {
			boolean noHand = curr.getCards().stream().noneMatch(d -> d instanceof Senshi);
			boolean noField = getSlots(curr.getSide()).stream().flatMap(sc -> sc.getCards().stream()).noneMatch(Objects::nonNull);

			if (noHand && noField) {
				reportResult(GameReport.SUCCESS, getOther().getSide(), "arcade/deck_royale_win", "<@" + curr.getUid() + ">", "<@" + curr.getOther().getUid() + ">");
				return;
			}
		}

		if (curr.getOrigins().synergy() == Race.ANGEL) {
			curr.getRegDeg().add(curr.getMP() * 100);
		}

		for (Lock lock : Lock.values()) {
			curr.modLockTime(lock, -1);
		}

		for (SlotColumn slt : getSlots(curr.getSide())) {
			slt.reduceLock(1);

			for (Senshi s : slt.getCards()) {
				if (s != null && s.getSlot().getIndex() != -1) {
					s.reduceDebuffs(1);
					s.setAvailable(true);
					s.setSwitched(false);

					s.clearBlocked();
					s.getStats().getFlags().clearTemp();
					for (Evogear e : s.getEquipments()) {
						e.getStats().getFlags().clearTemp();
					}

					if (arcade == Arcade.DECAY) {
						s.getStats().getMana().set(-1);
						if (s.getMPCost() == 0) {
							s.getHand().getGraveyard().add(s);
						}
					}
				}
			}
		}

		if (arcade == Arcade.INSTABILITY) {
			int affected = Math.min((int) Math.ceil(getTurn() / 2d), 8);
			List<SlotColumn> chosen = Utils.getRandomN(Utils.flatten(arena.getSlots().values()), affected, 1);

			for (SlotColumn slt : chosen) {
				slt.setLock(1);
			}
		}

		super.nextTurn();
		setPhase(Phase.PLAN);
		curr = getCurrent();
		curr.modMP(curr.getBase().mpGain().get());
		curr.applyVoTs();
		curr.reduceOriginCooldown(1);
		curr.setCanAttack(true);
		curr.flushDiscard();
		curr.setSummoned(false);

		if (curr.getOrigins().synergy() == Race.WRAITH) {
			curr.getOther().modHP((int) -(curr.getGraveyard().size() * Math.ceil(getTurn() / 2d)));
		}

		curr.getStats().expireMods();

		if (curr.getLockTime(Lock.BLIND) > 0) {
			Utils.shuffle(curr.getCards());
		}

		List<Senshi> allCards = getCards();
		for (SlotColumn slt : getSlots(curr.getSide())) {
			for (Senshi s : slt.getCards()) {
				if (s != null && s.getSlot().getIndex() != -1) {
					s.reduceCooldown(1);
					s.reduceStasis(1);

					s.getStats().expireMods();
					for (Evogear e : s.getEquipments()) {
						e.getStats().expireMods();
					}

					if (s.isBerserk()) {
						List<Senshi> valid = allCards.stream().filter(d -> !d.equals(s)).toList();
						if (!valid.isEmpty()) {
							attack(s, Utils.getRandomEntry(getRng(), valid), null, true);
							s.setAvailable(false);
						}
					}
				}
			}
		}

		trigger(ON_TURN_BEGIN, curr.getSide());
		reportEvent("str/game_turn_change", true, "<@" + curr.getUid() + ">", (int) Math.ceil(getTurn() / 2d));

		takeSnapshot();
	}

	public Side getWinner() {
		return winner;
	}

	@Override
	protected void resetTimer() {
		super.resetTimer();
		getCurrent().setForfeit(false);

		for (EffectOverTime eot : eots) {
			eot.unlock();
		}
	}

	@Override
	public void setPhase(Phase phase) {
		super.setPhase(phase);
		Trigger trigger = switch (phase) {
			case PLAN -> ON_PLAN;
			case COMBAT -> ON_COMBAT;
		};

		trigger(trigger, getCurrentSide());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Shoukan shoukan = (Shoukan) o;
		return getSeed() == shoukan.getSeed() && arcade == shoukan.arcade && isSingleplayer() == shoukan.isSingleplayer();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSeed(), arcade, isSingleplayer());
	}
}
