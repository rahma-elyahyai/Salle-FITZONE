package ginf.demosalle.bean;

import ginf.demosalle.dao.SalleDAO;
import ginf.demosalle.dao.SeanceDAO;
import ginf.demosalle.dao.UtilisateurDAO;
import ginf.demosalle.model.Coach;
import ginf.demosalle.model.Membre;
import ginf.demosalle.model.Salle;
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
import org.hibernate.Transaction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Named("coachDashboardBean")
@SessionScoped
public class CoachDashboardBean implements Serializable {

    @Inject
    private SeanceDAO seanceDAO;

    @Inject
    private SalleDAO salleDAO;

    @Inject
    private UtilisateurDAO utilisateurDAO;

    private Coach coach;
    private List<Seance> toutesSeances = new ArrayList<>();
    private List<Membre> membres = new ArrayList<>();

    private Seance seanceForm = new Seance();
    private boolean modeEditionSeance = false;
    private String dateHeureStr = "";
    private Integer idSalleSelectionne;
    private List<Salle> salles = new ArrayList<>();

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String specialite;
    private String description;
    private String nouveauMdp = "";
    private String confirmMdp = "";

    @PostConstruct
    public void init() {
        chargerSalles();
        charger();
    }

    private void chargerSalles() {
        try {
            List<Salle> resultat = salleDAO.findAll();
            salles = (resultat != null) ? resultat : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            salles = new ArrayList<>();
        }
    }

    private void chargerSeances(Integer idCoach) {
        try {
            List<Seance> resultat = seanceDAO.findByCoach(idCoach);
            toutesSeances = (resultat != null) ? resultat : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            toutesSeances = new ArrayList<>();
        }
    }

    public void charger() {
        Utilisateur u = utilisateurConnecte();
        if (u == null) {
            resetData();
            return;
        }

        chargerCoach(u.getIdUtilisateur());
        chargerSeances(u.getIdUtilisateur());
        chargerMembres(u.getIdUtilisateur());

        nom = safe(u.getNom());
        prenom = safe(u.getPrenom());
        email = safe(u.getEmail());
        telephone = safe(u.getTelephone());

        if (coach != null) {
            specialite = safe(coach.getSpecialite());
            description = safe(coach.getDescription());
        } else {
            specialite = "";
            description = "";
        }
    }

