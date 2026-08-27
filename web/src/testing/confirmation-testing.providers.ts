import { ConfirmationService } from '../app/shared/services/confirmation.service';

/** Mock du ConfirmationService (modale async) pour les tests unitaires. */
export function provideConfirmationServiceForTests(confirme = true) {
  return {
    provide: ConfirmationService,
    useValue: {
      confirmer: jest.fn().mockResolvedValue(confirme),
      repondre: jest.fn(),
    },
  };
}
