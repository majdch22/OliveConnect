/** Plus grand côté d'une photo envoyée, en pixels : assez pour voir des olives ou des dégâts, léger sur le réseau du verger. */
const COTE_MAXIMUM = 1600;
const QUALITE_JPEG = 0.8;

/**
 * Réduit une photo sur le téléphone avant l'envoi (décision 15) : redimensionnée à 1600 pixels au plus
 * et réenregistrée en JPEG. Une photo de 4 Mo pèse alors environ 300 Ko.
 * Refuse ce qui n'est pas une image.
 */
export async function reduirePhoto(fichier: File): Promise<Blob> {
  if (!fichier.type.startsWith('image/')) {
    throw new Error('Choisissez une photo (JPEG, PNG ou WebP).');
  }
  const image = await createImageBitmap(fichier);
  const echelle = Math.min(1, COTE_MAXIMUM / Math.max(image.width, image.height));
  const toile = document.createElement('canvas');
  toile.width = Math.round(image.width * echelle);
  toile.height = Math.round(image.height * echelle);
  toile.getContext('2d')!.drawImage(image, 0, 0, toile.width, toile.height);
  image.close();
  return new Promise((resoudre, rejeter) =>
    toile.toBlob((resultat) => (resultat ? resoudre(resultat) : rejeter(new Error('La photo n’a pas pu être préparée.'))), 'image/jpeg', QUALITE_JPEG),
  );
}
