package com.kuuhaku.game;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.util.Utils;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Dunhun extends GameInstance<NullPhase> {
	private final Dungeon instance;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private Pair<String, String> message = null;

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

		setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, "str/dungeon_leave"
				, Utils.properlyJoin(locale.get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
				, getTurn()
		), 1 /* TODO Revert to 5 */, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return false;
	}

	@Override
	protected void begin() {
		Combat c = new Combat(this);
	}

	@Override
	protected void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException {

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
