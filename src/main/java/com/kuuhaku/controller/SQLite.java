/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.File;
import java.util.*;

public class SQLite {

	private static EntityManagerFactory emf;

	public static void connect() {

		File DBfile = new File(Main.getInfo().getDBFileName());
		if (!DBfile.exists()) {
			Helper.log(SQLite.class, LogLevel.FATAL, "A base de dados não foi encontrada. Entre no servidor discord oficial da Shiro para obter ajuda.");
			System.exit(1);
		}

		Map<String, String> props = new HashMap<>();
		props.put("javax.persistence.jdbc.url", "jdbc:sqlite:" + DBfile.getPath());

		if (emf == null) emf = Persistence.createEntityManagerFactory("shiro_local", props);

		emf.getCache().evictAll();
	}

	static EntityManager getEntityManager() {
		if (emf == null) connect();
		return emf.createEntityManager();
	}

	public static void disconnect() {
		if (emf != null) {
			emf.close();
			Helper.log(SQLite.class, LogLevel.INFO, "Ligação à base de dados desfeita.");
		}
	}

	public static boolean restoreData(DataDump data) {
		EntityManager em = getEntityManager();

		try {
			em.getTransaction().begin();
			data.getCaDump().forEach(em::merge);
			data.getmDump().forEach(em::merge);
			data.getGcDump().forEach(em::merge);
			em.getTransaction().commit();

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswers> getCADump() {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		List<CustomAnswers> ca = q.getResultList();
		ca.removeIf(CustomAnswers::isMarkForDelete);

		return ca;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberDump() {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);

		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<guildConfig> getGuildDump() {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		return q.getResultList();
	}

	public static guildConfig getGuildById(String id) {
		EntityManager em = getEntityManager();
		guildConfig gc;

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		gc = (guildConfig) q.getSingleResult();

		em.close();

		return gc;
	}

	public static void addGuildToDB(Guild guild) {
		EntityManager em = getEntityManager();

		guildConfig gc = new guildConfig();
		gc.setName(guild.getName());
		gc.setGuildId(guild.getId());

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeGuildFromDB(guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static CustomAnswers getCAByTrigger(String trigger, String guild) {
		EntityManager em = getEntityManager();
		List<CustomAnswers> ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE LOWER(gatilho) LIKE ?1 AND guildID = ?2", CustomAnswers.class);
		q.setParameter(1, trigger.toLowerCase());
		q.setParameter(2, guild);
		ca = (List<CustomAnswers>) q.getResultList();

		em.close();

		return ca.size() > 0 ? ca.get(Helper.rng(ca.size())) : null;
	}

	public static CustomAnswers getCAByID(Long id) {
		EntityManager em = getEntityManager();
		CustomAnswers ca;

		Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE id = ?1", CustomAnswers.class);
		q.setParameter(1, id);
		ca = (CustomAnswers) q.getSingleResult();

		em.close();

		return ca;
	}

	public static void addCAtoDB(Guild g, String trigger, String answer) {
		EntityManager em = getEntityManager();

		CustomAnswers ca = new CustomAnswers();
		ca.setGuildID(g.getId());
		ca.setGatilho(trigger);
		ca.setAnswer(answer);

		em.getTransaction().begin();
		em.merge(ca);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeCAFromDB(CustomAnswers ca) {
		EntityManager em = getEntityManager();

		ca.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(ca);
		em.getTransaction().commit();

		em.close();
	}

	public static Member getMemberById(String id) {
		EntityManager em = getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE id = ?1", Member.class);
		q.setParameter(1, id);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	public static void addMemberToDB(net.dv8tion.jda.core.entities.Member u) {
		EntityManager em = getEntityManager();

		Member m = new Member();
		m.setId(u.getUser().getId() + u.getGuild().getId());
		m.setMid(u.getUser().getId());

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveMemberToDB(Member m) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveMemberMid(Member m, User u) {
		EntityManager em = getEntityManager();

		m.setMid(u.getId());

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveMemberWaifu(Member m, User u) {
		EntityManager em = getEntityManager();

		m.setWaifu(u);

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMemberFromDB(Member m) {
		EntityManager em = getEntityManager();

		m.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberRank(String gid, boolean global) {
		EntityManager em = getEntityManager();

		Query q;

		if (global) q = em.createQuery("SELECT m FROM Member m WHERE m.mid IS NOT NULL ORDER BY m.level DESC", Member.class);
		else {
			q = em.createQuery("SELECT m FROM Member m WHERE id LIKE ?1 AND m.mid IS NOT NULL ORDER BY m.level DESC", Member.class);
			q.setParameter(1, "%" + gid);
		}

		List<Member> mbs = (List<Member>) q.getResultList();

		em.close();

		return mbs;
	}

	// --- guildConfig -- \\
	public static void updateGuildName(String newName, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setName(newName);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildPrefix(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		String prefix = gc.getPrefix();

		if (prefix == null) prefix = "s!";
		em.close();

		return prefix;
	}

	public static void updateGuildPrefix(String newPrefix, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setPrefix(newPrefix);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalBV(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalBV = gc.getCanalBV();
		if (canalBV == null) canalBV = "Não definido.";

		return canalBV;
	}

	public static void updateGuildCanalBV(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalBV(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildMsgBV(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String msgBV = gc.getMsgBoasVindas();
		if (msgBV == null) msgBV = "Não definido.";

		return msgBV;
	}

	public static void updateGuildMsgBV(String newMsgBV, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setMsgBoasVindas(newMsgBV);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalAdeus(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalAdeus = gc.getCanalAdeus();
		if (canalAdeus == null) canalAdeus = "Não definido.";

		return canalAdeus;
	}

	public static void updateGuildCanalAdeus(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalAdeus(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildMsgAdeus(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String msgAdeus = gc.getMsgAdeus();
		if (msgAdeus == null) msgAdeus = "Não definido.";

		return msgAdeus;
	}

	public static void updateGuildMsgAdeus(String newMsgAdeus, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setMsgAdeus(newMsgAdeus);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalSUG(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalSUG = gc.getCanalSUG();
		if (canalSUG == null) canalSUG = "Não definido.";

		return canalSUG;
	}

	public static void updateGuildCanalSUG(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalSUG(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalAvisos(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalAvisos = gc.getCanalAV();
		if (canalAvisos == null) canalAvisos = "Não definido.";

		return canalAvisos;
	}

	public static void updateGuildCanalAvisos(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalAV(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCargoWarn(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String cargoWarnID = gc.getCargoWarn();
		if (cargoWarnID == null) cargoWarnID = "Não definido.";

		return cargoWarnID;
	}

	public static void updateGuildCargoWarn(String newCargoID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCargoWarn(newCargoID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static Map<String, Object> getGuildCargoNew(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getCargoNew();
	}

	public static void updateGuildCargoNew(Role r, guildConfig gc) {
		EntityManager em = getEntityManager();

		Map<String, Object> cn = gc.getCargoNew();
		cn.put(r.getName(), r.getId());
		gc.setCargoNew(new JSONObject(cn));

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalLvlUp(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalLvlUp = gc.getCanalLvl();
		if (canalLvlUp == null) canalLvlUp = "Não definido.";

		return canalLvlUp;
	}

	public static void updateGuildCanalLvlUp(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalLvl(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static Boolean getGuildLvlUpNotif(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getLvlNotif();
	}

	public static void updateGuildLvlUpNotif(Boolean LvlUpNotif, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setLvlNotif(LvlUpNotif);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();
		em.close();
	}

	public static Map<String, Object> getGuildCargosLvl(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getCargoslvl();
	}

	public static void updateGuildCargosLvl(String lvl, Role r, guildConfig gc) {
		EntityManager em = getEntityManager();

		Map<String, Object> cn = gc.getCargoslvl();
		cn.put(lvl, r.getId());
		gc.setCargosLvl(new JSONObject(cn));

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void switchGuildAnyTell(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();

		gc.setAnyTell(!gc.isAnyTell());

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();
		em.close();
	}

	public static String getGuildCanalRelay(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalRelay = gc.getCanalRelay();
		if (canalRelay == null) canalRelay = "Não definido.";

		return canalRelay;
	}

	public static void updateGuildCanalRelay(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalRelay(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static String getGuildCanalIA(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		String canalIA = gc.getCanalai();
		if (canalIA == null) canalIA = "Não definido.";

		return canalIA;
	}

	public static void updateGuildCanalIA(String newCanalID, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setCanalAi(newCanalID);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static boolean getGuildIaMode(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.isAiMode();
	}

	public static void updateGuildIaMode(boolean aiMode, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setAiMode(aiMode);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static List<String> getGuildNoLinkChannels(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getNoLinkChannels();
	}

	public static void updateGuildNoLinkChannels(guildConfig gc) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}
	
	public static void updateMemberBiography(Member m) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}
}
