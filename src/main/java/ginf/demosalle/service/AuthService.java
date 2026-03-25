package ginf.demosalle.service;

import ginf.demosalle.dao.UtilisateurDAO;
import ginf.demosalle.model.Role;
import ginf.demosalle.model.Utilisateur;
import ginf.demosalle.util.PasswordUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class AuthService {

    public enum LoginResult {
        SUCCESS,
        EMAIL_INCONNU,
        MOT_DE_PASSE_INCORRECT
    }

    @Inject
    private UtilisateurDAO dao;

    public LoginResult login(String email, String mdp) {
        if (email == null || mdp == null) return LoginResult.EMAIL_INCONNU;

        Optional<Utilisateur> opt = dao.findByEmail(email);

        if (opt.isEmpty()) {
            // Timing attack protection — toujours vérifier même si email inconnu
            PasswordUtil.verify(mdp, "$2a$12$dummyhashXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            return LoginResult.EMAIL_INCONNU;
        }

        if (!PasswordUtil.verify(mdp, opt.get().getMdp())) {
            return LoginResult.MOT_DE_PASSE_INCORRECT;
        }

        return LoginResult.SUCCESS;
    }

    public Utilisateur getByEmail(String email) {
        return dao.findByEmail(email).orElse(null);
    }

    public String redirectUrl(Role role) {
        return switch (role) {
            case COACH  -> "/coach/dashboard.xhtml?faces-redirect=true";
            case MEMBRE -> "/membre/dashboard.xhtml?faces-redirect=true";
            case ADMIN -> "/admin/dashboard.xhtml?faces-redirect=true";
        };
    }

    public boolean emailDisponible(String email) {
        return !dao.emailExists(email);
    }

    public void inscrire(Utilisateur u) {
        u.setMdp(PasswordUtil.hash(u.getMdp()));
        dao.save(u);
    }
}