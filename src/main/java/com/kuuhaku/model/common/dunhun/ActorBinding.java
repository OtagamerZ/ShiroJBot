package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.util.Utils;

public class ActorBinding {
	private I18N locale;
	private Dunhun game;
	private Team team;

	public void setLocale(I18N locale) {
		this.locale = locale;
	}

	public I18N getLocale() {
		if (game != null) {
			return game.getLocale();
		}

		return Utils.getOr(locale, I18N.EN);
	}

	public Dunhun getGame() {
		return game;
	}

	public Team getTeam() {
		return team;
	}

	public boolean isBound() {
		return game != null && team != null;
	}

	public void bind(ActorBinding binding) {
		game = binding.game;
		team = binding.team;
	}

	public void bind(Dunhun game, Team team) {
		this.game = game;
		this.team = team;
	}

	public void unbind() {
		game = null;
		team = null;
	}
}
