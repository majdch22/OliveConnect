package tn.oliveconnect.utilisateur;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux comptes. Les requêtes sont générées par Spring Data à partir du nom des méthodes. */
public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByTelephone(String telephone);

    boolean existsByRole(Role role);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    /** Recherche dans le sous-document « agriculteur » (champ agriculteur.cin). */
    boolean existsByAgriculteurCin(String cin);

    long countByStatut(StatutCompte statut);

    long countByRoleAndStatut(Role role, StatutCompte statut);

    long countByPressoirId(String pressoirId);
}
