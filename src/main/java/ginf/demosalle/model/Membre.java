package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membre")
public class Membre implements Serializable {

    @Id
    @Column(name = "id_membre")
    private Integer id;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_membre")
    private Utilisateur utilisateur;

    @ManyToMany(mappedBy = "membresEncadres")
    private List<Coach> coachs = new ArrayList<>();

    public Membre() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<Coach> getCoachs() {
        return coachs;
    }

    public void setCoachs(List<Coach> coachs) {
        this.coachs = coachs;
    }
}