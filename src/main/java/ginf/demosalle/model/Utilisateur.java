package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
public class Utilisateur implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Integer idUtilisateur;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "mdp", nullable = false, length = 255)
    private String mdp;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "photo", length = 255)
    private String photo;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email, String mdp, Role role) {
        this.nom    = nom;
        this.prenom = prenom;
        this.email  = email;
        this.mdp    = mdp;
        this.role   = role;
    }

    /** Initiales pour avatar (ex: "KA" pour Karim Alami) */
    public String getInitiales() {
        String n = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        String m = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
        return n + m;
    }

    public Integer getIdUtilisateur()               { return idUtilisateur; }
    public void setIdUtilisateur(Integer i)         { this.idUtilisateur = i; }
    public String getNom()                          { return nom; }
    public void setNom(String n)                    { this.nom = n; }
    public String getPrenom()                       { return prenom; }
    public void setPrenom(String p)                 { this.prenom = p; }
    public String getEmail()                        { return email; }
    public void setEmail(String e)                  { this.email = e; }
    public Role getRole()                           { return role; }
    public void setRole(Role r)                     { this.role = r; }
    public String getMdp()                          { return mdp; }
    public void setMdp(String m)                    { this.mdp = m; }
    public String getTelephone()                    { return telephone; }
    public void setTelephone(String t)              { this.telephone = t; }
    public LocalDateTime getDateCreation()          { return dateCreation; }
    public void setDateCreation(LocalDateTime d)    { this.dateCreation = d; }
    public String getPhoto()                        { return photo; }
    public void setPhoto(String p)                  { this.photo = p; }
}