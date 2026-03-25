package ginf.demosalle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation implements Serializable {

    public enum Statut { confirmee, annulee, en_attente, terminee }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservation")
    private Integer id;

    @Column(name = "date_reservation")
    private LocalDateTime dateReservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_reservation")
    private Statut statutReservation = Statut.en_attente;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seance", nullable = false)
    private Seance seance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_membre", nullable = false)
    private Membre membre;

    public Reservation() {}

    public Integer getId()                              { return id; }
    public LocalDateTime getDateReservation()           { return dateReservation; }
    public void setDateReservation(LocalDateTime d)     { this.dateReservation = d; }
    public Statut getStatutReservation()                { return statutReservation; }
    public void setStatutReservation(Statut s)          { this.statutReservation = s; }
    public LocalDateTime getDateAnnulation()            { return dateAnnulation; }
    public void setDateAnnulation(LocalDateTime d)      { this.dateAnnulation = d; }
    public Seance getSeance()                           { return seance; }
    public void setSeance(Seance s)                     { this.seance = s; }
    public Membre getMembre()                           { return membre; }
    public void setMembre(Membre m)                     { this.membre = m; }
}