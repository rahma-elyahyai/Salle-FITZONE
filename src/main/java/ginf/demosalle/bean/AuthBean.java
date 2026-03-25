package ginf.demosalle.bean;

import ginf.demosalle.model.Role;
import ginf.demosalle.model.Utilisateur;
import ginf.demosalle.service.AuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {

    @Inject
    private AuthService authService;

    private String email;
    private String mdp;

    private Utilisateur utilisateurConnecte;

    public String login() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        AuthService.LoginResult result = authService.login(email, mdp);

        switch (result) {

            case EMAIL_INCONNU:
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Email inconnu",
                                "Aucun compte associé à cet email."));
                return null;

            case MOT_DE_PASSE_INCORRECT:
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Mot de passe incorrect",
                                "Veuillez vérifier votre mot de passe."));
                return null;

            case SUCCESS:

                utilisateurConnecte = authService.getByEmail(email);

                ctx.getExternalContext().getSessionMap()
                        .put("utilisateurConnecte", utilisateurConnecte);

                return authService.redirectUrl(utilisateurConnecte.getRole());

            default:
                return null;
        }
    }

    public String logout() {

        utilisateurConnecte = null;

        FacesContext.getCurrentInstance()
                .getExternalContext()
                .invalidateSession();

        return "/login.xhtml?faces-redirect=true";
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public String getInitiales() {

        if(utilisateurConnecte == null)
            return "?";

        return utilisateurConnecte.getInitiales();
    }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getMdp() { return mdp; }

    public void setMdp(String mdp) { this.mdp = mdp; }
}