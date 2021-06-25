/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import java.util.function.BiConsumer;

public class BiContract<A, B> {
	private BiConsumer<A, B> action = null;
	private A signatureA = null;
	private B signatureB = null;

	public BiContract(BiConsumer<A, B> action) {
		this.action = action;
	}

	public BiContract() {
	}

	public BiConsumer<A, B> getAction() {
		return action;
	}

	public void setAction(BiConsumer<A, B> action) {
		this.action = action;
	}

	public A getSignatureA() {
		return signatureA;
	}

	public void setSignatureA(A signatureA) {
		this.signatureA = signatureA;
		checkContract();
	}

	public B getSignatureB() {
		return signatureB;
	}

	public void setSignatureB(B signatureB) {
		this.signatureB = signatureB;
		checkContract();
	}

	private void checkContract() {
		if (this.signatureA != null && signatureB != null) {
			action.accept(signatureA, signatureB);
			signatureA = null;
			signatureB = null;
		}
	}
}
