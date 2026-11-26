import { Component, computed, input } from '@angular/core';

/** États d'un compte (mêmes noms que l'énumération StatutCompte en Java). */
export type StatutCompte = 'EN_ATTENTE_VALIDATION' | 'ACTIF' | 'REFUSE' | 'DESACTIVE';

/** Libellé et couleurs de chaque état. */
const APPARENCES: Record<StatutCompte, { libelle: string; classes: string }> = {
  EN_ATTENTE_VALIDATION: { libelle: 'En attente', classes: 'bg-amber-100 text-amber-800' },
  ACTIF: { libelle: 'Actif', classes: 'bg-olive-100 text-olive-900' },
  REFUSE: { libelle: 'Refusé', classes: 'bg-red-100 text-red-800' },
  DESACTIVE: { libelle: 'Désactivé', classes: 'bg-gray-200 text-gray-700' },
};

/** Pastille colorée affichant l'état d'un compte. */
@Component({
  selector: 'app-badge-statut',
  template: `
    <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap" [class]="apparence().classes">
      {{ apparence().libelle }}
    </span>
  `,
})
export class BadgeStatut {
  readonly statut = input.required<StatutCompte>();
  readonly apparence = computed(() => APPARENCES[this.statut()]);
}

export function libelleStatut(statut: StatutCompte): string {
  return APPARENCES[statut].libelle;
}
