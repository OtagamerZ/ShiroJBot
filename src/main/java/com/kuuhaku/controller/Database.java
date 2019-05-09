package com.kuuhaku.controller;

import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.List;

public class Database {
    private static EntityManagerFactory emf;


    public static EntityManager getEntityManager() {
        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro");

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void initDatabase() {
        guildConfig gc = new guildConfig();

        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.persist(gc);
        em.getTransaction().commit();
        em.close();
        System.out.println("Banco de dados inicializado com sucesso!");
    }

    public static void sendConfig(guildConfig gc) {
        try {
            EntityManager em = getEntityManager();
            em.getTransaction().begin();
            em.persist(gc);
            em.getTransaction().commit();
            em.close();
            System.out.println("Entidade inserida com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao inserir: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<guildConfig> listConfig() {
        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM `LnVsGtoAwc`.`guildConfig` c", guildConfig.class);
            List<guildConfig> lgc = q.getResultList();
            em.close();

            return lgc;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar configurações: " + e);
            return null;
        }
    }
}
