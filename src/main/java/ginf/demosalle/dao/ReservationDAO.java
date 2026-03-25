package ginf.demosalle.dao;

import ginf.demosalle.model.Reservation;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ReservationDAO {

    public List<Reservation> findByMembre(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "FROM Reservation r LEFT JOIN FETCH r.seance se " +
                                    "LEFT JOIN FETCH se.salle LEFT JOIN FETCH se.coach c LEFT JOIN FETCH c.utilisateur " +
                                    "WHERE r.membre.id = :id ORDER BY se.dateHeure DESC", Reservation.class)
                    .setParameter("id", idMembre).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public boolean existsReservation(Integer idMembre, Integer idSeance) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Long count = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :m AND r.seance.id = :se " +
                                    "AND r.statutReservation != :ann", Long.class)
                    .setParameter("m", idMembre)
                    .setParameter("se", idSeance)
                    .setParameter("ann", Reservation.Statut.annulee)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public Reservation findById(Integer id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Reservation.class, id);
        }
    }

    public void save(Reservation r) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction(); s.persist(r); tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw new RuntimeException(e); }
    }

    public void update(Reservation r) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction(); s.merge(r); tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw new RuntimeException(e); }
    }
}