import { TestBed } from '@angular/core/testing';
import { ConfirmationService } from './confirmation.service';

describe('ConfirmationService', () => {
  let service: ConfirmationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfirmationService);
  });

  it('relaie la reponse de window.confirm', () => {
    const spy = jest.spyOn(window, 'confirm').mockReturnValue(true);

    expect(service.confirmer('Continuer ?')).toBe(true);
    expect(spy).toHaveBeenCalledWith('Continuer ?');

    spy.mockReturnValue(false);
    expect(service.confirmer('Continuer ?')).toBe(false);

    spy.mockRestore();
  });
});
