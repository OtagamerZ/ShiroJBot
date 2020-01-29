/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller.mysql;

import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.Member;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import java.util.List;

public class WaifuDAO {
	public static void saveMemberWaifu(Member m, User u) {
		EntityManager em = Manager.getEntityManager();

		List<Member> mbs = MemberDAO.getMemberByMid(m.getMid());

		mbs.forEach(mb -> mb.marry(u));

		em.getTransaction().begin();
		mbs.forEach(em::merge);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMemberWaifu(Member m) {
		EntityManager em = Manager.getEntityManager();

		List<Member> mbs = MemberDAO.getMemberByMid(m.getMid());

		mbs.forEach(Member::divorce);

		em.getTransaction().begin();
		mbs.forEach(em::merge);
		em.getTransaction().commit();

		em.close();
	}
}
