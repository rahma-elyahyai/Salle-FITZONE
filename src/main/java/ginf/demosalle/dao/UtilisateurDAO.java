package ginf.demosalle.dao;

import ginf.demosalle.model.Utilisateur;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;

// ✅ CORRIGÉ : @ApplicationScoped rend ce DAO injectable via CDI
@ApplicationScoped
public class UtilisateurDAO {

    public Optional<Utilisateur> findByEmail(String email) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Utilisateur> q = s.createQuery(
                    "FROM Utilisateur u WHERE LOWER(u.email) = LOWER(:email)",
                    Utilisateur.class
            );
            q.setParameter("email", email.trim());
            return q.uniqueResultOptional();
        }
    }

    public boolean emailExists(String email) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> q = s.createQuery(
                    "SELECT COUNT(u) FROM Utilisateur u WHERE LOWER(u.email) = LOWER(:email)",
                    Long.class
            );
            q.setParameter("email", email.trim());
            return q.uniqueResult() > 0;
        }
    }

    public void save(Utilisateur u) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.persist(u);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur sauvegarde utilisateur", e);
        }
    }
}