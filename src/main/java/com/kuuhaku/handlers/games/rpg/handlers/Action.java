package com.kuuhaku.handlers.games.rpg.handlers;

public class Action {
	private boolean attacking = false;
	private boolean defending = false;
	private boolean lookingBag = false;
	private boolean fleeing = false;

	boolean isAttacking() {
		return attacking;
	}

	void setAttacking() {
		this.attacking = true;
	}

	boolean isDefending() {
		return defending;
	}

	void setDefending() {
		this.defending = true;
	}

	boolean isLookingBag() {
		return lookingBag;
	}

	void setLookingBag(boolean lookingBag) {
		this.lookingBag = lookingBag;
	}

	boolean isFleeing() {
		return fleeing;
	}

	void setFleeing() {
		this.fleeing = true;
	}
}
