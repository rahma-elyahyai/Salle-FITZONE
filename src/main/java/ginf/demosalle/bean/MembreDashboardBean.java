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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Bean dashboard membre — CRUD complet :
 *   READ   : abonnement actif, séances dispo, réservations, historiques
 *   CREATE : inscrire à une séance
 *   UPDATE : annuler une réservation, modifier profil, changer mot de passe
 *   DELETE : (annulation de réservation = soft-delete)
 */
@Named("membreBean")
@SessionScoped
public class MembreDashboardBean implements Serializable {

    // ─── Injection ─────────────────────────────────
    @Inject
    private AuthBean authBean;

    // ─── Entités chargées ──────────────────────────
    private Membre     membre;
    private Abonnement abonnementActif;

    private List<Reservation> reservationsAvenir;
    private List<Reservation> reservationsHistorique;
    private List<Seance>      seancesDisponibles;
    private List<Abonnement>  historiquesAbonnements;

    // ─── Stats ─────────────────────────────────────
    private long seancesCeMois;
    private long totalPresences;
    private int s1, s2, s3, s4;

    // ─── Champs formulaire profil ──────────────────
    private String  telephone;
    private String  adresse;
    private boolean modeEditionProfil = false;

    // ─── Champs formulaire mot de passe (nouveaux) ─
    private String ancienMotDePasse;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;

    // ─── Action IDs ────────────────────────────────
    private Integer seanceAInscrire;
    private Integer reservationAannuler;