    private void chargerCoach(Integer idCoach) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            coach = s.createQuery(
                            "SELECT c FROM Coach c " +
                                    "JOIN FETCH c.utilisateur " +
                                    "WHERE c.id = :id",
                            Coach.class
                    )
                    .setParameter("id", idCoach)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            coach = null;
        }
    }

    public List<Seance> findByCoach(Integer idCoach) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT s FROM Seance s " +
                                    "LEFT JOIN FETCH s.salle " +
                                    "LEFT JOIN FETCH s.coach c " +
                                    "WHERE c.id = :id " +
                                    "ORDER BY s.dateHeure DESC",
                            Seance.class
                    )
                    .setParameter("id", idCoach)
                    .getResultList();
        }
    }

    private void resetData() {
        coach = null;
        toutesSeances = new ArrayList<>();
        membres = new ArrayList<>();
        salles = new ArrayList<>();
        seanceForm = new Seance();
        modeEditionSeance = false;
        dateHeureStr = "";
        idSalleSelectionne = null;
        nom = "";
        prenom = "";
        email = "";
        telephone = "";
        specialite = "";
        description = "";
        nouveauMdp = "";
        confirmMdp = "";
    }

    private void chargerMembres(Integer idCoach) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            membres = s.createQuery(
                            "SELECT DISTINCT m " +
                                    "FROM Reservation r " +
                                    "JOIN r.membre m " +
                                    "JOIN FETCH m.utilisateur u " +
                                    "WHERE r.seance.coach.id = :id " +
                                    "ORDER BY u.nom, u.prenom",
                            Membre.class
                    )
                    .setParameter("id", idCoach)
                    .getResultList();

            if (membres == null) {
                membres = new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            membres = new ArrayList<>();
        }
    }

    public void syncEncadre(Membre membre) {
        Utilisateur u = utilisateurConnecte();
        if (u == null || membre == null || membre.getId() == null) {
            return;
        }

        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Coach coachDB = s.createQuery(
                            "SELECT DISTINCT c FROM Coach c " +
                                    "LEFT JOIN FETCH c.membresEncadres " +
                                    "WHERE c.id = :id",
                            Coach.class
                    )
                    .setParameter("id", u.getIdUtilisateur())
                    .uniqueResult();

            Membre membreDB = s.get(Membre.class, membre.getId());

            if (coachDB != null && membreDB != null) {
                boolean existe = coachDB.getMembresEncadres()
                        .stream()
                        .anyMatch(m -> m.getId().equals(membreDB.getId()));

                if (!existe) {
                    coachDB.getMembresEncadres().add(membreDB);
                    s.merge(coachDB);
                }
            }

            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (tx != null) {
                tx.rollback();
            }
        }
    }

    public int getTotalSeances() {
        return (toutesSeances != null) ? toutesSeances.size() : 0;
    }

    public int getTotalMembres() {
        return (membres != null) ? membres.size() : 0;
    }

    public long getSeancesPlanifiees() {
        if (toutesSeances == null) {
            return 0;
        }

        return toutesSeances.stream()
                .filter(s -> s != null && s.getStatut() == Seance.Statut.planifiee)
                .count();
    }

    public long getSeancesAujourdhui() {
        if (toutesSeances == null) {
            return 0;
        }

        LocalDateTime debut = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return toutesSeances.stream()
                .filter(s -> s != null
                        && s.getDateHeure() != null
                        && !s.getDateHeure().isBefore(debut)
                        && s.getDateHeure().isBefore(debut.plusDays(1)))
                .count();
    }

    public int getHeuresEntrainement() {
        if (toutesSeances == null) {
            return 0;
        }

        YearMonth mois = YearMonth.now();

        int minutes = toutesSeances.stream()
                .filter(s -> s != null
                        && s.getDateHeure() != null
                        && s.getDuree() != null
                        && YearMonth.from(s.getDateHeure()).equals(mois))
                .mapToInt(Seance::getDuree)
                .sum();

        return minutes / 60;
    }

    public List<Seance> getProchainesSeances() {
        if (toutesSeances == null) {
            return Collections.emptyList();
        }

        return toutesSeances.stream()
                .filter(s -> s != null
                        && s.getStatut() == Seance.Statut.planifiee
                        && s.getDateHeure() != null
                        && s.getDateHeure().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Seance::getDateHeure))
                .limit(5)
                .collect(Collectors.toList());
    }

    public List<Seance> getDernieresSeances() {
        if (toutesSeances == null || toutesSeances.isEmpty()) {
            return Collections.emptyList();
        }

        List<Seance> copie = toutesSeances.stream()
                .filter(s -> s != null)
                .sorted(Comparator.comparing(
                        Seance::getDateHeure,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(10)
                .collect(Collectors.toList());

        return new ArrayList<>(copie);
    }

    public long getNbSeancesMembre(Integer idMembre) {
        Utilisateur u = utilisateurConnecte();
        if (u == null || idMembre == null) {
            return 0;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Long result = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r " +
                                    "WHERE r.membre.id = :m AND r.seance.coach.id = :c",
                            Long.class
                    )
                    .setParameter("m", idMembre)
                    .setParameter("c", u.getIdUtilisateur())
                    .uniqueResult();

            return (result != null) ? result : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void nouvelleSeance() {
        seanceForm = new Seance();
        dateHeureStr = "";
        idSalleSelectionne = null;
        modeEditionSeance = false;
    }

    public void editerSeance(Seance s) {
        if (s == null || s.getId() == null) {
            return;
        }

        seanceForm = seanceDAO.findById(s.getId());

        if (seanceForm == null) {
            seanceForm = new Seance();
            modeEditionSeance = false;
            dateHeureStr = "";
            idSalleSelectionne = null;
            return;
        }

        dateHeureStr = (seanceForm.getDateHeure() != null)
                ? seanceForm.getDateHeure().toString().substring(0, 16)
                : "";

        idSalleSelectionne = (seanceForm.getSalle() != null)
                ? seanceForm.getSalle().getId()
                : null;

        modeEditionSeance = true;
    }

    public void sauvegarderSeance() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (dateHeureStr == null || dateHeureStr.isBlank()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Champ requis",
                        "La date et heure sont obligatoires."
                ));
                return;
            }

            seanceForm.setDateHeure(LocalDateTime.parse(dateHeureStr));

            if (idSalleSelectionne == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Champ requis",
                        "Veuillez sélectionner une salle."
                ));
                return;
            }

            Salle salle = salleDAO.findById(idSalleSelectionne);
            if (salle == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Salle invalide",
                        "La salle sélectionnée est introuvable."
                ));
                return;
            }
            seanceForm.setSalle(salle);

            if (!modeEditionSeance) {
                Utilisateur u = utilisateurConnecte();
                if (u != null) {
                    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                        Coach coachDb = s.get(Coach.class, u.getIdUtilisateur());
                        seanceForm.setCoach(coachDb);
                    }
                }
            }

            if (modeEditionSeance) {
                seanceDAO.update(seanceForm);
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Modifiée",
                        "La séance a été mise à jour."
                ));
            } else {
                seanceDAO.save(seanceForm);
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Créée",
                        "Nouvelle séance enregistrée."
                ));
            }

            charger();
            nouvelleSeance();

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Impossible d'enregistrer la séance : " + e.getMessage()
            ));
        }
    }

    public void supprimerSeance(Seance s) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (s == null || s.getId() == null) {
            return;
        }

        try {
            seanceDAO.delete(s.getId());
            charger();

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Supprimée",
                    "La séance « " + s.getTitre() + " » a été supprimée."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Impossible de supprimer : " + e.getMessage()
            ));
        }
    }

    public void sauvegarderProfil() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Utilisateur u = utilisateurConnecte();

        if (u == null) {
            return;
        }

        if (nouveauMdp != null && !nouveauMdp.isEmpty()) {
            if (!nouveauMdp.equals(confirmMdp)) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Les mots de passe ne correspondent pas."
                ));
                return;
            }

            if (nouveauMdp.length() < 6) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Mot de passe trop court (min 6 caractères)."
                ));
                return;
            }
        }

        if (email != null
                && !email.equalsIgnoreCase(u.getEmail())
                && utilisateurDAO.emailExistsExcept(email, u.getIdUtilisateur())) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Email utilisé",
                    "Cet email est déjà pris."
            ));
            return;
        }

        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Utilisateur uM = s.get(Utilisateur.class, u.getIdUtilisateur());
            if (uM == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Utilisateur introuvable."
                ));
                return;
            }

            uM.setNom(safe(nom).trim());
            uM.setPrenom(safe(prenom).trim());
            uM.setEmail(safe(email).trim().toLowerCase());
            uM.setTelephone(safe(telephone));

            if (nouveauMdp != null && !nouveauMdp.isEmpty()) {
                uM.setMdp(PasswordUtil.hash(nouveauMdp));
            }

            Coach cM = s.get(Coach.class, u.getIdUtilisateur());
            if (cM != null) {
                cM.setSpecialite(safe(specialite));
                cM.setDescription(safe(description));
            }

            tx.commit();

            ctx.getExternalContext().getSessionMap().put("utilisateurConnecte", uM);
            nouveauMdp = "";
            confirmMdp = "";
            charger();

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Profil mis à jour",
                    "Modifications sauvegardées."
            ));
        } catch (Exception e) {
            e.printStackTrace();

            if (tx != null) {
                tx.rollback();
            }

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Impossible de sauvegarder le profil : " + e.getMessage()
            ));
        }
    }

    private Utilisateur utilisateurConnecte() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }

        Object obj = context.getExternalContext()
                .getSessionMap()
                .get("utilisateurConnecte");

        return (obj instanceof Utilisateur) ? (Utilisateur) obj : null;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    public Coach getCoach() {
        return coach;
    }

    public List<Seance> getToutesSeances() {
        return toutesSeances;
    }

    public List<Membre> getMembres() {
        return membres;
    }

    public List<Salle> getSalles() {
        return salles;
    }

    public Seance getSeanceForm() {
        return seanceForm;
    }

    public void setSeanceForm(Seance seanceForm) {
        this.seanceForm = seanceForm;
    }

    public boolean isModeEditionSeance() {
        return modeEditionSeance;
    }

    public String getDateHeureStr() {
        return dateHeureStr;
    }

    public void setDateHeureStr(String dateHeureStr) {
        this.dateHeureStr = dateHeureStr;
    }

    public Integer getIdSalleSelectionne() {
        return idSalleSelectionne;
    }

    public void setIdSalleSelectionne(Integer idSalleSelectionne) {
        this.idSalleSelectionne = idSalleSelectionne;
    }

    public Seance.Categorie[] getCategories() {
        return Seance.Categorie.values();
    }

    public Seance.Statut[] getStatuts() {
        return Seance.Statut.values();
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNouveauMdp() {
        return nouveauMdp;
    }

    public void setNouveauMdp(String nouveauMdp) {
        this.nouveauMdp = nouveauMdp;
    }

    public String getConfirmMdp() {
        return confirmMdp;
    }

    public void setConfirmMdp(String confirmMdp) {
        this.confirmMdp = confirmMdp;
    }
}