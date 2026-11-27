import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, map, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import {
  aujourdhui,
  DemandeCollecte,
  EquipeDisponible,
  ICONES_RESSOURCE,
  OptionsPlanification,
  PlanificationRequete,
  RessourceDisponible,
  TypeRessource,
  Verification,
} from '../collecte.modeles';
import { CollecteService } from '../collecte.service';

const ORDRE_TYPES: TypeRessource[] = ['TRACTEUR', 'BENNE', 'OUVRIER', 'MATERIEL'];

/**
 * Page /collecte/planifier (maquette D1) : nouvelle tournée.
 * 1. Cocher des demandes validées (urgentes puis plus mûres d'abord) : l'ordre des coches est l'ordre de passage.
 * 2. Choisir la date : seuls les chefs d'équipe et les ressources libres ce jour-là sont proposés.
 * 3. Choisir le chef, le pressoir (capacité restante affichée) et les ressources ; une équipe s'affecte en un clic.
 * À chaque changement, le serveur contrôle la tournée : conflits (bloquants) et avertissements (capacité des bennes,
 * pressoir saturé avec d'autres pressoirs proposés…). « Créer la tournée » prévient agriculteurs et chef d'équipe.
 */
@Component({
  selector: 'app-planifier-tournee',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './planifier-tournee.html',
})
export class PlanifierTournee {
  private readonly service = inject(CollecteService);
  private readonly router = inject(Router);
  private readonly parametres = inject(ActivatedRoute).snapshot.queryParamMap;

  readonly icones = ICONES_RESSOURCE;
  readonly dateMinimum = aujourdhui();

  // --- Demandes validées ---

  readonly messageErreur = signal<string | null>(null);
  readonly demandesValidees = toSignal(
    this.service.demandes({ statut: 'VALIDEE', type: null, texte: '', page: 0, taille: 50 }).pipe(
      map((page) => page.elements),
      catchError((erreur: HttpErrorResponse) => {
        this.messageErreur.set(texteErreur(erreur));
        return of([] as DemandeCollecte[] | null);
      }),
    ),
    { initialValue: null as DemandeCollecte[] | null },
  );

  /** Demandes cochées, dans l'ordre de passage. */
  readonly demandesChoisies = signal<string[]>(this.parametres.get('demandes')?.split(',').filter(Boolean) ?? []);
  readonly quantiteChoisie = computed(() =>
    (this.demandesValidees() ?? []).filter((demande) => this.demandesChoisies().includes(demande.id))
      .reduce((total, demande) => total + demande.quantiteEstimeeKg, 0),
  );

  basculerDemande(demande: DemandeCollecte): void {
    this.demandesChoisies.update((choisies) =>
      choisies.includes(demande.id) ? choisies.filter((id) => id !== demande.id) : [...choisies, demande.id],
    );
    // La première demande cochée propose sa date souhaitée
    if (this.demandesChoisies().length === 1 && !this.dateChoisieALaMain) {
      this.date.set(demande.dateSouhaitee < this.dateMinimum ? this.dateMinimum : demande.dateSouhaitee);
    }
  }

  rang(demande: DemandeCollecte): number {
    return this.demandesChoisies().indexOf(demande.id) + 1;
  }

  // --- Date, chef, pressoir, ressources ---

  readonly date = signal(this.parametres.get('date') && this.parametres.get('date')! >= aujourdhui() ? this.parametres.get('date')! : aujourdhui(1));
  private dateChoisieALaMain = !!this.parametres.get('date');
  readonly chefEquipeId = signal('');
  readonly pressoirId = signal('');
  readonly ressourcesChoisies = signal<string[]>([]);
  readonly commentaire = signal('');

  readonly options = toSignal(
    toObservable(this.date).pipe(
      switchMap((date) => (date ? this.service.optionsPlanification(date).pipe(catchError(() => of(null))) : of(null))),
    ),
    { initialValue: null as OptionsPlanification | null },
  );

