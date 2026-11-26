import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Equipe, ETATS_RESSOURCES, Ressource } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

/**
 * Panneau d'une équipe : nom (création ou renommage), liste des ouvriers avec leur état, archivage.
 * Les ouvriers changent d'équipe depuis leur propre fiche.
 */
@Component({
  selector: 'app-fiche-equipe',
  imports: [ReactiveFormsModule],
  template: `
    <form (ngSubmit)="enregistrer()" novalidate class="flex flex-col gap-1">
      <label for="equipe-nom" class="etiquette">Nom de l'équipe</label>
      <div class="flex gap-3">
        <input id="equipe-nom" [formControl]="nom" class="champ-saisie flex-1" [class.en-erreur]="erreurNom()" placeholder="Équipe C" />
        <button type="submit" class="bouton-principal" [disabled]="envoiEnCours()">{{ equipe() ? 'Renommer' : 'Créer' }}</button>
      </div>
      @if (erreurNom(); as message) { <p class="message-champ">{{ message }}</p> }
    </form>

    @if (messageErreur(); as message) {
      <p role="alert" class="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
    }

    @if (equipe(); as equipeOuverte) {
      <section class="mt-8">
        <h3 class="font-semibold text-gray-900">
          Ouvriers de l'équipe
          <span class="font-normal text-gray-500">· {{ equipeOuverte.nombreDisponibles }} disponible(s) sur {{ equipeOuverte.nombreMembres }}</span>
        </h3>
        @if (membres().length === 0) {
          <p class="mt-3 text-sm text-gray-500">Aucun ouvrier. Choisissez cette équipe dans la fiche d'un ouvrier pour l'y ajouter.</p>
        } @else {
          <ul class="mt-3 divide-y divide-gray-100 rounded-xl border border-gray-200">
            @for (membre of membres(); track membre.id) {
              <li class="flex items-center justify-between px-4 py-3 text-sm">
                <span><span class="font-mono text-gray-500">{{ membre.code }}</span> · {{ membre.nom }}</span>
                <span class="rounded-full px-2.5 py-1 text-xs font-semibold" [class]="classesEtat(membre)">{{ membre.libelleEtat }}</span>
              </li>
            }
          </ul>
        }
      </section>

      <div class="mt-8 border-t border-gray-200 pt-6">
        <p class="text-sm text-gray-600">
          {{ equipeOuverte.archivee
            ? 'Cette équipe est archivée : elle n’est plus proposée pour les tournées.'
            : equipeOuverte.nombreMembres > 0
              ? 'Changez d’abord ses ouvriers d’équipe pour pouvoir l’archiver.'
              : 'Archiver retire l’équipe des choix proposés, sans effacer l’historique des tournées.' }}
        </p>
        <button type="button" class="bouton-secondaire mt-3 w-full" (click)="changerArchivage(equipeOuverte)"
          [disabled]="!equipeOuverte.archivee && equipeOuverte.nombreMembres > 0">
          {{ equipeOuverte.archivee ? 'Restaurer l’équipe' : 'Archiver l’équipe' }}
        </button>
      </div>
    }
  `,
})
export class FicheEquipe implements OnInit {
  private readonly administration = inject(AdministrationService);

  readonly equipe = input<Equipe | null>(null);
  readonly change = output<Equipe>();

  readonly nom = new FormControl('', { nonNullable: true, validators: Validators.required });
  readonly membres = signal<Ressource[]>([]);
  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreurServeurNom = signal<string | null>(null);

  ngOnInit(): void {
    const equipe = this.equipe();
    if (equipe) {
      this.nom.setValue(equipe.nom);
      this.administration.membresEquipe(equipe.id).subscribe((membres) => this.membres.set(membres));
    }
  }

  erreurNom(): string | null {
    return this.erreurServeurNom() ?? (this.nom.touched && this.nom.invalid ? "Saisissez le nom de l'équipe." : null);
  }

  classesEtat(membre: Ressource): string {
    return ETATS_RESSOURCES.find((etat) => etat.code === membre.etat)?.classes ?? '';
  }

  enregistrer(): void {
    this.erreurServeurNom.set(null);
    this.messageErreur.set(null);
    if (this.nom.invalid) {
      this.nom.markAsTouched();
      return;
    }
    this.envoiEnCours.set(true);
    this.administration.enregistrerEquipe(this.equipe()?.id ?? null, this.nom.value.trim()).subscribe({
      next: (equipe) => {
        this.envoiEnCours.set(false);
        this.change.emit(equipe);
      },
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  changerArchivage(equipe: Equipe): void {
    this.messageErreur.set(null);
    this.administration.changerArchivageEquipe(equipe.id, !equipe.archivee).subscribe({
      next: (modifiee) => this.change.emit(modifiee),
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  private echec(erreur: HttpErrorResponse): void {
    this.envoiEnCours.set(false);
    this.erreurServeurNom.set(erreursParChamp(erreur)['nom'] ?? null);
    this.messageErreur.set(texteErreur(erreur));
  }
}
