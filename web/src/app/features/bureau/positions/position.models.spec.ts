import { formatAge } from './position.models';

describe('formatAge', () => {
  it('formats an age below one minute as instantaneous', () => {
    expect(formatAge(0)).toBe("à l'instant");
    expect(formatAge(59)).toBe("à l'instant");
  });

  it('formats an age in minutes below one hour', () => {
    expect(formatAge(90)).toBe('il y a 1 min');
    expect(formatAge(3599)).toBe('il y a 59 min');
  });

  it('formats an age in hours beyond one hour', () => {
    expect(formatAge(3600)).toBe('il y a 1 h');
    expect(formatAge(7260)).toBe('il y a 2 h');
  });
});
