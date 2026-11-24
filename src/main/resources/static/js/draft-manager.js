/**
 * Visual Draft State Engine & Cell Revert Controls
 */
(() => {
  'use strict';

  const FEEDBACK_TIMER = 6000;
  const draftMap = new Map();
  let hoveredDraftCell = null;
  let bypassUnloadGuard = false;

  function normalizePrice(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed.toFixed(2) : null;
  }

  function formatCurrency(normalizedValue) {
    return `$${normalizedValue}`;
  }

  function buildCellKey(rowKey, colKey) {
    return `${rowKey}::${colKey}`;
  }

  function getTableContext(cellElement = null) {
    if (typeof document === 'undefined') {
      return { tenant: 'DEFAULT', origin: 'DEFAULT' };
    }

    const table = cellElement?.closest('.fare-matrix-table')
      || document.querySelector('.fare-matrix-table');

    return {
      tenant: table?.dataset.tenant || 'DEFAULT',
      origin: table?.dataset.filterValue || document.getElementById('secondarySelect')?.value || 'DEFAULT'
    };
  }

  function showFeedback(message, level = 'info') {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('grid-feedback-container');
    if (!container) return;

    container.querySelectorAll('.grid-feedback-slot').forEach((slot) => {
      const isTarget = slot.dataset.feedbackLevel === level;
      slot.hidden = !isTarget;

      if (isTarget) {
        const textEl = slot.querySelector('.feedback-card-text');
        if (textEl) {
          textEl.textContent = message;
        }
      }
    });

    clearTimeout(container._hideTimer);
    container._hideTimer = setTimeout(() => {
      container.querySelectorAll('.grid-feedback-slot').forEach((slot) => {
        slot.hidden = true;
      });
    }, FEEDBACK_TIMER);
  }

  function findCellElement(rowKey, colKey) {
    if (typeof document === 'undefined') return null;
    const cells = document.querySelectorAll('td.fare-cell-container');
    for (const cell of cells) {
      if (cell.dataset.rowKey === rowKey && cell.dataset.colKey === colKey) {
        return cell;
      }
    }
    return null;
  }

  function applyDraftVisuals(cell, entry) {
    if (!cell) return;

    cell.dataset.value = entry.newValue;
    cell.classList.add('table-warning', 'cell-draft');

    const valueSpan = cell.querySelector('.fare-cell-value');
    if (valueSpan) {
      valueSpan.textContent = formatCurrency(entry.newValue);
    }

    let revertBtn = cell.querySelector('.fare-cell-revert-btn');
    if (!revertBtn) {
      revertBtn = document.createElement('button');
      revertBtn.type = 'button';
      revertBtn.className = 'btn btn-link btn-sm p-0 fare-cell-revert-btn';
      revertBtn.innerHTML = '<i class="bi bi-arrow-counterclockwise"></i>';

      revertBtn.addEventListener('mousedown', (event) => {
        event.preventDefault();
        event.stopPropagation();
      });

      revertBtn.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        revertCell(entry.rowKey, entry.colKey);
      });

      cell.appendChild(revertBtn);
    }

    revertBtn.title = `Revert to ${formatCurrency(entry.originalValue)}`;
  }

  function restoreCellBaselineDOM(cell, originalValue) {
    if (!cell) return;

    const input = cell.querySelector('.fare-inline-input');
    const feedbackEl = cell.querySelector('.fare-cell-feedback');
    const revertBtn = cell.querySelector('.fare-cell-revert-btn');
    const valueSpan = cell.querySelector('.fare-cell-value');

    if (input) input.remove();
    if (feedbackEl) feedbackEl.remove();
    if (revertBtn) revertBtn.remove();

    cell.classList.remove('table-warning', 'cell-draft', 'has-error', 'is-editing');
    cell.dataset.value = originalValue;

    if (valueSpan) {
      valueSpan.textContent = formatCurrency(originalValue);
      valueSpan.hidden = false;
    }
  }

  function syncToolbarState() {
    if (typeof document === 'undefined') return;

    const count = draftMap.size;
    const hasDrafts = count > 0;
    const hasInvalidInput = document.querySelector('.fare-inline-input.is-invalid') !== null;

    const counterBadge = document.getElementById('changesCounter');
    const discardBtn = document.getElementById('discardDraftsBtn');
    const saveBtn = document.getElementById('saveChangesBtn');

    if (counterBadge) {
      counterBadge.textContent = String(count);
    }
    if (discardBtn) {
      discardBtn.disabled = !hasDrafts;
    }
    if (saveBtn) {
      saveBtn.disabled = !hasDrafts || hasInvalidInput;
    }
  }

  /**
   * Stages a rate change on the active O-D grid, or removes the draft if edited back to originalValue.
   */
  function stageChange({ rowKey, colKey, originalValue, newValue, cellElement = null } = {}) {
    if (!rowKey || !colKey) return false;

    const normalizedOriginal = normalizePrice(originalValue);
    const normalizedNew = normalizePrice(newValue);
    if (normalizedOriginal === null || normalizedNew === null) return false;

    const cellKey = buildCellKey(rowKey, colKey);
    const cell = cellElement || findCellElement(rowKey, colKey);

    // False-mutation guard: if value matches baseline, remove from drafts
    if (normalizedOriginal === normalizedNew) {
      if (draftMap.has(cellKey)) {
        draftMap.delete(cellKey);
        restoreCellBaselineDOM(cell, normalizedOriginal);
        syncToolbarState();
      }
      return false;
    }

    const { tenant, origin } = getTableContext(cell);
    const entry = {
      cellKey,
      tenant,
      origin,
      destination: String(rowKey),
      product: String(colKey),
      rowKey: String(rowKey),
      colKey: String(colKey),
      originalValue: normalizedOriginal,
      newValue: normalizedNew
    };

    draftMap.set(cellKey, entry);
    applyDraftVisuals(cell, entry);
    syncToolbarState();
    return true;
  }

  /**
   * Reverts a single drafted cell back to its baseline price.
   * Accepts either (rowKey, colKey) or "rowKey::colKey".
   */
  function revertCell(keyOrRow, colKey) {
    const cellKey = colKey !== undefined ? buildCellKey(keyOrRow, colKey) : String(keyOrRow ?? '');
    const entry = draftMap.get(cellKey);
    if (!entry) return false;

    const cell = findCellElement(entry.rowKey, entry.colKey);
    restoreCellBaselineDOM(cell, entry.originalValue);

    if (hoveredDraftCell === cell) {
      hoveredDraftCell = null;
    }

    draftMap.delete(cellKey);
    syncToolbarState();
    return true;
  }

  /**
   * Discards all pending drafts on the current grid and restores baseline DOM values.
   */
  function discardAll() {
    if (draftMap.size === 0) {
      showFeedback('No Changes to Discard: There are currently no pending edits on the grid.', 'info');
      return false;
    }

    draftMap.forEach((entry) => {
      const cell = findCellElement(entry.rowKey, entry.colKey);
      restoreCellBaselineDOM(cell, entry.originalValue);
    });

    hoveredDraftCell = null;
    draftMap.clear();
    syncToolbarState();
    showFeedback('Drafts Discarded: All pending rate edits have been reverted to baseline.', 'info');
    return true;
  }

  function getPendingChanges() {
    return Array.from(draftMap.values()).map(({ rowKey, colKey, ...publicEntry }) => publicEntry);
  }

  function getDraftCount() {
    return draftMap.size;
  }

  function isDirty() {
    return draftMap.size > 0;
  }

  /**
   * Intercepts dropdown switches (#globalTenantSelect in navbar.html and #secondarySelect in fare.html)
   * without requiring HTML edits to fragments/navbar.html.
   */
  function bindContextSwitchGuards() {
    ['globalTenantSelect', 'secondarySelect', 'productSelect'].forEach((selectId) => {
      const selectEl = document.getElementById(selectId);
      if (!selectEl) return;

      const initialValue = selectEl.value;
      selectEl.onchange = null;
      selectEl.removeAttribute('onchange');

      selectEl.addEventListener('change', () => {
        if (isDirty()) {
          const confirmed = window.confirm(
            'You have unsaved rate changes on this grid. Switching will discard your pending drafts. Continue?'
          );
          if (!confirmed) {
            selectEl.value = initialValue;
            return;
          }
          bypassUnloadGuard = true;
        }
        selectEl.form?.submit();
      });
    });

    window.addEventListener('beforeunload', (event) => {
      if (isDirty() && !bypassUnloadGuard) {
        event.preventDefault();
        event.returnValue = '';
      }
    });
  }

  function initDraftManager() {
    const discardBtn = document.getElementById('discardDraftsBtn');
    if (discardBtn) {
      discardBtn.addEventListener('click', () => {
        discardAll();
      });
    }

    document.addEventListener('mouseover', (event) => {
      const draftedCell = event.target.closest('td.fare-cell-container.cell-draft');
      hoveredDraftCell = draftedCell || null;
    });

    document.addEventListener('keydown', (event) => {
      if (event.key !== 'Escape') return;

      const activeTag = document.activeElement?.tagName;
      if (activeTag === 'INPUT' || activeTag === 'SELECT' || activeTag === 'TEXTAREA') {
        return;
      }

      if (hoveredDraftCell && hoveredDraftCell.classList.contains('cell-draft')) {
        event.preventDefault();
        revertCell(hoveredDraftCell.dataset.rowKey, hoveredDraftCell.dataset.colKey);
      }
    });

    bindContextSwitchGuards();
    syncToolbarState();
  }

  const DraftStateManager = {
    buildCellKey,
    stageChange,
    stageCellChange: (rowKey, colKey, originalValue, newValue) =>
      stageChange({ rowKey, colKey, originalValue, newValue }),
    revertCell,
    revertChange: revertCell,
    discardAll,
    getPendingChanges,
    getDrafts: getPendingChanges,
    getDraftCount,
    isDirty,
    syncToolbarState
  };

  if (typeof window !== 'undefined') {
    window.DraftStateManager = DraftStateManager;
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = DraftStateManager;
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initDraftManager);
    } else {
      initDraftManager();
    }
  }
})();