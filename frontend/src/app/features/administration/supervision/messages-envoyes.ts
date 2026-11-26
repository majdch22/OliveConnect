import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs';

import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';

interface MessageEnvoye {
  telephone: string;
  texte: string;
  libelleMotif: string;
  dateEnvoi: string;
}

interface PageMessages {
  elements: MessageEnvoye[];
  page: number;
  totalElements: number;
  totalPages: number;
}

/**
 * « Messages envoyés » de la supervision (décision 17) : les SMS sont simulés, rien ne part vers un opérateur.
 * Cette liste montre ce qui aurait été envoyé (activation de compte, code de mot de passe oublié, suivi des collectes).
 */
@Component({
  selector: 'app-messages-envoyes',
  imports: [DatePipe, TelephonePipe],
  template: `
    <section class="mt-8 rounded-2xl border border-gray-200 bg-white">
      <div class="border-b border-gray-200 px-6 py-4">
        <h2 class="text-lg font-semibold text-olive-900">SMS envoyés (simulés)</h2>
        <p class="text-sm text-gray-500">Aucun SMS ne part réellement : un fournisseur de SMS pourra remplacer cette simulation. Gardés 90 jours.</p>
      </div>
      @if (messages(); as resultat) {
        @if (resultat.elements.length === 0) {
          <p class="px-6 py-10 text-center text-gray-500">Aucun SMS pour l'instant.</p>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full min-w-[40rem] text-left text-sm">
              <thead class="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
                <tr>
                  <th class="px-6 py-2 font-semibold">Date</th>
                  <th class="px-4 py-2 font-semibold">Téléphone</th>
                  <th class="px-4 py-2 font-semibold">Motif</th>
                  <th class="px-6 py-2 font-semibold">Message</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                @for (message of resultat.elements; track $index) {
                  <tr>
                    <td class="whitespace-nowrap px-6 py-3 text-gray-700">{{ message.dateEnvoi | date: 'dd/MM/yyyy HH:mm' }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-gray-700">{{ message.telephone | telephone }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-gray-700">{{ message.libelleMotif }}</td>
                    <td class="px-6 py-3 text-gray-900">{{ message.texte }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (resultat.totalPages > 1) {
            <div class="flex items-center justify-between border-t border-gray-100 px-6 py-3 text-sm text-gray-600">
              <span>Page {{ resultat.page + 1 }} sur {{ resultat.totalPages }} · {{ resultat.totalElements }} SMS</span>
              <div class="flex gap-2">
                <button type="button" class="bouton-secondaire px-3 py-1.5 text-sm" [disabled]="resultat.page === 0" (click)="page.set(resultat.page - 1)">Précédente</button>
                <button type="button" class="bouton-secondaire px-3 py-1.5 text-sm" [disabled]="resultat.page + 1 >= resultat.totalPages" (click)="page.set(resultat.page + 1)">Suivante</button>
              </div>
            </div>
          }
        }
      }
    </section>
  `,
})
export class MessagesEnvoyes {
  private readonly http = inject(HttpClient);
  readonly page = signal(0);
  readonly messages = toSignal(
    toObservable(this.page).pipe(
      switchMap((page) => this.http.get<PageMessages>('/api/administration/messages-envoyes', { params: new HttpParams().set('page', page) })),
    ),
  );
}
