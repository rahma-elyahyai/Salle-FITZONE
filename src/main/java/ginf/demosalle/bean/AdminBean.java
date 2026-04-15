package ginf.demosalle.bean;

import ginf.demosalle.model.*;
import ginf.demosalle.util.HibernateUtil;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Named("adminBean")
@SessionScoped
public class AdminBean implements Serializable {

    @Inject
    private AuthBean authBean;

    // ── Listes ───────────────────────────────────────────────
    private List<Utilisateur> tousMembers;
    private List<Utilisateur> tousCoachs;
    private List<Seance>      toutesSeances;
    private List<Abonnement>  tousAbonnements;
    private List<Salle>       toutesSalles;

    // ── Stats dashboard ──────────────────────────────────────
    private long nbMembres;
    private long nbCoachs;
    private long nbSeances;
    private long nbAbonnementsActifs;

    // ── Formulaire nouveau coach ─────────────────────────────
    private String  coachNom;
    private String  coachPrenom;
    private String  coachEmail;
    private String  coachMdp;
    private String  coachTelephone;
    private String  coachSpecialite;
    private String  coachDescription;

    // ── Formulaire nouvelle séance ───────────────────────────
    private String        seanceTitre;
    private String        seanceDescription;
    private LocalDateTime seanceDateHeure;
    private Integer       seanceDuree;
    private Integer       seanceCapacite;
    private String        seanceCategorie;
    private Integer       seanceCoachId;
    private Integer       seanceSalleId;

    // ── Formulaire modifier séance ───────────────────────────
    private Seance   seanceEnEdition;
    private boolean  modeEditionSeance = false;

    // ── Formulaire assigner abonnement ───────────────────────
    private Integer    aboMembreId;
    private String     aboType;
    private LocalDate  aboDateDebut;
    private LocalDate  aboDateFin;
    private BigDecimal aboPrix;

    // ── Suppression ─────────────────────────────────────────
    private Integer idASupprimer;

    @PostConstruct
    public void init() {
        chargerDonnees();
    }

    public void chargerDonnees() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            // Membres
            tousMembers = s.createQuery(
                            "FROM Utilisateur u WHERE u.role = :r ORDER BY u.nom", Utilisateur.class)
                    .setParameter("r", Role.MEMBRE).getResultList();

            // Coachs
            tousCoachs = s.createQuery(
                            "FROM Utilisateur u WHERE u.role = :r ORDER BY u.nom", Utilisateur.class)
                    .setParameter("r", Role.COACH).getResultList();

            // Séances + associations
            toutesSeances = s.createQuery(
                    "FROM Seance s ORDER BY s.dateHeure DESC", Seance.class).getResultList();
            for (Seance sc : toutesSeances) {
                org.hibernate.Hibernate.initialize(sc.getSalle());
                org.hibernate.Hibernate.initialize(sc.getCoach());
                if (sc.getCoach() != null)
                    org.hibernate.Hibernate.initialize(sc.getCoach().getUtilisateur());
            }

            // Abonnements + associations
            tousAbonnements = s.createQuery(
                    "FROM Abonnement a ORDER BY a.dateDebut DESC", Abonnement.class).getResultList();
            for (Abonnement a : tousAbonnements)
                org.hibernate.Hibernate.initialize(a.getMembre());

            // Salles (pour le formulaire séance)
            toutesSalles = s.createQuery(
                    "FROM Salle s ORDER BY s.nomSalle", Salle.class).getResultList();

