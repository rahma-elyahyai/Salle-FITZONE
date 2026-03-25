package ginf.demosalle.dao;

import ginf.demosalle.model.Salle;
import ginf.demosalle.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class SalleDAO {

    public List<Salle> findAll() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery("FROM Salle ORDER BY nomSalle", Salle.class).list();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public Salle findById(Integer id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Salle.class, id);
        }
    }
}