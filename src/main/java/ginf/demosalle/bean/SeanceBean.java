package ginf.demosalle.bean;

import ginf.demosalle.dao.SalleDAO;
import ginf.demosalle.dao.SeanceDAO;
import ginf.demosalle.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Named("seanceBean")
@SessionScoped
public class SeanceBean implements Serializable {

    @Inject private SeanceDAO seanceDAO;
    @Inject private SalleDAO  salleDAO;

    private List<Seance> seances;
    private List<Salle>  salles;
    private Seance       seanceForm         = new Seance();
    private boolean      modeEdition        = false;
    private String       dateHeureStr       = "";
    private Integer      idSalleSelectionne = null;

    @PostConstruct
    public void init() {
        salles = salleDAO.findAll();
        chargerSeances();
    }

    public void chargerSeances() {
        Utilisateur u = utilisateurConnecte();
        seances = (u != null && u.getRole() == Role.COACH)
                ? seanceDAO.findByCoach(u.getIdUtilisateur())
                : seanceDAO.findAll();
    }

    public void nouvelleSeance() {
        seanceForm = new Seance(); dateHeureStr = ""; idSalleSelectionne = null; modeEdition = false;
    }

    public void editer(Seance s) {
        seanceForm = seanceDAO.findById(s.getId());
        dateHeureStr = seanceForm.getDateHeure() != null
                ? seanceForm.getDateHeure().toString().substring(0, 16) : "";
        idSalleSelectionne = seanceForm.getSalle() != null ? seanceForm.getSalle().getId() : null;
        modeEdition = true;
    }

    public void sauvegarder() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            if (dateHeureStr == null || dateHeureStr.isEmpty()) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Requis", "Date et heure obligatoires.")); return;
            }
            seanceForm.setDateHeure(LocalDateTime.parse(dateHeureStr));
            if (idSalleSelectionne == null) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Requis", "Salle obligatoire.")); return;
            }
            seanceForm.setSalle(salleDAO.findById(idSalleSelectionne));
            if (!modeEdition) {
                Utilisateur u = utilisateurConnecte();
                if (u != null) { Coach c = new Coach(); c.setId(u.getIdUtilisateur()); seanceForm.setCoach(c); }
            }
            if (modeEdition) { seanceDAO.update(seanceForm); ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Modifiée", "Séance mise à jour.")); }
            else             { seanceDAO.save(seanceForm);   ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Créée",    "Séance créée.")); }
            chargerSeances(); nouvelleSeance();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
        }
    }

    public void supprimer(Seance s) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            seanceDAO.delete(s.getId());
            chargerSeances();
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Supprimée", "Séance supprimée."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
        }
    }

    private Utilisateur utilisateurConnecte() {
        return (Utilisateur) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("utilisateurConnecte");
    }

    public List<Seance>       getSeances()                       { return seances; }
    public List<Salle>        getSalles()                        { return salles; }
    public Seance             getSeanceForm()                    { return seanceForm; }
    public void               setSeanceForm(Seance s)            { this.seanceForm = s; }
    public boolean            isModeEdition()                    { return modeEdition; }
    public String             getDateHeureStr()                  { return dateHeureStr; }
    public void               setDateHeureStr(String d)          { this.dateHeureStr = d; }
    public Integer            getIdSalleSelectionne()            { return idSalleSelectionne; }
    public void               setIdSalleSelectionne(Integer i)   { this.idSalleSelectionne = i; }
    public Seance.Categorie[] getCategories()                    { return Seance.Categorie.values(); }
    public Seance.Statut[]    getStatuts()                       { return Seance.Statut.values(); }
}