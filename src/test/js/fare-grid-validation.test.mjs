import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fareGrid from '../../main/resources/static/js/fare-grid.js';

const { validateFareInput } = fareGrid;

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