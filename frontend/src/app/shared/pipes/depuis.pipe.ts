import { formatDate } from '@angular/common';
import { inject, LOCALE_ID, Pipe, PipeTransform } from '@angular/core';

/**
 * Date relative, comme dans les notifications d'un téléphone :
 * « à l'instant », « il y a 5 min », « il y a 3 h », « hier », puis la date (20/11).
 */
@Pipe({ name: 'depuis' })
export class DepuisPipe implements PipeTransform {
  private readonly langue = inject(LOCALE_ID);

  transform(date: string | null | undefined, maintenant: Date = new Date()): string {
    if (!date) {
      return '';
    }
    const minutes = Math.floor((maintenant.getTime() - new Date(date).getTime()) / 60000);
    if (minutes < 1) {
      return "à l'instant";
    }
    if (minutes < 60) {
      return `il y a ${minutes} min`;
    }
    if (minutes < 24 * 60) {
      return `il y a ${Math.floor(minutes / 60)} h`;
    }
    if (minutes < 48 * 60) {
      return 'hier';
    }
    return formatDate(date, 'dd/MM', this.langue);
  }
}
