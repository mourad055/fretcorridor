export type CanalNotification = 'EMAIL' | 'IN_APP';

export interface NotificationItem {
  id: string;
  canal: CanalNotification;
  destinataire: string;
  objet: string;
  resume: string;
  envoyeeLe: string;
}
