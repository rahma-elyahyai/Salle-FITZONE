package ginf.demosalle.bean;

import ginf.demosalle.dao.AbonnementDAO;
import ginf.demosalle.dao.MembreDAO;
import ginf.demosalle.dao.ReservationDAO;
import ginf.demosalle.dao.SeanceDAO;
import ginf.demosalle.dao.UtilisateurDAO;
import ginf.demosalle.model.Abonnement;
import ginf.demosalle.model.Membre;
import ginf.demosalle.model.Reservation;
import ginf.demosalle.model.Seance;
import ginf.demosalle.model.Utilisateur;
import ginf.demosalle.util.HibernateUtil;
import ginf.demosalle.util.PasswordUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hibernate.Session;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Named("membreBean")
@SessionScoped
public class MembreDashboardBean implements Serializable {

    @Inject private MembreDAO membreDAO;
    @Inject private AbonnementDAO abonnementDAO;
    @Inject private ReservationDAO reservationDAO;
    @Inject private SeanceDAO seanceDAO;
    @Inject private UtilisateurDAO utilisateurDAO;

    private Membre membre;
    private Abonnement abonnementActif;
    private List<Abonnement> historiquesAbonnements = new ArrayList<>();
    private List<Reservation> toutesReservations = new ArrayList<>();
    private List<Seance> seancesPlanifiees = new ArrayList<>();

    private Integer idSeanceSelectionnee;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private String dateNaissanceStr = "";
    private String nouveauMdp = "";
    private String confirmMdp = "";

    private Integer reservationAannuler;
    private Abonnement.Type typeAbonnementSelectionne;

    @PostConstruct
    public void init() {
        try {
            charger();
        } catch (Exception e) {
            e.printStackTrace();
            resetData();
        }
    }

    public void charger() {
        Utilisateur u = utilisateurConnecte();
        if (u == null) {
            resetData();
            return;
        }

        Integer id = u.getIdUtilisateur();

        membre = membreDAO.findByUtilisateurId(id).orElse(null);
        abonnementActif = abonnementDAO.findActifByMembre(id).orElse(null);

        List<Abonnement> hist = abonnementDAO.findByMembre(id);
        historiquesAbonnements = (hist != null) ? hist : new ArrayList<>();

        List<Reservation> reservations = reservationDAO.findByMembre(id);
        toutesReservations = (reservations != null) ? reservations : new ArrayList<>();

        List<Seance> seances = seanceDAO.findPlanifiees();
        seancesPlanifiees = (seances != null) ? seances : new ArrayList<>();

        nom = u.getNom();
        prenom = u.getPrenom();
        email = u.getEmail();
        telephone = u.getTelephone();

        if (membre != null) {
            adresse = membre.getAdresse();
            dateNaissanceStr = membre.getDateNaissance() != null
                    ? membre.getDateNaissance().toString()
                    : "";
        } else {
            adresse = "";
            dateNaissanceStr = "";
        }
    }

    private void resetData() {
        membre = null;
        abonnementActif = null;
        historiquesAbonnements = new ArrayList<>();
        toutesReservations = new ArrayList<>();
        seancesPlanifiees = new ArrayList<>();
        nom = "";
        prenom = "";
        email = "";
        telephone = "";
        adresse = "";
        dateNaissanceStr = "";
        idSeanceSelectionnee = null;
        reservationAannuler = null;
        typeAbonnementSelectionne = null;
    }

    public long getTotalPresences() {
        if (membre == null) return 0;
        return membreDAO.countPresences(membre.getId());
    }

    public long getSeancesCeMois() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = debut.plusMonths(1);

