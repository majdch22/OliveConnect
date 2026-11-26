import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { AdministrationService } from '../administration.service';
import { ListePressoirs } from './liste-pressoirs';
import { ListeVarietes } from './liste-varietes';

type Onglet = 'varietes' | 'pressoirs' | 'delegations' | 'categories';

/**
 * Page /administration/referentiels : les données de référence sur lesquelles reposent les autres fonctionnalités.
 * Chaque onglet explique en une phrase ce qu'est l'élément et qui l'utilise.
 */
@Component({
  selector: 'app-referentiels-administration',
  imports: [ListeVarietes, ListePressoirs],
  templateUrl: './referentiels-administration.html',
})
export class ReferentielsAdministration {
  private readonly administration = inject(AdministrationService);

  readonly onglets: { code: Onglet; libelle: string; explication: string; modifiable: boolean }[] = [
    {
      code: 'varietes',
      libelle: 'Variétés',
      explication:
        "Variétés d'olives cultivées dans les vergers. Leur rendement moyen sert à proposer une quantité estimée à l'agriculteur et à comparer les rendements réels.",
      modifiable: true,
    },
    {
      code: 'pressoirs',
      libelle: 'Pressoirs',
      explication:
        'Pressoirs partenaires qui reçoivent les olives. Chaque tournée a un pressoir de destination, et chaque responsable du pressoir est rattaché à un pressoir.',
      modifiable: true,
    },
    {
      code: 'delegations',
      libelle: 'Délégations',
      explication:
        "Les 16 délégations du gouvernorat de Sfax, zone d'activité de la coopérative. Chaque agriculteur et chaque pressoir appartient à une délégation.",
      modifiable: false,
    },
    {
      code: 'categories',
      libelle: 'Catégories de qualité',
      explication:
        "Catégories de l'huile selon l'acidité mesurée au pressoir (norme du Conseil oléicole international). La catégorie est déduite de l'acidité.",
      modifiable: false,
    },
  ];

  readonly ongletActif = signal<Onglet>('varietes');
  readonly ongletCourant = computed(() => this.onglets.find((onglet) => onglet.code === this.ongletActif())!);

  readonly delegations = toSignal(this.administration.delegations(), { initialValue: [] });
  readonly categories = toSignal(this.administration.categoriesQualite(), { initialValue: [] });
}
