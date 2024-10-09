package com.kuuhaku.game;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.records.dunhun.EventAction;
import com.kuuhaku.model.records.dunhun.EventDescription;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Dunhun extends GameInstance<NullPhase> {
	private final ExecutorService main = Executors.newSingleThreadExecutor();
	private final Dungeon instance;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private final AtomicReference<Combat> combat = new AtomicReference<>();
	private CompletableFuture<Void> lock;
	private Pair<String, String> message;

	public Dunhun(I18N locale, Dungeon instance, User... players) {
		this(locale, instance, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon instance, String... players) {
		super(locale, players);

		this.instance = instance;
		for (String p : players) {
			Hero h = DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1", p);
			if (h == null) {
				getChannel().sendMessage(getString("error/no_hero_target", "<@" + p + ">")).queue();
				close(GameReport.NO_HERO);
				return;
			}

			h.asSenshi(locale);
			heroes.put(p, h);
		}

		setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, "str/dungeon_leave"
				, Utils.properlyJoin(locale.get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
				, getTurn()
		), 5, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return true;
	}

	@Override
	protected void begin() {
		CompletableFuture.runAsync(() -> {
			while (true) {
				if (getCombat() != null) {
					getCombat().process();
				} else {
					if (Calc.chance(25)) {
						runEvent();
					} else {
						runCombat();
					}
				}

				try {
					if (lock != null) lock.get();
				} catch (ExecutionException | InterruptedException ignore) {
				}

				if (heroes.values().stream().allMatch(h -> h.getHp() <= 0 || h.hasFleed())) {
					reportResult(GameReport.GAME_TIMEOUT, "str/dungeon_fail"
							, Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
							, getTurn()
					);
					break;
				} else {
					nextTurn();
					getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_floor", getTurn()))).queue();
				}
			}
		}, main);
	}

	private void runCombat() {
		combat.set(new Combat(this));
		for (int i = 0; i < 4; i++) {
			List<Actor> keepers = getCombat().getActors(Team.KEEPERS);
			if (!Calc.chance(100 - 50d / getPlayers().length * keepers.size())) break;

			keepers.add(Monster.getRandom());
		}

		getCombat().process();
		combat.set(null);
	}

	private void runEvent() {
		lock = new CompletableFuture<>();

		Event evt = Event.getRandom();
		EventDescription ed = evt.parse(getLocale(), this);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription(ed.description());

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
				.setCancellable(false);

		Map<String, String> votes = new HashMap<>();
		for (EventAction act : ed.actions()) {
			helper.addAction(act.label(), w -> {
				if (votes.containsKey(w.getUser().getId())) return;
				votes.put(w.getUser().getId(), act.action());

				getChannel().sendMessage(getLocale().get("str/actor_chose",
						heroes.get(w.getUser().getId()).getName(), act.label())
				).queue();

				if (votes.size() >= heroes.size()) {
					eb.setDescription(Utils.getOr(evt.getAction(Utils.getRandomEntry(votes.values())).get(), "PLACEHOLDER"));

					ButtonizeHelper fin = new ButtonizeHelper(true)
							.setTimeout(5, TimeUnit.MINUTES)
							.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
							.setCancellable(false)
							.addAction(getLocale().get("str/continue"), s -> {
								lock.complete(null);
								Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
							});

					fin.apply(w.getMessage().editMessageEmbeds(eb.build()))
							.queue(s -> Pages.buttonize(s, fin));
				}
			});
		}

		getChannel().sendEmbed(eb.build())
				.apply(helper::apply)
				.queue(s -> Pages.buttonize(s, helper));
	}

	@Override
	protected void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException {
		Pair<Method, JSONObject> action = toAction(StringUtils.stripAccents(value).toLowerCase());
		if (action != null) {
			action.getFirst().invoke(this, action.getSecond());
		}
	}

	@PlayerAction("reload")
	private void reload(JSONObject args) {
		if (getCombat() != null) getCombat().getLock().complete(null);
	}

	@PlayerAction("info")
	private void info(JSONObject args) {
		if (getCombat() == null) return;

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Actor a : getCombat().getActors()) {
			if (!(a instanceof Monster m)) continue;

			XStringBuilder sb = new XStringBuilder("-# " + m.getInfo(getLocale()).getName() + "\n");

			Set<Affix> affs = m.getAffixes();
			if (!affs.isEmpty()) {
				sb.appendNewLine("**" + getLocale().get("str/affixes") + "**");
				for (Affix aff : affs) {
					sb.appendNewLine("- " + aff.getInfo(getLocale()).getDescription());
				}
			}

			sb.nextLine();

			List<Skill> skills = m.getSkills();
			if (!skills.isEmpty()) {
				sb.appendNewLine("**" + getLocale().get("str/skills") + "**");
				for (Skill skill : skills) {
					sb.appendNewLine("- " + skill.getInfo(getLocale()).getName());
					sb.appendNewLine("-# " + skill.getDescription(getLocale()));
					sb.nextLine();
				}
			}

			eb.addField(a.getName(getLocale()), sb.toString(), true);
		}

		getChannel().sendEmbed(eb.build()).queue();
	}

	private void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		getChannel().sendMessage(parsePlural(getString(msg, args)))
				.queue(m -> {
					if (message != null) {
						GuildMessageChannel channel = Main.getApp().getMessageChannelById(message.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(message.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue(null, Utils::doNothing);
						}
					}
				}, Utils::doNothing);

		close(code);
	}

	public Dungeon getInstance() {
		return instance;
	}

	public Map<String, Hero> getHeroes() {
		return heroes;
	}

	public Pair<String, String> getMessage() {
		return message;
	}

	public void setMessage(Pair<String, String> message) {
		this.message = message;
	}

	public Combat getCombat() {
		return combat.get();
	}

	public void beginCombat(Monster... enemies) {
		if (this.combat.get() != null) return;
		this.combat.set(new Combat(this, enemies));
	}

	public String parsePlural(String text) {
		String plural = heroes.size() == 1 ? "S" : "P";

		return Utils.regex(getString(text), "\\[(?<S>.*?)\\|(?<P>.*?)]")
				.replaceAll(r -> r.group(plural));
	}
}
