package com.kuuhaku.controller;

import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Database {
    private static EntityManagerFactory emf;


    private static EntityManager getEntityManager() {
        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro");

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void sendAllGuildConfigs(Collection<guildConfig> gc) {
        EntityManager em = getEntityManager();
        Query q = em.createQuery("DELETE FROM guildConfig");

        em.getTransaction().begin();
        q.executeUpdate();
        em.getTransaction().commit();
        System.out.println("Dados resetados com sucesso!");

        em.getTransaction().begin();
        gc.forEach(em::persist);
        em.getTransaction().commit();
        em.close();
        System.out.println("Dados salvos com sucesso!");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, guildConfig> getGuildConfigs() {
        List<guildConfig> lgc;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM guildConfig c", guildConfig.class);
            lgc = q.getResultList();
            em.close();

            return lgc.stream().collect(Collectors.toMap(guildConfig::getGuildId, g -> g));
        } catch (Exception e) {
            System.out.println("Erro ao recuperar configurações: " + e);
            return null;
        }
    }
}
