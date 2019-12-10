package com.kuuhaku.controller.SQLite;

import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.api.entities.Role;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

import static com.kuuhaku.controller.SQLite.Manager.getEntityManager;

public class GuildOperationsDAO {
	//GETTERS
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

	//UPDATERS
	public static void updateGuildPrefix(String newPrefix, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setPrefix(newPrefix);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void updateGuildName(String newName, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setName(newName);

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

	public static int getGuildPollTime(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getPollTime();
	}

	public static void updateGuildPollTime(int pollTime, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setPollTime(pollTime);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static int getGuildWarnTime(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getWarnTime();
	}

	public static void updateGuildWarnTime(int warnTime, guildConfig gc) {
		EntityManager em = getEntityManager();

		gc.setWarnTime(warnTime);

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

		return gc.isLvlNotif();
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
		if (cn.get(lvl) != null || r == null) cn.remove(lvl);
		else cn.put(lvl, r.getId());
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

	public static List<String> getGuildNoLinkChannels(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getNoLinkChannels();
	}

	public static List<String> getGuildNoSpamChannels(String id) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		guildConfig gc = (guildConfig) q.getSingleResult();
		em.close();

		return gc.getNoSpamChannels();
	}

	public static void updateGuildSettings(guildConfig gc) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void updateMemberSettings(Member m) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}
}
