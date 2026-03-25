package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "coach")
public class Coach implements Serializable {

    @Id
    @Column(name = "id_coach")
    private Integer id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_coach")
    private Utilisateur utilisateur;

    @Column(name = "specialite", nullable = false, length = 100)
    private String specialite;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public Coach() {}

    public Integer getId()                    { return id; }
    public void setId(Integer id)             { this.id = id; }
    public Utilisateur getUtilisateur()       { return utilisateur; }
    public void setUtilisateur(Utilisateur u) { this.utilisateur = u; }
    public String getSpecialite()             { return specialite; }
    public void setSpecialite(String s)       { this.specialite = s; }
    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }
}