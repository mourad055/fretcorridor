/** Clé d'idempotence côté client (RFC 4122 v4 simplifiée) — pas de dépendance à l'API Web Crypto, qui n'est pas garantie dans tous les environnements de test/navigateurs cibles. */
export function generateIdempotencyKey(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
