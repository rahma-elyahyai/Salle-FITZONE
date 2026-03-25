package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "membre")
public class Membre implements Serializable {

    @Id
    @Column(name = "id_membre")
    private Integer id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_membre")
    private Utilisateur utilisateur;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "adresse", length = 255)
    private String adresse;

    public Membre() {}

    public Integer getId()                          { return id; }
    public void setId(Integer id)                   { this.id = id; }
    public Utilisateur getUtilisateur()             { return utilisateur; }
    public void setUtilisateur(Utilisateur u)       { this.utilisateur = u; }
    public LocalDate getDateNaissance()             { return dateNaissance; }
    public void setDateNaissance(LocalDate d)       { this.dateNaissance = d; }
    public String getAdresse()                      { return adresse; }
    public void setAdresse(String a)                { this.adresse = a; }
}