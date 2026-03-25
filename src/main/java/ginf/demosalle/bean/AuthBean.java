package ginf.demosalle.bean;

import ginf.demosalle.model.Utilisateur;
import ginf.demosalle.service.AuthService;
import ginf.demosalle.service.AuthService.LoginResult;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {

    private String email;
    private String mdp;

    private Utilisateur utilisateurConnecte;
    private boolean     connecte = false;

    // ✅ CORRIGÉ : @Inject au lieu de new AuthService()
    // WildFly CDI gère le cycle de vie du service
    @Inject
    private AuthService authService;

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        LoginResult result = authService.login(email, mdp);

        switch (result) {
            case SUCCESS -> {
                utilisateurConnecte = authService.getByEmail(email);
                connecte = true;
                ExternalContext ec = ctx.getExternalContext();
                ec.getSessionMap().put("utilisateur", utilisateurConnecte);
                ec.getSessionMap().put("role",        utilisateurConnecte.getRole().name());
                mdp = null;
                return authService.redirectUrl(utilisateurConnecte.getRole());
            }
            case EMAIL_INCONNU -> ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Email introuvable",
                    "Aucun compte n'existe avec cet email."
            ));
            case MOT_DE_PASSE_INCORRECT -> ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Mot de passe incorrect",
                    "Veuillez vérifier votre mot de passe."
            ));
        }
        mdp = null;
        return null;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        utilisateurConnecte = null;
        connecte            = false;
        email               = null;
        mdp                 = null;
        return "/login.xhtml?faces-redirect=true";
    }

    public boolean isCoach() {
        return connecte && utilisateurConnecte != null
                && ginf.demosalle.model.Role.COACH == utilisateurConnecte.getRole();
    }

    public boolean isMembre() {
        return connecte && utilisateurConnecte != null
                && ginf.demosalle.model.Role.MEMBRE == utilisateurConnecte.getRole();
    }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }
    public String getMdp()                      { return mdp; }
    public void setMdp(String mdp)              { this.mdp = mdp; }
    public Utilisateur getUtilisateurConnecte() { return utilisateurConnecte; }
    public boolean isConnecte()                 { return connecte; }
    public String getInitiales() {
        if (utilisateurConnecte == null) return "?";
        String p = utilisateurConnecte.getPrenom();
        String n = utilisateurConnecte.getNom();
        return (p != null && !p.isEmpty() ? String.valueOf(p.charAt(0)) : "")
                + (n != null && !n.isEmpty() ? String.valueOf(n.charAt(0)) : "");
    }
}