  readonly ressourcesParType = computed(() => {
    const ressources = this.options()?.ressources ?? [];
    return ORDRE_TYPES.map((type) => ({
      type,
      libelle: ressources.find((ressource) => ressource.type === type)?.libelleType ?? type,
      ressources: ressources.filter((ressource) => ressource.type === type && (type !== 'OUVRIER' || !ressource.equipeId)),
    })).filter((groupe) => groupe.ressources.length > 0);
  });

  constructor() {
    // Nouvelle date : on oublie ce qui n'est plus libre ce jour-là
    effect(() => {
      const options = this.options();
      if (!options) return;
      const libres = new Set(options.ressources.map((ressource) => ressource.id));
      this.ressourcesChoisies.update((choisies) => choisies.filter((id) => libres.has(id)));
      if (!options.chefs.some((chef) => chef.id === this.chefEquipeId())) this.chefEquipeId.set('');
    });
  }

  changerDate(date: string): void {
    this.dateChoisieALaMain = true;
    this.date.set(date);
  }

  basculerRessource(ressource: RessourceDisponible): void {
    this.ressourcesChoisies.update((choisies) =>
      choisies.includes(ressource.id) ? choisies.filter((id) => id !== ressource.id) : [...choisies, ressource.id],
    );
  }

  estChoisie(id: string): boolean {
    return this.ressourcesChoisies().includes(id);
  }

  /** Équipe entièrement affectée (tous ses ouvriers disponibles sont cochés) ? */
  equipeAffectee(equipe: EquipeDisponible): boolean {
    return equipe.ouvriersDisponibles.length > 0 && equipe.ouvriersDisponibles.every((id) => this.estChoisie(id));
  }

  /** Affecte en un clic les ouvriers disponibles de l'équipe, ou les retire. */
  basculerEquipe(equipe: EquipeDisponible): void {
    const affectee = this.equipeAffectee(equipe);
    this.ressourcesChoisies.update((choisies) =>
      affectee
        ? choisies.filter((id) => !equipe.ouvriersDisponibles.includes(id))
        : [...choisies, ...equipe.ouvriersDisponibles.filter((id) => !choisies.includes(id))],
    );
  }

  ouvriersAffectes(): number {
    const ouvriers = new Set((this.options()?.ressources ?? []).filter((ressource) => ressource.type === 'OUVRIER').map((ressource) => ressource.id));
    return this.ressourcesChoisies().filter((id) => ouvriers.has(id)).length;
  }

  // --- Contrôle par le serveur ---

  private readonly requete = computed<PlanificationRequete | null>(() => {
    if (!this.date() || this.demandesChoisies().length === 0 || !this.chefEquipeId() || !this.pressoirId()) {
      return null;
    }
    return {
      date: this.date(),
      demandeIds: this.demandesChoisies(),
      chefEquipeId: this.chefEquipeId(),
      pressoirId: this.pressoirId(),
      ressourceIds: this.ressourcesChoisies(),
      commentaire: this.commentaire().trim() || null,
    };
  });

  readonly erreurVerification = signal<string | null>(null);
  readonly verification = toSignal(
    toObservable(this.requete).pipe(
      debounceTime(300),
      switchMap((requete) => {
        this.erreurVerification.set(null);
        return requete
          ? this.service.verifier(requete).pipe(
              catchError((erreur: HttpErrorResponse) => {
                this.erreurVerification.set(texteErreur(erreur));
                return of(null);
              }),
            )
          : of(null);
      }),
    ),
    { initialValue: null as Verification | null },
  );

  readonly creable = computed(() => !!this.requete() && !!this.verification() && this.verification()!.conflits.length === 0);

  readonly envoiEnCours = signal(false);
  readonly erreurCreation = signal<string | null>(null);

  creer(): void {
    const requete = this.requete();
    if (!requete || !this.creable()) return;
    this.envoiEnCours.set(true);
    this.erreurCreation.set(null);
    this.service.creerTournee(requete).subscribe({
      next: (tournee) =>
        this.router.navigate(['/collecte/tournees'], { queryParams: { date: tournee.date, tournee: tournee.id, creee: tournee.numero } }),
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreurCreation.set(texteErreur(erreur));
      },
    });
  }
}
