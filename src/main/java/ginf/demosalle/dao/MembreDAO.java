package ginf.demosalle.dao;

import ginf.demosalle.model.Abonnement;
import ginf.demosalle.model.Membre;
import ginf.demosalle.model.Reservation;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MembreDAO {

    public Optional<Membre> findByUtilisateurId(Integer id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "FROM Membre m LEFT JOIN FETCH m.utilisateur WHERE m.id = :id", Membre.class)
                    .setParameter("id", id).uniqueResultOptional();
        }
    }

    public List<Abonnement> findAbonnements(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM Abonnement a WHERE a.membre.id = :id ORDER BY a.dateDebut DESC",
                    Abonnement.class).setParameter("id", idMembre).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public Optional<Abonnement> findAbonnementActif(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "FROM Abonnement a WHERE a.membre.id = :id AND a.statut = :st ORDER BY a.dateFin DESC",
                            Abonnement.class)
                    .setParameter("id", idMembre)
                    .setParameter("st", ginf.demosalle.model.Abonnement.Statut.actif)
                    .setMaxResults(1).uniqueResultOptional();
        }
    }

    public List<Reservation> findReservations(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM Reservation r " +
                            "LEFT JOIN FETCH r.seance se " +
                            "LEFT JOIN FETCH se.salle " +
                            "LEFT JOIN FETCH se.coach c " +
                            "LEFT JOIN FETCH c.utilisateur " +
                            "WHERE r.membre.id = :id ORDER BY se.dateHeure DESC",
                    Reservation.class).setParameter("id", idMembre).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public long countPresences(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Long r = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :id " +
                                    "AND r.statutReservation IN (:s1, :s2)", Long.class)
                    .setParameter("id", idMembre)
                    .setParameter("s1", Reservation.Statut.confirmee)
                    .setParameter("s2", Reservation.Statut.terminee)
                    .uniqueResult();
            return r != null ? r : 0L;
        }
    }

    public void update(Membre membre) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.merge(membre);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur mise à jour membre", e);
        }
    }
}