            // Stats
            nbMembres         = (long) tousMembers.size();
            nbCoachs          = (long) tousCoachs.size();
            nbSeances         = (long) toutesSeances.size();
            nbAbonnementsActifs = s.createQuery(
                            "SELECT COUNT(a) FROM Abonnement a WHERE a.statut = :st", Long.class)
                    .setParameter("st", Abonnement.Statut.actif).uniqueResult();
        }
    }

    // ════════════════════════════════════════════════════════
    // MEMBRES — bloquer / supprimer
    // ════════════════════════════════════════════════════════

    public void supprimerMembre() {
        if (idASupprimer == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            // Supprimer réservations du membre
            s.createMutationQuery(
                            "DELETE FROM Reservation r WHERE r.membre.id = :mid")
                    .setParameter("mid", idASupprimer).executeUpdate();

            // Supprimer abonnements du membre
            s.createMutationQuery(
                            "DELETE FROM Abonnement a WHERE a.membre.id = :mid")
                    .setParameter("mid", idASupprimer).executeUpdate();

            // Supprimer Membre
            s.createMutationQuery(
                            "DELETE FROM Membre m WHERE m.id = :mid")
                    .setParameter("mid", idASupprimer).executeUpdate();

            // Supprimer Utilisateur
            s.createMutationQuery(
                            "DELETE FROM Utilisateur u WHERE u.id = :uid")
                    .setParameter("uid", idASupprimer).executeUpdate();

            tx.commit();
            addInfo("Membre supprimé avec succès.");
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur suppression membre : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // COACHS — ajouter / supprimer
    // ════════════════════════════════════════════════════════

    public void ajouterCoach() {
        if (coachNom == null || coachNom.isBlank() ||
                coachEmail == null || coachEmail.isBlank() ||
                coachMdp == null || coachMdp.isBlank()) {
            addError("Nom, email et mot de passe sont obligatoires.");
            return;
        }
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            // Vérifier doublon email
            long existe = s.createQuery(
                            "SELECT COUNT(u) FROM Utilisateur u WHERE u.email = :e", Long.class)
                    .setParameter("e", coachEmail.trim()).uniqueResult();
            if (existe > 0) { addError("Cet email est déjà utilisé."); return; }

            // Créer utilisateur
            Utilisateur u = new Utilisateur();
            u.setNom(coachNom.trim());
            u.setPrenom(coachPrenom != null ? coachPrenom.trim() : "");
            u.setEmail(coachEmail.trim());
            u.setMdp(BCrypt.hashpw(coachMdp, BCrypt.gensalt()));
            u.setRole(Role.COACH);
            u.setTelephone(coachTelephone);
            u.setDateCreation(LocalDateTime.now());
            s.persist(u);
            s.flush();

            // Créer Coach
            Coach c = new Coach();
            c.setUtilisateur(u);
            c.setSpecialite(coachSpecialite != null ? coachSpecialite.trim() : "Non défini");
            c.setDescription(coachDescription);
            s.persist(c);

            tx.commit();
            addInfo("Coach « " + u.getNomComplet() + " » créé avec succès.");
            viderFormulaireCoach();
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur création coach : " + e.getMessage());
        }
    }

    public void supprimerCoach() {
        if (idASupprimer == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            // Mettre les séances du coach à null coach (ou supprimer)
            List<Seance> seancesCoach = s.createQuery(
                            "FROM Seance sc WHERE sc.coach.id = :cid", Seance.class)
                    .setParameter("cid", idASupprimer).getResultList();
            for (Seance sc : seancesCoach) {
                // Supprimer les réservations de ces séances
                s.createMutationQuery(
                                "DELETE FROM Reservation r WHERE r.seance.id = :sid")
                        .setParameter("sid", sc.getId()).executeUpdate();
            }
            s.createMutationQuery(
                            "DELETE FROM Seance sc WHERE sc.coach.id = :cid")
                    .setParameter("cid", idASupprimer).executeUpdate();

            // Supprimer Coach
            s.createMutationQuery(
                            "DELETE FROM Coach c WHERE c.id = :cid")
                    .setParameter("cid", idASupprimer).executeUpdate();

            // Supprimer Utilisateur
            s.createMutationQuery(
                            "DELETE FROM Utilisateur u WHERE u.id = :uid")
                    .setParameter("uid", idASupprimer).executeUpdate();

            tx.commit();
            addInfo("Coach supprimé avec succès.");
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur suppression coach : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // SÉANCES — créer / modifier / supprimer
    // ════════════════════════════════════════════════════════

    public void creerSeance() {
        if (seanceTitre == null || seanceTitre.isBlank()) {
            addError("Le titre est obligatoire."); return;
        }
        if (seanceDateHeure == null || seanceCoachId == null || seanceSalleId == null) {
            addError("Date, coach et salle sont obligatoires."); return;
        }
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Coach coach = s.createQuery(
                            "FROM Coach c WHERE c.id = :id", Coach.class)
                    .setParameter("id", seanceCoachId).uniqueResult();
            Salle salle = s.get(Salle.class, seanceSalleId);

            if (coach == null || salle == null) {
                addError("Coach ou salle introuvable."); return;
            }

            Seance sc = new Seance();
            sc.setTitre(seanceTitre.trim());
            sc.setDescription(seanceDescription);
            sc.setDateHeure(seanceDateHeure);
            sc.setDuree(seanceDuree != null ? seanceDuree : 60);
            sc.setCapaciteMaximale(seanceCapacite != null ? seanceCapacite : 20);
            sc.setCategorie(Seance.Categorie.valueOf(seanceCategorie));
            sc.setStatut(Seance.Statut.planifiee);
            sc.setCoach(coach);
            sc.setSalle(salle);
            s.persist(sc);

            tx.commit();
            addInfo("Séance « " + sc.getTitre() + " » créée avec succès.");
            viderFormulaireSeance();
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur création séance : " + e.getMessage());
        }
    }

    public void preparerEditionSeance(Integer seanceId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            seanceEnEdition = s.get(Seance.class, seanceId);
            if (seanceEnEdition != null) {
                org.hibernate.Hibernate.initialize(seanceEnEdition.getSalle());
                org.hibernate.Hibernate.initialize(seanceEnEdition.getCoach());
                // Pré-remplir le formulaire
                seanceTitre       = seanceEnEdition.getTitre();
                seanceDescription = seanceEnEdition.getDescription();
                seanceDateHeure   = seanceEnEdition.getDateHeure();
                seanceDuree       = seanceEnEdition.getDuree();
                seanceCapacite    = seanceEnEdition.getCapaciteMaximale();
                seanceCategorie   = seanceEnEdition.getCategorie().name();
                seanceCoachId     = seanceEnEdition.getCoach().getId();
                seanceSalleId     = seanceEnEdition.getSalle().getId();
                modeEditionSeance = true;
            }
        }
    }

    public void sauvegarderSeance() {
        if (seanceEnEdition == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Seance sc = s.get(Seance.class, seanceEnEdition.getId());
            Coach  coach = s.createQuery(
                            "FROM Coach c WHERE c.id = :id", Coach.class)
                    .setParameter("id", seanceCoachId).uniqueResult();
            Salle  salle = s.get(Salle.class, seanceSalleId);

            sc.setTitre(seanceTitre.trim());
            sc.setDescription(seanceDescription);
            sc.setDateHeure(seanceDateHeure);
            sc.setDuree(seanceDuree);
            sc.setCapaciteMaximale(seanceCapacite);
            sc.setCategorie(Seance.Categorie.valueOf(seanceCategorie));
            sc.setCoach(coach);
            sc.setSalle(salle);
            s.merge(sc);

            tx.commit();
            addInfo("Séance mise à jour.");
            modeEditionSeance = false;
            viderFormulaireSeance();
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur modification séance : " + e.getMessage());
        }
    }

    public void annulerEditionSeance() {
        modeEditionSeance = false;
        seanceEnEdition   = null;
        viderFormulaireSeance();
    }

    public void supprimerSeance() {
        if (idASupprimer == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery(
                            "DELETE FROM Reservation r WHERE r.seance.id = :sid")
                    .setParameter("sid", idASupprimer).executeUpdate();
            s.createMutationQuery(
                            "DELETE FROM Seance sc WHERE sc.id = :sid")
                    .setParameter("sid", idASupprimer).executeUpdate();
            tx.commit();
            addInfo("Séance supprimée.");
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur suppression séance : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // ABONNEMENTS — assigner / supprimer
    // ════════════════════════════════════════════════════════

    public void assignerAbonnement() {
        if (aboMembreId == null || aboType == null || aboDateDebut == null || aboDateFin == null || aboPrix == null) {
            addError("Tous les champs sont obligatoires."); return;
        }
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            // Expirer l'abonnement actif existant
            s.createMutationQuery(
                            "UPDATE Abonnement a SET a.statut = :exp " +
                                    "WHERE a.membre.id = :mid AND a.statut = :act")
                    .setParameter("exp", Abonnement.Statut.renouvele)
                    .setParameter("mid", aboMembreId)
                    .setParameter("act", Abonnement.Statut.actif)
                    .executeUpdate();

            Membre m = s.createQuery(
                            "FROM Membre m WHERE m.id = :id", Membre.class)
                    .setParameter("id", aboMembreId).uniqueResult();

            if (m == null) { addError("Membre introuvable."); return; }

            Abonnement a = new Abonnement();
            a.setMembre(m);
            a.setType(Abonnement.Type.valueOf(aboType));
            a.setDateDebut(aboDateDebut);
            a.setDateFin(aboDateFin);
            a.setPrix(aboPrix);
            a.setStatut(Abonnement.Statut.actif);
            s.persist(a);

            tx.commit();
            addInfo("Abonnement assigné avec succès.");
            viderFormulaireAbonnement();
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur assignation abonnement : " + e.getMessage());
        }
    }

    public void supprimerAbonnement() {
        if (idASupprimer == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery(
                            "DELETE FROM Abonnement a WHERE a.id = :id")
                    .setParameter("id", idASupprimer).executeUpdate();
            tx.commit();
            addInfo("Abonnement supprimé.");
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur suppression abonnement : " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────
    public List<Utilisateur> getListeCoachs() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "FROM Utilisateur u WHERE u.role = :r", Utilisateur.class)
                    .setParameter("r", Role.COACH).getResultList();
        }
    }

    public long getNbInscrits(Integer seanceId) {
        if (seanceId == null) return 0;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.seance.id = :sid " +
                                    "AND r.statutReservation <> :ann", Long.class)
                    .setParameter("sid", seanceId)
                    .setParameter("ann", Reservation.Statut.annulee).uniqueResult();
        }
    }

    private void viderFormulaireCoach() {
        coachNom = null; coachPrenom = null; coachEmail = null;
        coachMdp = null; coachTelephone = null;
        coachSpecialite = null; coachDescription = null;
    }

    private void viderFormulaireSeance() {
        seanceTitre = null; seanceDescription = null; seanceDateHeure = null;
        seanceDuree = null; seanceCapacite = null; seanceCategorie = null;
        seanceCoachId = null; seanceSalleId = null; seanceEnEdition = null;
    }

    private void viderFormulaireAbonnement() {
        aboMembreId = null; aboType = null;
        aboDateDebut = null; aboDateFin = null; aboPrix = null;
    }

    private void addInfo(String msg)  { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,  msg, null)); }
    private void addError(String msg) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null)); }
    // ═══════════════════════════════════════════════════════════════════
