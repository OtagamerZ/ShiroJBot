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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.persistent.GiftCode;

import javax.persistence.EntityManager;

public class GiftCodeDAO {
	public static GiftCode redeemGiftCode(String id, String code) {
		EntityManager em = Manager.getEntityManager();

		GiftCode gc = em.find(GiftCode.class, code);
		if (gc == null || !gc.getRedeemedBy().isBlank()) return null;

		gc.setRedeemedBy(id);
		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();

		return gc;
	}
}