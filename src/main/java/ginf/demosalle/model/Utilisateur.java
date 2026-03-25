package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entité JPA mappée sur la table `utilisateur`.
 * Structure exacte du SQL fourni :
 *   id_utilisateur, nom, prenom, email, role (MEMBRE|COACH), mdp, telephone, date_creation, photo
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Integer id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "ENUM('MEMBRE','COACH')")
    private Role role;

    @Column(name = "mdp", nullable = false, length = 255)
    private String mdp;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "photo", length = 255)
    private String photo;

    // ── Constructeurs ──────────────────────────────────────────────────────────

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email, String mdp, Role role) {
        this.nom          = nom;
        this.prenom       = prenom;
        this.email        = email;
        this.mdp          = mdp;
        this.role         = role;
        this.dateCreation = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }
    // Dans Utilisateur.java
    public String getInitiales() {
        return (prenom != null && !prenom.isEmpty() ? String.valueOf(prenom.charAt(0)) : "")
                + (nom    != null && !nom.isEmpty()    ? String.valueOf(nom.charAt(0))    : "");
    }
    // ── Helper ─────────────────────────────────────────────────────────────────

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Integer getId()                       { return id; }
    public void setId(Integer id)                { this.id = id; }

    public String getNom()                       { return nom; }
    public void setNom(String nom)               { this.nom = nom; }

    public String getPrenom()                    { return prenom; }
    public void setPrenom(String prenom)         { this.prenom = prenom; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public Role getRole()                        { return role; }
    public void setRole(Role role)               { this.role = role; }

    public String getMdp()                       { return mdp; }
    public void setMdp(String mdp)               { this.mdp = mdp; }

    public String getTelephone()                 { return telephone; }
    public void setTelephone(String telephone)   { this.telephone = telephone; }

    public LocalDateTime getDateCreation()       { return dateCreation; }
    public void setDateCreation(LocalDateTime d) { this.dateCreation = d; }

    public String getPhoto()                     { return photo; }
    public void setPhoto(String photo)           { this.photo = photo; }
}