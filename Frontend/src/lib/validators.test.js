import { describe, expect, it } from 'vitest';
import { validateEmail, validateIBAN, validateTaxNumberByType, validateTCKN, validateVKN } from './validators';

describe('validators', () => {
  // Validates Turkish IBAN format and checksum.
  it('validateIBAN accepts valid Turkish IBANs and rejects malformed values', () => {
    expect(validateIBAN('TR33 0006 1005 1978 6457 8413 26')).toEqual({ ok: true });
    expect(validateIBAN('TR123')).toMatchObject({ ok: false });
  });

  // Validates TCKN using length, first digit, and checksum rules.
  it('validateTCKN enforces Turkish identity number checksum', () => {
    expect(validateTCKN('10000000146')).toEqual({ ok: true });
    expect(validateTCKN('00000000000')).toMatchObject({ ok: false });
    expect(validateTCKN('123')).toMatchObject({ ok: false });
  });

  // Routes tax number validation by customer type.
  it('validateTaxNumberByType delegates by customer type', () => {
    expect(validateTaxNumberByType('INDIVIDUAL', '10000000146')).toEqual({ ok: true });
    expect(validateTaxNumberByType('CORPORATE', '1234567890')).toEqual({ ok: true });
    expect(validateTaxNumberByType('', '1234567890')).toMatchObject({ ok: false });
  });

  // Validates simple VKN and email shapes.
  it('validates VKN and email shape', () => {
    expect(validateVKN('1234567890')).toEqual({ ok: true });
    expect(validateVKN('123')).toMatchObject({ ok: false });
    expect(validateEmail('test@example.com')).toEqual({ ok: true });
    expect(validateEmail('bad-email')).toMatchObject({ ok: false });
  });
});
