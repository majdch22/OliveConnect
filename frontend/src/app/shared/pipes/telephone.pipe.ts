import { Pipe, PipeTransform } from '@angular/core';

/**
 * Affiche un numéro tunisien par groupes, plus facile à lire et à dicter :
 * « +21655000210 » devient « +216 55 000 210 ». Tout autre format est affiché tel quel.
 */
@Pipe({ name: 'telephone' })
export class TelephonePipe implements PipeTransform {
  transform(numero: string | null | undefined): string {
    if (!numero) {
      return '';
    }
    const morceaux = /^\+216(\d{2})(\d{3})(\d{3})$/.exec(numero);
    return morceaux ? `+216 ${morceaux[1]} ${morceaux[2]} ${morceaux[3]}` : numero;
  }
}
