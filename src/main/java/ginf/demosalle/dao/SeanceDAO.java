package ginf.demosalle.dao;

import ginf.demosalle.model.Seance;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class SeanceDAO {

    private static final String FETCH =
            "FROM Seance se LEFT JOIN FETCH se.salle " +
                    "LEFT JOIN FETCH se.coach c LEFT JOIN FETCH c.utilisateur ";

    public List<Seance> findAll() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(FETCH + "ORDER BY se.dateHeure DESC", Seance.class).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public List<Seance> findByCoach(Integer idCoach) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "SELECT DISTINCT se FROM Seance se " +
                                    "LEFT JOIN FETCH se.salle sa " +
                                    "LEFT JOIN FETCH se.coach c " +
                                    "WHERE c.id = :id " +
                                    "ORDER BY se.dateHeure DESC",
                            Seance.class
                    )
                    .setParameter("id", idCoach)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Seance> findPlanifiees() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            FETCH + "WHERE se.statut = :st ORDER BY se.dateHeure ASC", Seance.class)
                    .setParameter("st", Seance.Statut.planifiee).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public Seance findById(Integer id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Seance.class, id);
        }
    }

    public void save(Seance seance) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.persist(seance);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur création séance : " + e.getMessage(), e);
        }
    }

    public void update(Seance seance) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.merge(seance);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur modification séance : " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Seance seance = s.get(Seance.class, id);
            if (seance != null) s.remove(seance);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur suppression séance : " + e.getMessage(), e);
        }
    }
}