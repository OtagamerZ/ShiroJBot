package com.kuuhaku.game;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dunhun extends GameInstance<NullPhase> {
	private final ExecutorService main = Executors.newSingleThreadExecutor();
	private final Dungeon instance;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private Pair<String, String> message;
	private Combat combat;

	public Dunhun(I18N locale, Dungeon instance, User... players) {
		this(locale, instance, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon instance, String... players) {
		super(locale, players);

		this.instance = instance;
		for (String p : players) {
			Hero h = DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1", p);
			if (h == null) {
				getChannel().sendMessage(getString("error/no_hero_other", "<@" + p + ">")).queue();
				close(GameReport.NO_HERO);
				return;
			}

			heroes.put(p, h);
		}

		setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, players.length > 1 ? "str/dungeon_leave_multi" : "str/dungeon_leave"
				, Utils.properlyJoin(locale.get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
				, getTurn()
		), 1 /* TODO Revert to 5 */, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return true;
	}

	@Override
	protected void begin() {
		CompletableFuture.runAsync(() -> {
			while (true) {
				combat = new Combat(this);
				if (!combat.isWin()) {
					reportResult(GameReport.GAME_TIMEOUT, getPlayers().length > 1 ? "str/dungeon_leave_multi" : "str/dungeon_leave"
							, Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
							, getTurn()
					);
					break;
				}

				nextTurn();
			}
		}, main);
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
		if (combat != null) combat.getLock().complete(null);
	}

	private void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		getChannel().sendMessage(getString(msg, args))
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
}
