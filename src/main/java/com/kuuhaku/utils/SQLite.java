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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQLite {

    private static Connection con;
    private static EntityManagerFactory emf;

    public static void connect() {
        con = null;

        File DBfile = new File(Main.getInfo().getDBFileName());
        if (!DBfile.exists()) {
            System.out.println("❌ | O ficheiro usado como base de dados não foi encontrado. Entre no servidor discord oficial da Shiro para obter ajuda.");
        }

        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url", "jdbc:sqlite:" + DBfile.getPath());

        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro_local", props);

        emf.getCache().evictAll();

        System.out.println("✅ | Ligação à base de dados estabelecida.");
    }

    private static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void disconnect() throws SQLException {
        if (con != null) {
            con.close();
            System.out.println("✅ | Ligação à base de dados desfeita.");
        }
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

    public static String getGuildPrefix(String id) {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT c FROM guildConfig c WHERE guild_id = ?1", guildConfig.class);
        q.setParameter(1, id);
        guildConfig gc = (guildConfig) q.getSingleResult();
        String prefix = gc.getPrefix();

        if (prefix == null) prefix = "s!";
        em.close();

        return prefix;
    }

    public static void addGuildToDB(Guild guild) {
        EntityManager em = getEntityManager();

        guildConfig gc = new guildConfig();
        gc.setName(guild.getName());
        gc.setGuildId(guild.getId());
        gc.setOwner(guild.getOwner().getUser().getAsTag());

        em.getTransaction().begin();
        em.merge(gc);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeGuildFromDB(guildConfig gc) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.remove(em.getReference(gc.getClass(), gc.getGuildId()));
        em.getTransaction().commit();

        em.close();
    }
}