    // ═══════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════
    @PostConstruct
    public void init() {
        Utilisateur u = authBean.getUtilisateurConnecte();
        if (u == null) return;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Membre> membres = s.createQuery(
                            "FROM Membre m WHERE m.utilisateur.id = :uid", Membre.class)
                    .setParameter("uid", u.getId()).getResultList();
            if (membres.isEmpty()) return;
            this.membre    = membres.get(0);
            this.telephone = u.getTelephone();
            this.adresse   = this.membre.getAdresse();
        }
        chargerDonnees();
    }

    // ═══════════════════════════════════════════════
    //  READ — charger toutes les données du dashboard
    // ═══════════════════════════════════════════════
    public void chargerDonnees() {
        if (membre == null) return;
        LocalDateTime now   = LocalDateTime.now();
        LocalDate     today = now.toLocalDate();

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            /* Abonnement actif */
            List<Abonnement> abos = s.createQuery(
                            "FROM Abonnement a WHERE a.membre.id = :mid " +
                                    "AND a.statut = :st AND a.dateFin >= :today ORDER BY a.dateFin DESC", Abonnement.class)
                    .setParameter("mid",   membre.getId())
                    .setParameter("st",    Abonnement.Statut.actif)
                    .setParameter("today", today)
                    .setMaxResults(1).getResultList();
            abonnementActif = abos.isEmpty() ? null : abos.get(0);

            /* Réservations à venir */
            reservationsAvenir = s.createQuery(
                            "FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND r.seance.dateHeure > :now AND r.statutReservation <> :ann " +
                                    "ORDER BY r.seance.dateHeure ASC", Reservation.class)
                    .setParameter("mid", membre.getId())
                    .setParameter("now", now)
                    .setParameter("ann", Reservation.Statut.annulee).getResultList();
            initReservations(reservationsAvenir);

            /* Réservations historique */
            reservationsHistorique = s.createQuery(
                            "FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND r.seance.dateHeure <= :now ORDER BY r.seance.dateHeure DESC", Reservation.class)
                    .setParameter("mid", membre.getId())
                    .setParameter("now", now).getResultList();
            initReservations(reservationsHistorique);

            /* Stats mois */
            LocalDateTime debut = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime fin   = now.withDayOfMonth(today.lengthOfMonth()).withHour(23).withMinute(59);

            seancesCeMois = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND r.seance.dateHeure BETWEEN :debut AND :fin " +
                                    "AND r.statutReservation <> :ann", Long.class)
                    .setParameter("mid", membre.getId())
                    .setParameter("debut", debut).setParameter("fin", fin)
                    .setParameter("ann",  Reservation.Statut.annulee).uniqueResult();

            totalPresences = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND (r.statutReservation = :conf OR r.statutReservation = :term)", Long.class)
                    .setParameter("mid",  membre.getId())
                    .setParameter("conf", Reservation.Statut.confirmee)
                    .setParameter("term", Reservation.Statut.terminee).uniqueResult();

            /* Bar chart semaines */
            s1 = cntSem(s, debut,             debut.plusDays(6));
            s2 = cntSem(s, debut.plusDays(7),  debut.plusDays(13));
            s3 = cntSem(s, debut.plusDays(14), debut.plusDays(20));
            s4 = cntSem(s, debut.plusDays(21), fin);

            /* Séances disponibles (non inscrit, planifiées, futures) */
            List<Integer> dejInscrits = s.createQuery(
                            "SELECT r.seance.id FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND r.seance.dateHeure > :now AND r.statutReservation <> :ann", Integer.class)
                    .setParameter("mid", membre.getId())
                    .setParameter("now", now)
                    .setParameter("ann", Reservation.Statut.annulee).getResultList();

            seancesDisponibles = dejInscrits.isEmpty()
                    ? s.createQuery(
                            "FROM Seance s WHERE s.dateHeure > :now AND s.statut = :pl " +
                                    "ORDER BY s.dateHeure ASC", Seance.class)
                    .setParameter("now", now)
                    .setParameter("pl",  Seance.Statut.planifiee).getResultList()
                    : s.createQuery(
                            "FROM Seance s WHERE s.dateHeure > :now AND s.statut = :pl " +
                                    "AND s.id NOT IN :exclu ORDER BY s.dateHeure ASC", Seance.class)
                    .setParameter("now",   now)
                    .setParameter("pl",    Seance.Statut.planifiee)
                    .setParameter("exclu", dejInscrits).getResultList();

            for (Seance seance : seancesDisponibles) {
                org.hibernate.Hibernate.initialize(seance.getSalle());
                org.hibernate.Hibernate.initialize(seance.getCoach());
                if (seance.getCoach() != null)
                    org.hibernate.Hibernate.initialize(seance.getCoach().getUtilisateur());
            }

            /* Historique abonnements */
            historiquesAbonnements = s.createQuery(
                            "FROM Abonnement a WHERE a.membre.id = :mid ORDER BY a.dateDebut DESC", Abonnement.class)
                    .setParameter("mid", membre.getId()).getResultList();
        }
    }

    /** Initialise les associations Hibernate des réservations (évite LazyInit). */
    private void initReservations(List<Reservation> liste) {
        for (Reservation r : liste) {
            org.hibernate.Hibernate.initialize(r.getSeance());
            if (r.getSeance() != null) {
                org.hibernate.Hibernate.initialize(r.getSeance().getSalle());
                org.hibernate.Hibernate.initialize(r.getSeance().getCoach());
                if (r.getSeance().getCoach() != null)
                    org.hibernate.Hibernate.initialize(r.getSeance().getCoach().getUtilisateur());
            }
        }
    }

    private int cntSem(Session s, LocalDateTime d, LocalDateTime f) {
        Long n = s.createQuery(
                        "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :mid " +
                                "AND r.seance.dateHeure BETWEEN :d AND :f " +
                                "AND r.statutReservation <> :ann", Long.class)
                .setParameter("mid", membre.getId())
                .setParameter("d", d).setParameter("f", f)
                .setParameter("ann", Reservation.Statut.annulee).uniqueResult();
        return n == null ? 0 : n.intValue();
    }

    // ═══════════════════════════════════════════════
    //  CREATE — s'inscrire à une séance
    // ═══════════════════════════════════════════════
    public void inscrire() {
        if (seanceAInscrire == null || membre == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Seance seance = s.get(Seance.class, seanceAInscrire);
            if (seance == null) { addError("Séance introuvable."); return; }

            long inscrits = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.seance.id = :sid " +
                                    "AND r.statutReservation <> :ann", Long.class)
                    .setParameter("sid", seanceAInscrire)
                    .setParameter("ann", Reservation.Statut.annulee).uniqueResult();
            if (inscrits >= seance.getCapaciteMaximale()) {
                addError("Séance complète — impossible de s'inscrire."); return;
            }

            long doublon = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = :mid " +
                                    "AND r.seance.id = :sid AND r.statutReservation <> :ann", Long.class)
                    .setParameter("mid", membre.getId())
                    .setParameter("sid", seanceAInscrire)
                    .setParameter("ann", Reservation.Statut.annulee).uniqueResult();
            if (doublon > 0) { addError("Vous êtes déjà inscrit(e) à cette séance."); return; }

            Reservation r = new Reservation();
            r.setSeance(seance);
            r.setMembre(membre);
            r.setDateReservation(LocalDateTime.now());
            r.setStatutReservation(Reservation.Statut.confirmee);
            s.persist(r);
            tx.commit();
            addInfo("✅ Inscription confirmée : « " + seance.getTitre() + " » !");
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            addError("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    //  UPDATE — annuler une réservation (soft-delete)
    // ═══════════════════════════════════════════════
    public void annulerReservation() {
        if (reservationAannuler == null) return;
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Reservation r = s.get(Reservation.class, reservationAannuler);
            if (r != null && r.getMembre().getId().equals(membre.getId())) {
                r.setStatutReservation(Reservation.Statut.annulee);
                r.setDateAnnulation(LocalDateTime.now());
                s.merge(r);
                tx.commit();
                addInfo("Réservation annulée avec succès.");
            } else {
                addError("Réservation introuvable ou accès refusé.");
                if (tx.isActive()) tx.rollback();
            }
            chargerDonnees();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            addError("Erreur lors de l'annulation : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    //  UPDATE — sauvegarder profil (téléphone + adresse)
    // ═══════════════════════════════════════════════
    public void sauvegarderProfil() {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Utilisateur u = s.get(Utilisateur.class, authBean.getUtilisateurConnecte().getId());
            u.setTelephone(telephone);
            Membre m = s.get(Membre.class, membre.getId());
            m.setAdresse(adresse);
            s.merge(u);
            s.merge(m);
            tx.commit();
            // Rafraîchir le bean authBean
            authBean.getUtilisateurConnecte().setTelephone(telephone);
            membre.setAdresse(adresse);
            modeEditionProfil = false;
            addInfo("✅ Profil mis à jour avec succès !");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            addError("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    //  UPDATE — changer le mot de passe (nouveau CRUD)
    // ═══════════════════════════════════════════════
    public void changerMotDePasse() {
        /* Validations métier */
        if (ancienMotDePasse == null || ancienMotDePasse.isBlank()) {
            addError("Veuillez saisir votre mot de passe actuel."); return;
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 8) {
            addError("Le nouveau mot de passe doit contenir au moins 8 caractères."); return;
        }
        if (!nouveauMotDePasse.equals(confirmationMotDePasse)) {
            addError("Les deux mots de passe ne correspondent pas."); return;
        }

        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Utilisateur u = s.get(Utilisateur.class, authBean.getUtilisateurConnecte().getId());

            /* Vérifier l'ancien mot de passe (supposant stockage BCrypt ou plain) */
            if (!verifierMotDePasse(ancienMotDePasse, u.getMdp())) {
                addError("Le mot de passe actuel est incorrect.");
                tx.rollback(); return;
            }

            u.setMdp(hasherMotDePasse(nouveauMotDePasse));
            s.merge(u);
            tx.commit();

            /* Reset champs */
            ancienMotDePasse      = null;
            nouveauMotDePasse     = null;
            confirmationMotDePasse = null;

            addInfo("🔐 Mot de passe modifié avec succès !");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            addError("Erreur lors du changement : " + e.getMessage());
        }
    }

    /**
     * Vérification du mot de passe.
     * À adapter selon votre stratégie de hashage (BCrypt, PBKDF2, plain…).
     * Exemple minimal : comparaison plain ou BCrypt.
     */
    private boolean verifierMotDePasse(String saisi, String stocke) {
        if (stocke == null) return false;
        // Si BCrypt : return BCrypt.checkpw(saisi, stocke);
        // Sinon (MD5 / SHA / plain) :
        return stocke.equals(saisi) || stocke.equals(org.mindrot.jbcrypt.BCrypt.hashpw(saisi, stocke));
    }

    /**
     * Hashage du nouveau mot de passe.
     * Remplacez par votre propre stratégie si différente.
     */
    private String hasherMotDePasse(String clair) {
        // BCrypt recommandé :
        return org.mindrot.jbcrypt.BCrypt.hashpw(clair, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        // Si plain/MD5 : return DigestUtils.md5Hex(clair);
    }

    // ═══════════════════════════════════════════════
    //  Edition profil helpers
    // ═══════════════════════════════════════════════
    public void activerEdition() {
        telephone         = authBean.getUtilisateurConnecte().getTelephone();
        adresse           = membre != null ? membre.getAdresse() : "";
        modeEditionProfil = true;
    }
    public void annulerEdition() { modeEditionProfil = false; }

    // ═══════════════════════════════════════════════
    //  HELPERS FACES MESSAGES
    // ═══════════════════════════════════════════════
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // ═══════════════════════════════════════════════
    //  HELPERS CALCUL
    // ═══════════════════════════════════════════════
    public String getTypeAbonnementLabel() {
        if (abonnementActif == null) return "Aucun";
        return switch (abonnementActif.getType()) {
            case mensuel       -> "Mensuel";
            case annuel        -> "Annuel";
            case seance_unique -> "Séance unique";
        };
    }

    public int getProgressionAbonnement() {
        if (abonnementActif == null) return 0;
        long total  = ChronoUnit.DAYS.between(abonnementActif.getDateDebut(), abonnementActif.getDateFin());
        long ecoule = ChronoUnit.DAYS.between(abonnementActif.getDateDebut(), LocalDate.now());
        if (total <= 0) return 100;
        return (int) Math.min(100, Math.round((ecoule * 100.0) / total));
    }

    public long getJoursRestants() {
        if (abonnementActif == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), abonnementActif.getDateFin());
    }

    public boolean placesDisponibles(Integer seanceId) {
        if (seanceId == null) return false;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Seance seance = s.get(Seance.class, seanceId);
            if (seance == null) return false;
            long n = s.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.seance.id = :sid " +
                                    "AND r.statutReservation <> :ann", Long.class)
                    .setParameter("sid", seanceId)
                    .setParameter("ann", Reservation.Statut.annulee).uniqueResult();
            return n < seance.getCapaciteMaximale();
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

    // ═══════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ═══════════════════════════════════════════════
    public Membre     getMembre()               { return membre; }
    public Abonnement getAbonnementActif()       { return abonnementActif; }

    public List<Reservation> getReservationsAvenir()     { return reservationsAvenir     != null ? reservationsAvenir     : Collections.emptyList(); }
    public List<Reservation> getReservationsHistorique() { return reservationsHistorique != null ? reservationsHistorique : Collections.emptyList(); }
    public List<Seance>      getSeancesDisponibles()     { return seancesDisponibles     != null ? seancesDisponibles     : Collections.emptyList(); }
    public List<Abonnement>  getHistoriquesAbonnements() { return historiquesAbonnements != null ? historiquesAbonnements : Collections.emptyList(); }

    public long getSeancesCeMois()  { return seancesCeMois; }
    public long getTotalPresences() { return totalPresences; }
    public int  getS1() { return s1; }
    public int  getS2() { return s2; }
    public int  getS3() { return s3; }
    public int  getS4() { return s4; }

    public String  getTelephone()              { return telephone; }
    public void    setTelephone(String t)      { this.telephone = t; }
    public String  getAdresse()                { return adresse; }
    public void    setAdresse(String a)        { this.adresse = a; }
    public boolean isModeEditionProfil()       { return modeEditionProfil; }

    public String getAncienMotDePasse()               { return ancienMotDePasse; }
    public void   setAncienMotDePasse(String v)       { this.ancienMotDePasse = v; }
    public String getNouveauMotDePasse()              { return nouveauMotDePasse; }
    public void   setNouveauMotDePasse(String v)      { this.nouveauMotDePasse = v; }
    public String getConfirmationMotDePasse()         { return confirmationMotDePasse; }
    public void   setConfirmationMotDePasse(String v) { this.confirmationMotDePasse = v; }

    public Integer getReservationAannuler()          { return reservationAannuler; }
    public void    setReservationAannuler(Integer i) { this.reservationAannuler = i; }
    public Integer getSeanceAInscrire()              { return seanceAInscrire; }
    public void    setSeanceAInscrire(Integer i)     { this.seanceAInscrire = i; }
}