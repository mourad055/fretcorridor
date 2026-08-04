import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MissionChronologieComponent } from './mission-chronologie.component';
import { Mission } from '../../models/mission.models';

const MISSIONS: Mission[] = [
  {
    id: 'mission-a',
    transporteurNom: 'Transport Étoile SARL',
    origine: 'Douala',
    destination: 'Yaoundé',
    etapes: [
      { rang: 1, type: 'ENLEVEMENT', lieu: 'Douala', etat: 'TERMINEE' },
      { rang: 2, type: 'LIVRAISON', lieu: 'Yaoundé', etat: 'EN_COURS' },
    ],
  },
];

describe('MissionChronologieComponent', () => {
  it('affiche une entree de chronologie par etape de mission', async () => {
    await TestBed.configureTestingModule({ imports: [MissionChronologieComponent] }).compileComponents();
    const fixture = TestBed.createComponent(MissionChronologieComponent);
    fixture.componentRef.setInput('missions', MISSIONS);
    fixture.detectChanges();

    const items = fixture.debugElement.queryAll(By.css('li'));
    expect(items).toHaveLength(2);
    expect(items[0].nativeElement.textContent).toContain('Enlèvement');
    expect(items[1].nativeElement.textContent).toContain('Livraison');
  });
});
