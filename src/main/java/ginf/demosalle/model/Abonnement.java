package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "abonnement")
public class Abonnement implements Serializable {

    public enum Type    { mensuel, annuel, seance_unique }
    public enum Statut  { actif, expire, renouvele }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_abonnement")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private Statut statut = Statut.actif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_membre", nullable = false)
    private Membre membre;

    public Abonnement() {}

    public Integer getId()              { return id; }
    public Type getType()               { return type; }
    public void setType(Type t)         { this.type = t; }
    public LocalDate getDateDebut()     { return dateDebut; }
    public void setDateDebut(LocalDate d){ this.dateDebut = d; }
    public LocalDate getDateFin()       { return dateFin; }
    public void setDateFin(LocalDate d) { this.dateFin = d; }
    public BigDecimal getPrix()         { return prix; }
    public void setPrix(BigDecimal p)   { this.prix = p; }
    public Statut getStatut()           { return statut; }
    public void setStatut(Statut s)     { this.statut = s; }
    public Membre getMembre()           { return membre; }
    public void setMembre(Membre m)     { this.membre = m; }
}