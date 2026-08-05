import { Ecriture } from '../../../shared/models/ecriture.models';

export interface SoldeTransporteur {
  solde: number;
  historique: Ecriture[];
}
