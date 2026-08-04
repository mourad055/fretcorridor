import { decodeJwtPayload } from './jwt.util';

function fakeJwt(payload: object): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'HS256' })}.${base64url(payload)}.signature`;
}

describe('decodeJwtPayload', () => {
  it('decodes a well-formed JWT payload', () => {
    const token = fakeJwt({ sub: 'actor-1', role: 'BUREAU', tenantId: 'tenant-1' });

    const claims = decodeJwtPayload<{ sub: string; role: string; tenantId: string }>(token);

    expect(claims).toEqual({ sub: 'actor-1', role: 'BUREAU', tenantId: 'tenant-1' });
  });

  it('returns null for a malformed token', () => {
    expect(decodeJwtPayload('not-a-jwt')).toBeNull();
  });

  it('returns null when the payload segment is not valid base64/JSON', () => {
    expect(decodeJwtPayload('header.%%%invalid%%%.signature')).toBeNull();
  });
});
