/** Les douze mois, pour choisir la saison de récolte d'une variété. */
export const MOIS: { numero: number; nom: string }[] = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
].map((nom, indice) => ({ numero: indice + 1, nom }));
