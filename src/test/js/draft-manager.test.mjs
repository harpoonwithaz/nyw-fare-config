import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const DraftStateManager = require('../../main/resources/static/js/draft-manager.js');

describe('DraftStateManager Unit Tests', () => {
  beforeEach(() => {
    delete globalThis.document;
    DraftStateManager.discardAll();
  });

  it('starts with an empty draft state (count = 0, isDirty = false)', () => {
    assert.equal(DraftStateManager.getDraftCount(), 0);
    assert.equal(DraftStateManager.isDirty(), false);
    assert.deepEqual(DraftStateManager.getPendingChanges(), []);
  });

  it('stages valid rate modifications and normalizes prices to two decimal places', () => {
    const stagedFirst = DraftStateManager.stageCellChange(
      'Midtown / W. 39th St',
      'Adult One-Way',
      '13.25',
      '14'
    );
    const stagedSecond = DraftStateManager.stageCellChange(
      'Brookfield Place',
      'Child One-Way',
      '9.00',
      '10.5'
    );

    assert.equal(stagedFirst, true);
    assert.equal(stagedSecond, true);
    assert.equal(DraftStateManager.getDraftCount(), 2);
    assert.equal(DraftStateManager.isDirty(), true);

    const pending = DraftStateManager.getPendingChanges();
    assert.equal(pending.length, 2);
    assert.equal(pending[0].destination, 'Midtown / W. 39th St');
    assert.equal(pending[0].product, 'Adult One-Way');
    assert.equal(pending[0].originalValue, '13.25');
    assert.equal(pending[0].newValue, '14.00');
    assert.equal(pending[1].originalValue, '9.00');
    assert.equal(pending[1].newValue, '10.50');
  });

  it('overwrites an existing cell entry when edited again without double counting', () => {
    DraftStateManager.stageCellChange('Midtown / W. 39th St', 'Adult One-Way', '13.25', '14.00');
    DraftStateManager.stageCellChange('Midtown / W. 39th St', 'Adult One-Way', '13.25', '15.50');

    assert.equal(DraftStateManager.getDraftCount(), 1);
    const [entry] = DraftStateManager.getPendingChanges();
    assert.equal(entry.originalValue, '13.25');
    assert.equal(entry.newValue, '15.50');
  });

  it('ignores false mutations when newValue equals originalValue and removes draft if edited back to baseline', () => {
    const unchangedResult = DraftStateManager.stageCellChange(
      'Midtown / W. 39th St',
      'Adult One-Way',
      '13.25',
      '13.250'
    );
    assert.equal(unchangedResult, false);
    assert.equal(DraftStateManager.getDraftCount(), 0);
    assert.equal(DraftStateManager.isDirty(), false);

    DraftStateManager.stageCellChange('Midtown / W. 39th St', 'Adult One-Way', '13.25', '14.00');
    assert.equal(DraftStateManager.getDraftCount(), 1);

    const revertedViaEdit = DraftStateManager.stageCellChange(
      'Midtown / W. 39th St',
      'Adult One-Way',
      '13.25',
      '13.25'
    );
    assert.equal(revertedViaEdit, false);
    assert.equal(DraftStateManager.getDraftCount(), 0);
    assert.equal(DraftStateManager.isDirty(), false);
  });

  it('rejects missing coordinates or non-numeric prices', () => {
    assert.equal(DraftStateManager.stageCellChange('', 'Adult One-Way', '13.25', '14.00'), false);
    assert.equal(DraftStateManager.stageCellChange('Midtown', '', '13.25', '14.00'), false);
    assert.equal(DraftStateManager.stageCellChange('Midtown', 'Adult One-Way', '13.25', 'abc'), false);
    assert.equal(DraftStateManager.stageCellChange('Midtown', 'Adult One-Way', NaN, '14.00'), false);
    assert.equal(DraftStateManager.getDraftCount(), 0);
  });

  it('reverts a single drafted cell and decrements the pending counter', () => {
    DraftStateManager.stageCellChange('Midtown / W. 39th St', 'Adult One-Way', '13.25', '14.00');
    DraftStateManager.stageCellChange('Brookfield Place', 'Child One-Way', '10.75', '11.25');
    assert.equal(DraftStateManager.getDraftCount(), 2);

    const firstReverted = DraftStateManager.revertCell('Midtown / W. 39th St', 'Adult One-Way');
    assert.equal(firstReverted, true);
    assert.equal(DraftStateManager.getDraftCount(), 1);

    const compositeKey = DraftStateManager.buildCellKey('Brookfield Place', 'Child One-Way');
    const secondReverted = DraftStateManager.revertChange(compositeKey);
    assert.equal(secondReverted, true);
    assert.equal(DraftStateManager.getDraftCount(), 0);
    assert.equal(DraftStateManager.isDirty(), false);
  });

  it('returns false and never decrements below zero when reverting an unmodified cell', () => {
    const result = DraftStateManager.revertCell('Untracked Destination', 'Adult One-Way');
    assert.equal(result, false);
    assert.equal(DraftStateManager.getDraftCount(), 0);
  });

  it('discards all staged changes and returns false when discarding an already clean grid', () => {
    assert.equal(DraftStateManager.discardAll(), false);

    DraftStateManager.stageCellChange('Midtown / W. 39th St', 'Adult One-Way', '13.25', '14.00');
    DraftStateManager.stageCellChange('Brookfield Place', '10-Trip Pass', '127.50', '130.00');
    assert.equal(DraftStateManager.getDraftCount(), 2);

    assert.equal(DraftStateManager.discardAll(), true);
    assert.equal(DraftStateManager.getDraftCount(), 0);
    assert.equal(DraftStateManager.isDirty(), false);
    assert.deepEqual(DraftStateManager.getPendingChanges(), []);
  });

  it('applies .table-warning / .cell-draft classes, mounts revert button, and syncs toolbar when DOM is present', () => {
    const classes = new Set(['fare-cell-container']);
    const valueSpan = { textContent: '$13.25', hidden: false };
    let appendedBtn = null;

    const mockCell = {
      dataset: { rowKey: 'Midtown / W. 39th St', colKey: 'Adult One-Way', value: '13.25' },
      classList: {
        add: (...names) => names.forEach((n) => classes.add(n)),
        remove: (...names) => names.forEach((n) => classes.delete(n)),
        contains: (name) => classes.has(name)
      },
      querySelector: (selector) => {
        if (selector === '.fare-cell-value') return valueSpan;
        if (selector === '.fare-cell-revert-btn') return appendedBtn;
        return null;
      },
      appendChild: (el) => {
        appendedBtn = el;
      },
      closest: () => ({
        dataset: { tenant: 'NYW_SANDBOX', filterValue: 'EDGEWTR' }
      })
    };

    const counterBadge = { textContent: '0' };
    const discardBtn = { disabled: true };
    const saveBtn = { disabled: true };

    globalThis.document = {
      querySelectorAll: (selector) =>
        selector === 'td.fare-cell-container' ? [mockCell] : [],
      querySelector: () => null,
      getElementById: (id) => {
        if (id === 'changesCounter') return counterBadge;
        if (id === 'discardDraftsBtn') return discardBtn;
        if (id === 'saveChangesBtn') return saveBtn;
        return null;
      },
      createElement: () => ({
        type: '',
        className: '',
        innerHTML: '',
        title: '',
        addEventListener: () => {},
        remove: () => {
          appendedBtn = null;
        }
      })
    };

    DraftStateManager.stageChange({
      rowKey: 'Midtown / W. 39th St',
      colKey: 'Adult One-Way',
      originalValue: '13.25',
      newValue: '14.50',
      cellElement: mockCell
    });

    assert.equal(classes.has('table-warning'), true);
    assert.equal(classes.has('cell-draft'), true);
    assert.equal(mockCell.dataset.value, '14.50');
    assert.equal(valueSpan.textContent, '$14.50');
    assert.notEqual(appendedBtn, null);
    assert.equal(counterBadge.textContent, '1');
    assert.equal(discardBtn.disabled, false);
    assert.equal(saveBtn.disabled, false);

    DraftStateManager.revertCell('Midtown / W. 39th St', 'Adult One-Way');

    assert.equal(classes.has('table-warning'), false);
    assert.equal(classes.has('cell-draft'), false);
    assert.equal(mockCell.dataset.value, '13.25');
    assert.equal(valueSpan.textContent, '$13.25');
    assert.equal(appendedBtn, null);
    assert.equal(counterBadge.textContent, '0');
    assert.equal(discardBtn.disabled, true);
    assert.equal(saveBtn.disabled, true);
  });
});