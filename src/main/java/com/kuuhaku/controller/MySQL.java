package com.kuuhaku.controller;

import com.kuuhaku.model.*;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

public class MySQL {

    private static EntityManagerFactory emf;

    private static EntityManager getEntityManager() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.user", "OH-34179");
        props.put("javax.persistence.jdbc.password", "OP-31814");

        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro", props);

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void dumpData(DataDump data) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        data.getCaDump().forEach(em::merge);
        data.getmDump().forEach(em::merge);
        data.getGcDump().forEach(em::merge);
        em.getTransaction().commit();
    }

    @SuppressWarnings("unchecked")
    public static DataDump getData() {
        EntityManager em = getEntityManager();

        Query ca = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
        Query m = em.createQuery("SELECT m FROM Member m", Member.class);
        Query gc = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

        return new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList());
    }

    public static void sendBeybladeToDB(Beyblade bb) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.merge(bb);
        em.getTransaction().commit();
    }

    public static Beyblade getBeybladeById(String id) {
        EntityManager em = getEntityManager();

        Beyblade bb;

        try {
            Query b = em.createQuery("SELECT b FROM Beyblade b WHERE id = ?1", Beyblade.class);
            b.setParameter(1, id);
            bb = (Beyblade) b.getSingleResult();

            return bb;
        } catch (NoResultException e) {
            return null;
        }
    }
}
