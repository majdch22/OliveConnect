package tn.oliveconnect.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Une page d'une liste filtrée et triée, avec de quoi afficher la pagination.
 *
 * @param elements      éléments de la page
 * @param page          numéro de la page (0 pour la première)
 * @param taille        nombre d'éléments par page
 * @param totalElements nombre total d'éléments qui correspondent aux filtres
 * @param totalPages    nombre total de pages
 */
public record PageResultat<T>(List<T> elements, int page, int taille, long totalElements, int totalPages) {

    public static <E, T> PageResultat<T> depuis(Page<E> page, Function<E, T> conversion) {
        return new PageResultat<>(page.getContent().stream().map(conversion).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
