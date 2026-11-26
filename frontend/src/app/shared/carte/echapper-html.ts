/**
 * Rend un texte venu de la base sans danger dans une bulle de carte (Leaflet attend du HTML) :
 * les caractères spéciaux deviennent des entités, aucun script ne peut s'y glisser.
 */
export function echapperHtml(texte: string): string {
  return texte.replace(/[&<>"']/g, (caractere) => `&#${caractere.charCodeAt(0)};`);
}
