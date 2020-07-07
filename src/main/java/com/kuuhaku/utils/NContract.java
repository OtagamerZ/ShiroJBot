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

package com.kuuhaku.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NContract<A> {
	private final int signers;
	private Consumer<List<A>> action = null;
	private final List<A> signatures = new ArrayList<>();

	public NContract(int signers, Consumer<List<A>> action) {
		this.signers = signers;
		this.action = action;
	}

	public NContract(int signers) {
		this.signers = signers;
	}

	public Consumer<List<A>> getAction() {
		return action;
	}

	public void setAction(Consumer<List<A>> action) {
		this.action = action;
	}

	public List<A> getSignatures() {
		return signatures;
	}

	public void addSignature(A signature) {
		this.signatures.add(signature);
		checkContract();
	}

	private void checkContract() {
		if (this.signatures.size() == signers) {
			action.accept(signatures);
			signatures.clear();
		}
	}
}
