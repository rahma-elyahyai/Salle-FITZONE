package ginf.demosalle.bean;

import ginf.demosalle.model.*;
import ginf.demosalle.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;

@Named("registerBean")
@RequestScoped
public class RegisterBean implements Serializable {

    private String nom;
    private String prenom;
    private String email;
    private String mdp;
    private String confirmMdp;
    private String telephone;
    private String roleChoisi       = "MEMBRE";

    // Champs COACH
    private String specialite;
    private String descriptionCoach;

    // Champs MEMBRE
    private String dateNaissance;
    private String adresse;

    @Inject
    private AuthService authService;

    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (!mdp.equals(confirmMdp)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Mots de passe différents", "Les deux mots de passe ne correspondent pas."));
            return null;
        }

        if (!authService.emailDisponible(email)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Email déjà utilisé", "Un compte existe déjà avec cet email."));
            return null;
        }

        try {
            Utilisateur u = new Utilisateur(
                    nom.trim(), prenom.trim(),
                    email.trim().toLowerCase(),
                    mdp, Role.valueOf(roleChoisi));
            u.setTelephone(telephone);

            if ("COACH".equals(roleChoisi)) {
                Coach coach = new Coach();
                coach.setSpecialite(specialite != null ? specialite : "Non spécifié");
                coach.setDescription(descriptionCoach);
                authService.inscrireCoach(u, coach);
            } else {
                Membre membre = new Membre();
                if (dateNaissance != null && !dateNaissance.isEmpty())
                    membre.setDateNaissance(LocalDate.parse(dateNaissance));
                membre.setAdresse(adresse);
                authService.inscrireMembre(u, membre);
            }

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Inscription réussie !", "Vous pouvez maintenant vous connecter."));
            return "/login.xhtml?faces-redirect=true";

        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
                    "Erreur : " + e.getMessage(), "Inscription impossible, veuillez réessayer."));
            return null;
        }
    }

    // Getters / Setters
    public String getNom()                     { return nom; }
    public void setNom(String n)               { this.nom = n; }
    public String getPrenom()                  { return prenom; }
    public void setPrenom(String p)            { this.prenom = p; }
    public String getEmail()                   { return email; }
    public void setEmail(String e)             { this.email = e; }
    public String getMdp()                     { return mdp; }
    public void setMdp(String m)               { this.mdp = m; }
    public String getConfirmMdp()              { return confirmMdp; }
    public void setConfirmMdp(String c)        { this.confirmMdp = c; }
    public String getTelephone()               { return telephone; }
    public void setTelephone(String t)         { this.telephone = t; }
    public String getRoleChoisi()              { return roleChoisi; }
    public void setRoleChoisi(String r)        { this.roleChoisi = r; }
    public String getSpecialite()              { return specialite; }
    public void setSpecialite(String s)        { this.specialite = s; }
    public String getDescriptionCoach()        { return descriptionCoach; }
    public void setDescriptionCoach(String d)  { this.descriptionCoach = d; }
    public String getDateNaissance()           { return dateNaissance; }
    public void setDateNaissance(String d)     { this.dateNaissance = d; }
    public String getAdresse()                 { return adresse; }
    public void setAdresse(String a)           { this.adresse = a; }
}