        return toutesReservations.stream()
                .filter(r -> r.getSeance() != null && r.getSeance().getDateHeure() != null)
                .filter(r -> {
                    LocalDate d = r.getSeance().getDateHeure().toLocalDate();
                    return !d.isBefore(debut) && d.isBefore(fin);
                })
                .count();
    }

    public List<Reservation> getReservationsAvenir() {
        return toutesReservations.stream()
                .filter(r -> r.getSeance() != null
                        && r.getSeance().getDateHeure() != null
                        && r.getSeance().getDateHeure().isAfter(LocalDateTime.now())
                        && r.getStatutReservation() != Reservation.Statut.annulee)
                .sorted(Comparator.comparing(r -> r.getSeance().getDateHeure()))
                .collect(Collectors.toList());
    }

    public List<Reservation> getReservationsHistorique() {
        return toutesReservations.stream()
                .filter(r -> r.getSeance() != null
                        && r.getSeance().getDateHeure() != null
                        && (r.getSeance().getDateHeure().isBefore(LocalDateTime.now())
                        || r.getStatutReservation() == Reservation.Statut.annulee))
                .sorted(Comparator.comparing((Reservation r) -> r.getSeance().getDateHeure()).reversed())
                .collect(Collectors.toList());
    }

    public int getProgressionAbonnement() {
        if (abonnementActif == null) return 0;

        LocalDate debut = abonnementActif.getDateDebut();
        LocalDate fin = abonnementActif.getDateFin();

        if (debut == null || fin == null) return 0;

        long total = ChronoUnit.DAYS.between(debut, fin);
        long ecoule = ChronoUnit.DAYS.between(debut, LocalDate.now());

        if (total <= 0) return 100;

        return (int) Math.min(100, Math.max(0, (ecoule * 100) / total));
    }

    public String getProgressBarStyle() {
        return "width:" + getProgressionAbonnement() + "%;";
    }

    public Reservation getProchaineReservation() {
        List<Reservation> avenir = getReservationsAvenir();
        return (!avenir.isEmpty()) ? avenir.get(0) : null;
    }

    public long getJoursRestants() {
        if (abonnementActif == null || abonnementActif.getDateFin() == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), abonnementActif.getDateFin()));
    }

    public String getTypeAbonnementLabel() {
        if (abonnementActif == null) return "—";
        return switch (abonnementActif.getType()) {
            case mensuel -> "Mensuel";
            case annuel -> "Annuel";
            case seance_unique -> "Séance unique";
        };
    }

    public void preparerSouscription(String typeStr) {
        try {
            typeAbonnementSelectionne = Abonnement.Type.valueOf(typeStr);
        } catch (Exception e) {
            typeAbonnementSelectionne = null;
        }
    }

    public void preparerAnnulation(Integer idReservation) {
        this.reservationAannuler = idReservation;
    }

    public void souscrireMensuel() {
        typeAbonnementSelectionne = Abonnement.Type.mensuel;
        souscrireAbonnement();
    }

    public void souscrireAnnuel() {
        typeAbonnementSelectionne = Abonnement.Type.annuel;
        souscrireAbonnement();
    }

    public void souscrireSeanceUnique() {
        typeAbonnementSelectionne = Abonnement.Type.seance_unique;
        souscrireAbonnement();
    }

    public void reserverSeance() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (idSeanceSelectionnee == null) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Sélection requise", "Veuillez choisir une séance."));
            return;
        }

        if (membre == null) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", "Membre introuvable."));
            return;
        }

        if (reservationDAO.existsReservation(membre.getId(), idSeanceSelectionnee)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Déjà réservé", "Vous êtes déjà inscrit à cette séance."));
            return;
        }

        try {
            Seance seance = seanceDAO.findById(idSeanceSelectionnee);

            Reservation r = new Reservation();
            r.setMembre(membre);
            r.setSeance(seance);
            r.setDateReservation(LocalDateTime.now());
            r.setStatutReservation(Reservation.Statut.confirmee);

            reservationDAO.save(r);
            charger();
            idSeanceSelectionnee = null;

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Réservé", "Votre place est confirmée pour « " + seance.getTitre() + " »."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", e.getMessage()));
        }
    }

    public void annulerReservation() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (reservationAannuler == null) return;

        try {
            Reservation r = reservationDAO.findById(reservationAannuler);
            if (r != null) {
                r.setStatutReservation(Reservation.Statut.annulee);
                r.setDateAnnulation(LocalDateTime.now());
                reservationDAO.update(r);
                charger();
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Annulée", "La réservation a été annulée."));
            }
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", e.getMessage()));
        }
    }

    public void souscrireAbonnement() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (membre == null || typeAbonnementSelectionne == null) return;

        try {
            if (abonnementActif != null) {
                abonnementActif.setStatut(Abonnement.Statut.renouvele);
                abonnementDAO.update(abonnementActif);
            }

            Abonnement a = new Abonnement();
            a.setMembre(membre);
            a.setType(typeAbonnementSelectionne);
            a.setDateDebut(LocalDate.now());
            a.setStatut(Abonnement.Statut.actif);

            switch (typeAbonnementSelectionne) {
                case mensuel -> {
                    a.setDateFin(LocalDate.now().plusMonths(1));
                    a.setPrix(new BigDecimal("250.00"));
                }
                case annuel -> {
                    a.setDateFin(LocalDate.now().plusYears(1));
                    a.setPrix(new BigDecimal("2400.00"));
                }
                case seance_unique -> {
                    a.setDateFin(LocalDate.now().plusDays(1));
                    a.setPrix(new BigDecimal("50.00"));
                }
            }

            abonnementDAO.save(a);
            charger();

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Abonnement activé",
                    "Votre abonnement " + typeAbonnementSelectionne + " est maintenant actif."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", e.getMessage()));
        }
    }

    public void annulerAbonnement(Abonnement a) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            abonnementDAO.delete(a.getId());
            charger();
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Supprimé", "L'abonnement a été supprimé."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", e.getMessage()));
        }
    }

    public void sauvegarderProfil() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Utilisateur u = utilisateurConnecte();

        if (u == null) return;

        if (nouveauMdp != null && !nouveauMdp.isEmpty()) {
            if (!nouveauMdp.equals(confirmMdp)) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur", "Les mots de passe ne correspondent pas."));
                return;
            }

            if (nouveauMdp.length() < 6) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur", "Mot de passe trop court (min 6 caractères)."));
                return;
            }
        }

        if (!email.equalsIgnoreCase(u.getEmail())
                && utilisateurDAO.emailExistsExcept(email, u.getIdUtilisateur())) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Email utilisé", "Cet email est déjà pris."));
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = s.beginTransaction();

            Utilisateur uM = s.get(Utilisateur.class, u.getIdUtilisateur());
            uM.setNom(nom.trim());
            uM.setPrenom(prenom.trim());
            uM.setEmail(email.trim().toLowerCase());
            uM.setTelephone(telephone);

            if (nouveauMdp != null && !nouveauMdp.isEmpty()) {
                uM.setMdp(PasswordUtil.hash(nouveauMdp));
            }

            Membre mM = s.get(Membre.class, u.getIdUtilisateur());
            if (mM != null) {
                mM.setAdresse(adresse);
                if (dateNaissanceStr != null && !dateNaissanceStr.isEmpty()) {
                    mM.setDateNaissance(LocalDate.parse(dateNaissanceStr));
                }
            }

            tx.commit();

            ctx.getExternalContext().getSessionMap().put("utilisateurConnecte", uM);
            nouveauMdp = "";
            confirmMdp = "";
            charger();

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Profil mis à jour", "Vos modifications ont été sauvegardées."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", e.getMessage()));
        }
    }

    public String formatTypeAbonnement(Abonnement.Type type) {
        if (type == null) return "—";
        return switch (type) {
            case mensuel -> "Mensuel";
            case annuel -> "Annuel";
            case seance_unique -> "Séance unique";
        };
    }

    private Utilisateur utilisateurConnecte() {
        return (Utilisateur) FacesContext.getCurrentInstance()
                .getExternalContext()
                .getSessionMap()
                .get("utilisateurConnecte");
    }

    public Membre getMembre() { return membre; }
    public Abonnement getAbonnementActif() { return abonnementActif; }
    public List<Abonnement> getHistoriquesAbonnements() { return historiquesAbonnements; }
    public List<Reservation> getToutesReservations() { return toutesReservations; }
    public List<Seance> getSeancesPlanifiees() { return seancesPlanifiees; }
    public Integer getIdSeanceSelectionnee() { return idSeanceSelectionnee; }
    public void setIdSeanceSelectionnee(Integer idSeanceSelectionnee) { this.idSeanceSelectionnee = idSeanceSelectionnee; }
    public Integer getReservationAannuler() { return reservationAannuler; }
    public void setReservationAannuler(Integer reservationAannuler) { this.reservationAannuler = reservationAannuler; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getDateNaissanceStr() { return dateNaissanceStr; }
    public void setDateNaissanceStr(String dateNaissanceStr) { this.dateNaissanceStr = dateNaissanceStr; }
    public String getNouveauMdp() { return nouveauMdp; }
    public void setNouveauMdp(String nouveauMdp) { this.nouveauMdp = nouveauMdp; }
    public String getConfirmMdp() { return confirmMdp; }
    public void setConfirmMdp(String confirmMdp) { this.confirmMdp = confirmMdp; }
    public Abonnement.Type getTypeAbonnementSelectionne() { return typeAbonnementSelectionne; }
    public void setTypeAbonnementSelectionne(Abonnement.Type typeAbonnementSelectionne) { this.typeAbonnementSelectionne = typeAbonnementSelectionne; }
}