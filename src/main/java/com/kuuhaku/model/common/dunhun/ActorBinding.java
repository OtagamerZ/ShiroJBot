package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.dunhun.Team;

public class ActorBinding {
	private Dunhun game;
	private Team team;

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
