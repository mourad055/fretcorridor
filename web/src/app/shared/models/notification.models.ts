export type CanalNotification = 'EMAIL';

export interface NotificationItem {
  id: string;
  canal: CanalNotification;
  destinataire: string;
  objet: string;
  resume: string;
  envoyeeLe: string;
}
