import { TestBed } from '@angular/core/testing';
import { ConfirmationService } from './confirmation.service';

describe('ConfirmationService', () => {
  let service: ConfirmationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfirmationService);
  });

  it('resout la promesse quand l utilisateur confirme', async () => {
    const promesse = service.confirmer('Continuer ?');
    expect(service.demande()?.message).toBe('Continuer ?');

    service.repondre(true);
    await expect(promesse).resolves.toBe(true);
    expect(service.demande()).toBeNull();
  });

  it('resout false quand l utilisateur annule', async () => {
    const promesse = service.confirmer('Continuer ?');
    service.repondre(false);
    await expect(promesse).resolves.toBe(false);
  });
});
