import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * FE-WEB-03 : réponse 403 propre, jamais une erreur technique. Aucune donnée
 * sur la ressource visée n'est affichée ni requêtée (pas de fuite réseau).
 */
@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="forbidden-screen">
      <h1>Accès non autorisé</h1>
      <p>Votre rôle ne permet pas d'accéder à cette page.</p>
      <a routerLink="/">Retour à l'accueil</a>
    </main>
  `,
})
export class ForbiddenComponent {}
