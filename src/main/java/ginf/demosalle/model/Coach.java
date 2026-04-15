package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "coach")
public class Coach implements Serializable {

    @Id
    @Column(name = "id_coach")
    private Integer id;

    @Column(name = "specialite", nullable = false, length = 100)
    private String specialite;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_coach")
    private Utilisateur utilisateur;

    @ManyToMany
    @JoinTable(
            name = "encadre",
            joinColumns = @JoinColumn(name = "id_coach"),
            inverseJoinColumns = @JoinColumn(name = "id_membre")
    )
    private List<Membre> membresEncadres = new ArrayList<>();

    public Coach() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<Membre> getMembresEncadres() {
        return membresEncadres;
    }

    public void setMembresEncadres(List<Membre> membresEncadres) {
        this.membresEncadres = membresEncadres;
    }
}