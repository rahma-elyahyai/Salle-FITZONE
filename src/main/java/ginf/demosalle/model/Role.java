package ginf.demosalle.model;


/**
 * Rôles possibles dans la table `utilisateur`.
 * Valeurs exactes de l'ENUM SQL : MEMBRE, COACH
 * Note : l'administrateur est géré hors de cette table (table admin séparée)
 *         ou peut être ajouté en étendant l'ENUM SQL si besoin.
 */
public enum Role {
    MEMBRE,
    COACH
}