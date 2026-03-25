package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "salle")
public class Salle implements Serializable {

    public enum TypeSalle { cardio, yoga, musculation, danse, stretching }
    public enum Disponible { oui, non }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salle")
    private Integer id;

    @Column(name = "nom_salle", nullable = false, length = 100)
    private String nomSalle;

    @Column(name = "capacite", nullable = false)
    private Integer capacite;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_salle", nullable = false)
    private TypeSalle typeSalle;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponible")
    private Disponible disponible = Disponible.oui;

    public Salle() {}

    public Integer getId()                  { return id; }
    public String getNomSalle()             { return nomSalle; }
    public void setNomSalle(String n)       { this.nomSalle = n; }
    public Integer getCapacite()            { return capacite; }
    public void setCapacite(Integer c)      { this.capacite = c; }
    public TypeSalle getTypeSalle()         { return typeSalle; }
    public void setTypeSalle(TypeSalle t)   { this.typeSalle = t; }
    public Disponible getDisponible()       { return disponible; }
    public void setDisponible(Disponible d) { this.disponible = d; }
}