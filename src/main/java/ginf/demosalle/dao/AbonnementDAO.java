package ginf.demosalle.dao;

import ginf.demosalle.model.Abonnement;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AbonnementDAO {

    public List<Abonnement> findByMembre(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM Abonnement a WHERE a.membre.id = :id ORDER BY a.dateDebut DESC",
                    Abonnement.class).setParameter("id", idMembre).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public Optional<Abonnement> findActifByMembre(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "FROM Abonnement a WHERE a.membre.id = :id AND a.statut = :st ORDER BY a.dateFin DESC",
                            Abonnement.class)
                    .setParameter("id", idMembre)
                    .setParameter("st", Abonnement.Statut.actif)
                    .setMaxResults(1).uniqueResultOptional();
        }
    }

    public Abonnement findById(Integer id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Abonnement.class, id);
        }
    }

    public void save(Abonnement a) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction(); s.persist(a); tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw new RuntimeException(e); }
    }

    public void update(Abonnement a) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction(); s.merge(a); tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw new RuntimeException(e); }
    }

    public void delete(Integer id) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Abonnement a = s.get(Abonnement.class, id);
            if (a != null) s.remove(a);
            tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw new RuntimeException(e); }
    }
}