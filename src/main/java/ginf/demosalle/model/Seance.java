package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "seance")
public class Seance implements Serializable {

    public enum Categorie { cardio, yoga, musculation, danse, stretching }
    public enum Statut    { planifiee, annulee, terminee }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seance")
    private Integer id;

    @Column(name = "titre", nullable = false, length = 150)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(name = "duree", nullable = false)
    private Integer duree;

    @Column(name = "capacite_maximale", nullable = false)
    private Integer capaciteMaximale;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private Statut statut = Statut.planifiee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_salle", nullable = false)
    private Salle salle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_coach", nullable = false)
    private Coach coach;

    public Seance() {}

    public Integer getId()                          { return id; }
    public String getTitre()                        { return titre; }
    public void setTitre(String t)                  { this.titre = t; }
    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }
    public LocalDateTime getDateHeure()             { return dateHeure; }
    public void setDateHeure(LocalDateTime dh)      { this.dateHeure = dh; }
    public Integer getDuree()                       { return duree; }
    public void setDuree(Integer d)                 { this.duree = d; }
    public Integer getCapaciteMaximale()            { return capaciteMaximale; }
    public void setCapaciteMaximale(Integer c)      { this.capaciteMaximale = c; }
    public Categorie getCategorie()                 { return categorie; }
    public void setCategorie(Categorie c)           { this.categorie = c; }
    public Statut getStatut()                       { return statut; }
    public void setStatut(Statut s)                 { this.statut = s; }
    public Salle getSalle()                         { return salle; }
    public void setSalle(Salle s)                   { this.salle = s; }
    public Coach getCoach()                         { return coach; }
    public void setCoach(Coach c)                   { this.coach = c; }
}