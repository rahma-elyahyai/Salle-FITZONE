package ginf.demosalle.service;

import ginf.demosalle.dao.UtilisateurDAO;
import ginf.demosalle.model.*;
import ginf.demosalle.util.HibernateUtil;
import ginf.demosalle.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Optional;

@ApplicationScoped
public class AuthService {

    public enum LoginResult { SUCCESS, EMAIL_INCONNU, MOT_DE_PASSE_INCORRECT }

    @Inject
    private UtilisateurDAO dao;

    // ── Connexion ─────────────────────────────────────────────────────

    public LoginResult login(String email, String mdp) {
        if (email == null || mdp == null) return LoginResult.EMAIL_INCONNU;
        Optional<Utilisateur> opt = dao.findByEmail(email);
        if (opt.isEmpty()) {
            PasswordUtil.verify(mdp, "$2a$12$dummyhashXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            return LoginResult.EMAIL_INCONNU;
        }
        if (!PasswordUtil.verify(mdp, opt.get().getMdp()))
            return LoginResult.MOT_DE_PASSE_INCORRECT;
        return LoginResult.SUCCESS;
    }

    public Utilisateur getByEmail(String email) {
        return dao.findByEmail(email).orElse(null);
    }

    public String redirectUrl(Role role) {
        return switch (role) {
            case COACH  -> "/coach/dashboard.xhtml?faces-redirect=true";
            case MEMBRE -> "/membre/dashboard.xhtml?faces-redirect=true";
        };
    }

    public boolean emailDisponible(String email) {
        return !dao.emailExists(email);
    }

    // ── Inscription Membre ────────────────────────────────────────────

    public void inscrireMembre(Utilisateur utilisateur, Membre membre) throws Exception {
        utilisateur.setMdp(PasswordUtil.hash(utilisateur.getMdp()));
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.persist(utilisateur);
            s.flush();
            membre.setUtilisateur(utilisateur);
            s.persist(membre);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new Exception("Erreur inscription membre : " + e.getMessage(), e);
        }
    }

    // ── Inscription Coach ─────────────────────────────────────────────

    public void inscrireCoach(Utilisateur utilisateur, Coach coach) throws Exception {
        utilisateur.setMdp(PasswordUtil.hash(utilisateur.getMdp()));
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.persist(utilisateur);
            s.flush();
            coach.setUtilisateur(utilisateur);
            s.persist(coach);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new Exception("Erreur inscription coach : " + e.getMessage(), e);
        }
    }
}