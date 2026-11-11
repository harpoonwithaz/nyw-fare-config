import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

// Validation logic matching fare-grid.js
const CURRENCY_REGEX = /^\d+(\.\d{1,2})?$/;
const MAX_FARE_AMOUNT = 9999.99;

function validateFareInput(rawString) {
  const trimmed = String(rawString ?? '').trim();
  if (trimmed === '') return { valid: false, reason: 'Fare cannot be blank.' };
  if (!CURRENCY_REGEX.test(trimmed)) return { valid: false, reason: 'Invalid format.' };

  const num = Number(trimmed);
  if (!Number.isFinite(num) || num < 0) return { valid: false, reason: 'Must be positive.' };
  if (num > MAX_FARE_AMOUNT) return { valid: false, reason: 'Exceeds maximum fare.' };

  return { valid: true, normalized: num.toFixed(2) };
}

describe('Story 2.2 Keystroke & Currency Validation Rules', () => {
  it('accepts standard positive currency values', () => {
    assert.equal(validateFareInput('14.00').valid, true);
    assert.equal(validateFareInput('13.25').normalized, '13.25');
    assert.equal(validateFareInput('0.01').valid, true);
    assert.equal(validateFareInput('0.00').valid, true);
  });

  it('normalizes whole numbers and single decimal places', () => {
    assert.equal(validateFareInput('14').normalized, '14.00');
    assert.equal(validateFareInput('14.5').normalized, '14.50');
    assert.equal(validateFareInput('0014.50').normalized, '14.50');
  });

  it('rejects negative numbers and symbols blocked by keystroke masking', () => {
    assert.equal(validateFareInput('-12.50').valid, false);
    assert.equal(validateFareInput('+14.00').valid, false);
    assert.equal(validateFareInput('$14.00').valid, false);
  });

  it('rejects letters and scientific notation', () => {
    assert.equal(validateFareInput('abc').valid, false);
    assert.equal(validateFareInput('14e2').valid, false);
    assert.equal(validateFareInput('14.00a').valid, false);
  });

  it('rejects values exceeding two decimal places', () => {
    assert.equal(validateFareInput('13.999').valid, false);
    assert.equal(validateFareInput('14.555').valid, false);
  });

  it('rejects empty or whitespace-only inputs', () => {
    assert.equal(validateFareInput('').valid, false);
    assert.equal(validateFareInput('   ').valid, false);
  });

  it('rejects amounts above the system maximum', () => {
    assert.equal(validateFareInput('10000.00').valid, false);
  });
});