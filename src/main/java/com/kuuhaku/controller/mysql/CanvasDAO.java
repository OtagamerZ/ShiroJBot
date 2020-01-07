package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.PixelCanvas;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class CanvasDAO {
	public static PixelCanvas getCanvas() {
        EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM PixelCanvas c WHERE c.shelved = false", PixelCanvas.class);
		q.setMaxResults(1);

        try {
            PixelCanvas p = (PixelCanvas) q.getSingleResult();
            em.close();

            return p;
        } catch (NoResultException e) {
            em.close();

            return new PixelCanvas();
        }
    }

	public static void saveCanvas(PixelCanvas canvas) {
        EntityManager em = Manager.getEntityManager();

        em.getTransaction().begin();
        em.merge(canvas);
        em.getTransaction().commit();

        em.close();
    }
}
