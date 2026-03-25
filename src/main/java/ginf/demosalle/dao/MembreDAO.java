package ginf.demosalle.dao;

import ginf.demosalle.model.Abonnement;
import ginf.demosalle.model.Membre;
import ginf.demosalle.model.Reservation;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MembreDAO {

    /** Trouve le Membre lié à un id_utilisateur */
    public Optional<Membre> findByUtilisateurId(Integer idUtilisateur) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Membre> q = s.createQuery(
                    "FROM Membre m LEFT JOIN FETCH m.utilisateur WHERE m.id = :id",
                    Membre.class
            );
            q.setParameter("id", idUtilisateur);
            return q.uniqueResultOptional();
        }
    }

    /** Abonnements du membre, du plus récent au plus ancien */
    public List<Abonnement> findAbonnements(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Abonnement> q = s.createQuery(
                    "FROM Abonnement a WHERE a.membre.id = :id ORDER BY a.dateDebut DESC",
                    Abonnement.class
            );
            q.setParameter("id", idMembre);
            return q.list();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Abonnement actif du membre (statut = 'actif') */
    public Optional<Abonnement> findAbonnementActif(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Abonnement> q = s.createQuery(
                    "FROM Abonnement a WHERE a.membre.id = :id AND a.statut = 'actif' " +
                            "ORDER BY a.dateFin DESC",
                    Abonnement.class
            );
            q.setParameter("id", idMembre);
            q.setMaxResults(1);
            return q.uniqueResultOptional();
        }
    }

    /** Réservations du membre avec séance + salle + coach chargés */
    public List<Reservation> findReservations(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Reservation> q = s.createQuery(
                    "FROM Reservation r " +
                            "LEFT JOIN FETCH r.seance se " +
                            "LEFT JOIN FETCH se.salle " +
                            "LEFT JOIN FETCH se.coach c " +
                            "LEFT JOIN FETCH c.utilisateur " +
                            "WHERE r.membre.id = :id " +
                            "ORDER BY se.dateHeure DESC",
                    Reservation.class
            );
            q.setParameter("id", idMembre);
            return q.list();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Nombre total de présences (réservations terminées ou confirmées) */
    public long countPresences(Integer idMembre) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> q = s.createQuery(
                    "SELECT COUNT(r) FROM Reservation r " +
                            "WHERE r.membre.id = :id " +
                            "AND r.statutReservation IN ('confirmee','terminee')",
                    Long.class
            );
            q.setParameter("id", idMembre);
            Long result = q.uniqueResult();
            return result != null ? result : 0L;
        }
    }
}