// MÉTHODES À AJOUTER dans AdminBean.java
// Placez ces méthodes dans la section "── Helpers ──"
// ═══════════════════════════════════════════════════════════════════

    /**
     * Retourne vrai si le membre (par userId) possède un abonnement actif.
     * Utilisé dans membres.xhtml pour l'indicateur de statut.
     */
    public boolean aAbonnementActif(Integer userId) {
        if (userId == null) return false;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            long count = s.createQuery(
                            "SELECT COUNT(a) FROM Abonnement a " +
                                    "WHERE a.membre.id = :uid AND a.statut = :st", Long.class)
                    .setParameter("uid", userId)
                    .setParameter("st", Abonnement.Statut.actif)
                    .uniqueResult();
            return count > 0;
        }
    }

    /**
     * Retourne la spécialité d'un coach par son userId (Utilisateur.id).
     * Utilisé dans coachs.xhtml pour afficher le badge spécialité.
     */
    public String getSpecialiteCoach(Integer userId) {
        if (userId == null) return "—";
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Coach coach = s.createQuery(
                            "FROM Coach c WHERE c.utilisateur.id = :uid", Coach.class)
                    .setParameter("uid", userId)
                    .uniqueResult();
            if (coach == null || coach.getSpecialite() == null) return "—";
            return coach.getSpecialite();
        }
    }

    /**
     * Calcule le pourcentage de remplissage d'une séance (0–100).
     * Utilisé dans seances.xhtml pour la barre de capacité.
     */
    public int getPct(Integer seanceId, Integer capaciteMax) {
        if (seanceId == null || capaciteMax == null || capaciteMax == 0) return 0;
        long inscrits = getNbInscrits(seanceId);
        int pct = (int) Math.round((inscrits * 100.0) / capaciteMax);
        return Math.min(pct, 100);
    }

    // ── Getters / Setters ────────────────────────────────────
    public List<Utilisateur> getTousMembers()       { return tousMembers      != null ? tousMembers      : Collections.emptyList(); }
    public List<Utilisateur> getTousCoachs()        { return tousCoachs       != null ? tousCoachs       : Collections.emptyList(); }
    public List<Seance>      getToutesSeances()     { return toutesSeances    != null ? toutesSeances    : Collections.emptyList(); }
    public List<Abonnement>  getTousAbonnements()   { return tousAbonnements  != null ? tousAbonnements  : Collections.emptyList(); }
    public List<Salle>       getToutesSalles()      { return toutesSalles     != null ? toutesSalles     : Collections.emptyList(); }
    public long  getNbMembres()                     { return nbMembres; }
    public long  getNbCoachs()                      { return nbCoachs; }
    public long  getNbSeances()                     { return nbSeances; }
    public long  getNbAbonnementsActifs()           { return nbAbonnementsActifs; }
    public boolean isModeEditionSeance()            { return modeEditionSeance; }
    public Seance  getSeanceEnEdition()             { return seanceEnEdition; }

    public String  getCoachNom()                    { return coachNom; }
    public void    setCoachNom(String v)            { this.coachNom = v; }
    public String  getCoachPrenom()                 { return coachPrenom; }
    public void    setCoachPrenom(String v)         { this.coachPrenom = v; }
    public String  getCoachEmail()                  { return coachEmail; }
    public void    setCoachEmail(String v)          { this.coachEmail = v; }
    public String  getCoachMdp()                    { return coachMdp; }
    public void    setCoachMdp(String v)            { this.coachMdp = v; }
    public String  getCoachTelephone()              { return coachTelephone; }
    public void    setCoachTelephone(String v)      { this.coachTelephone = v; }
    public String  getCoachSpecialite()             { return coachSpecialite; }
    public void    setCoachSpecialite(String v)     { this.coachSpecialite = v; }
    public String  getCoachDescription()            { return coachDescription; }
    public void    setCoachDescription(String v)    { this.coachDescription = v; }

    public String        getSeanceTitre()           { return seanceTitre; }
    public void          setSeanceTitre(String v)   { this.seanceTitre = v; }
    public String        getSeanceDescription()     { return seanceDescription; }
    public void          setSeanceDescription(String v) { this.seanceDescription = v; }
    public LocalDateTime getSeanceDateHeure()       { return seanceDateHeure; }
    public void          setSeanceDateHeure(LocalDateTime v) { this.seanceDateHeure = v; }
    public Integer       getSeanceDuree()           { return seanceDuree; }
    public void          setSeanceDuree(Integer v)  { this.seanceDuree = v; }
    public Integer       getSeanceCapacite()        { return seanceCapacite; }
    public void          setSeanceCapacite(Integer v){ this.seanceCapacite = v; }
    public String        getSeanceCategorie()       { return seanceCategorie; }
    public void          setSeanceCategorie(String v){ this.seanceCategorie = v; }
    public Integer       getSeanceCoachId()         { return seanceCoachId; }
    public void          setSeanceCoachId(Integer v){ this.seanceCoachId = v; }
    public Integer       getSeanceSalleId()         { return seanceSalleId; }
    public void          setSeanceSalleId(Integer v){ this.seanceSalleId = v; }

    public Integer    getAboMembreId()              { return aboMembreId; }
    public void       setAboMembreId(Integer v)     { this.aboMembreId = v; }
    public String     getAboType()                  { return aboType; }
    public void       setAboType(String v)          { this.aboType = v; }
    public LocalDate  getAboDateDebut()             { return aboDateDebut; }
    public void       setAboDateDebut(LocalDate v)  { this.aboDateDebut = v; }
    public LocalDate  getAboDateFin()               { return aboDateFin; }
    public void       setAboDateFin(LocalDate v)    { this.aboDateFin = v; }
    public BigDecimal getAboPrix()                  { return aboPrix; }
    public void       setAboPrix(BigDecimal v)      { this.aboPrix = v; }
    public Integer    getIdASupprimer()             { return idASupprimer; }
    public void       setIdASupprimer(Integer v)    { this.idASupprimer = v; }
}