/**
 * Story 2.2: O-D Mode Direct Inline Rate Editing & Numeric Input Masking
 */
(() => {
  'use strict';

  // Strict US currency validation: positive numbers with up to 2 decimal places
  const CURRENCY_REGEX = /^\d+(\.\d{1,2})?$/;
  // Partial input pattern allowed while actively typing (e.g., "14." before typing cents)
  const PARTIAL_INPUT_REGEX = /^\d*\.?\d{0,2}$/;
  const MAX_FARE_AMOUNT = 9999.99;

  const ALLOWED_CONTROL_KEYS = new Set([
    'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
    'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'
  ]);

  const FEEDBACK_ICONS = {
    danger: 'bi-exclamation-octagon-fill',
    warning: 'bi-exclamation-triangle-fill',
    secondary: 'bi-info-circle-fill',
    info: 'bi-info-circle-fill'
  };

  const FEEDBACK_DURATION = 10000; // 10 seconds

  /**
   * Formats a numeric value to a two-decimal string (e.g., "14.00")
   */
  function normalizePrice(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed.toFixed(2) : null;
  }

  /**
   * Formats a normalized price string as US Currency (e.g., "$14.00")
   */
  function formatCurrencyDisplay(normalizedValue) {
    return `$${normalizedValue}`;
  }

  /**
   * Validates a raw string against Story 2.2 acceptance criteria.
   * Returns { valid: boolean, reason?: string, normalized?: string }
   */
  function validateFareInput(rawString, badInputFlag = false) {
    if (badInputFlag) {
      return { valid: false, reason: 'Enter a valid positive amount (e.g., 14.00).' };
    }

    const trimmed = String(rawString ?? '').trim();
    if (trimmed === '') {
      return { valid: false, reason: 'Fare cannot be blank.' };
    }

    if (!CURRENCY_REGEX.test(trimmed)) {
      return { valid: false, reason: 'Use positive dollars and cents (max 2 decimals).' };
    }

    const numericValue = Number(trimmed);
    if (!Number.isFinite(numericValue) || numericValue < 0) {
      return { valid: false, reason: 'Fare must be $0.00 or greater.' };
    }

    if (numericValue > MAX_FARE_AMOUNT) {
      return { valid: false, reason: `Fare cannot exceed $${MAX_FARE_AMOUNT.toFixed(2)}.` };
    }

    return {
      valid: true,
      normalized: numericValue.toFixed(2)
    };
  }

  /**
   * Hides the existing feedback card in fare.html
   */
  function hideGridFeedback() {
    const banner = document.getElementById('fare-grid-feedback-banner');
    if (!banner) return;

    clearTimeout(banner._hideTimer);
    banner.classList.remove('d-flex');
    banner.classList.add('d-none');
  }

  /**
   * Toggles the existing feedback card in fare.html and updates its message and style
   */
  function showGridFeedback(message, type = 'warning') {
    const banner = document.getElementById('fare-grid-feedback-banner');
    const messageEl = document.getElementById('fare-grid-feedback-message');
    const iconEl = document.getElementById('fare-grid-feedback-icon');
    const closeBtn = document.getElementById('fare-grid-feedback-close');

    if (!banner || !messageEl) return;

    banner.classList.remove('alert-warning', 'alert-danger', 'alert-secondary', 'alert-info', 'd-none');
    banner.classList.add(`alert-${type}`, 'd-flex');

    if (iconEl) {
      iconEl.className = `bi ${FEEDBACK_ICONS[type] || FEEDBACK_ICONS.warning} fs-5 me-2`;
    }

    messageEl.textContent = message;

    if (closeBtn && !closeBtn.dataset.boundClose) {
      closeBtn.dataset.boundClose = 'true';
      closeBtn.addEventListener('click', hideGridFeedback);
    }

    clearTimeout(banner._hideTimer);
    banner._hideTimer = setTimeout(hideGridFeedback, FEEDBACK_DURATION);
  }

  /**
   * Marks an active cell input as invalid or valid and manages ARIA attributes
   */
  function setInputValidityUI(cell, input, isValid, errorMessage = '') {
    let feedbackEl = cell.querySelector('.fare-cell-feedback');

    if (!isValid) {
      input.classList.add('is-invalid');
      input.setAttribute('aria-invalid', 'true');
      cell.classList.add('has-error');

      if (!feedbackEl) {
        feedbackEl = document.createElement('span');
        feedbackEl.className = 'fare-cell-feedback';
        feedbackEl.id = `err-${cell.dataset.rowKey}-${cell.dataset.colKey}`;
        feedbackEl.setAttribute('role', 'alert');
        cell.appendChild(feedbackEl);
      }
      feedbackEl.textContent = errorMessage;
      input.setAttribute('aria-describedby', feedbackEl.id);
    } else {
      input.classList.remove('is-invalid');
      input.setAttribute('aria-invalid', 'false');
      input.removeAttribute('aria-describedby');
      cell.classList.remove('has-error');
      if (feedbackEl) feedbackEl.remove();
    }
  }

  /**
   * Restores the cell from an <input> back to its static currency <span>
   */
  function closeInlineEditor(cell, committedPrice) {
    const valueSpan = cell.querySelector('.fare-cell-value');
    const input = cell.querySelector('.fare-inline-input');
    const feedbackEl = cell.querySelector('.fare-cell-feedback');

    if (input) input.remove();
    if (feedbackEl) feedbackEl.remove();

    cell.classList.remove('has-error', 'is-editing');
    cell.dataset.value = committedPrice;

    if (valueSpan) {
      valueSpan.textContent = formatCurrencyDisplay(committedPrice);
      valueSpan.hidden = false;
    }
  }

  /**
   * Commits the current input value if valid; otherwise shows error state and blocks commit
   */
  function attemptCommit(cell, input) {
    const result = validateFareInput(input.value, input.validity?.badInput);

    if (!result.valid) {
      setInputValidityUI(cell, input, false, result.reason);
      showGridFeedback(
        `Invalid fare for ${cell.dataset.rowKey} (${cell.dataset.colKey}): ${result.reason}`,
        'danger'
      );
      return false;
    }

    closeInlineEditor(cell, result.normalized);
    return true;
  }

  /**
   * Cancels the active edit and reverts to the cell's value prior to opening the input
   */
  function cancelEdit(cell) {
    const priorValue = normalizePrice(cell.dataset.value) ?? normalizePrice(cell.dataset.originalPrice) ?? '0.00';
    closeInlineEditor(cell, priorValue);
  }

  /**
   * Moves focus to the next or previous inline-editable cell when Tab is pressed
   */
  function focusAdjacentEditableCell(currentCell, backwards = false) {
    const table = currentCell.closest('.fare-matrix-table');
    if (!table) return;

    const editableCells = Array.from(
      table.querySelectorAll('td.fare-cell-container[data-inline-editable="true"]')
    );
    const currentIndex = editableCells.indexOf(currentCell);
    if (currentIndex === -1) return;

    const targetCell = editableCells[backwards ? currentIndex - 1 : currentIndex + 1];
    if (targetCell) {
      openInlineEditor(targetCell);
    }
  }

  /**
   * Converts a clicked O-D price cell into a focused numeric input
   */
  function openInlineEditor(cell) {
    if (cell.classList.contains('is-editing')) return;

    const valueSpan = cell.querySelector('.fare-cell-value');
    if (!valueSpan) return;

    const rawPrice = normalizePrice(cell.dataset.value) ?? normalizePrice(cell.dataset.originalPrice) ?? '0.00';

    cell.classList.add('is-editing');
    valueSpan.hidden = true;

    const input = document.createElement('input');
    input.type = 'number';
    input.min = '0.00';
    input.step = '0.01';
    input.inputMode = 'decimal';
    input.className = 'fare-inline-input';
    input.value = rawPrice;
    input.setAttribute('aria-label', `Edit fare for ${cell.dataset.rowKey} ${cell.dataset.colKey}`);

    let lastAcceptedValue = rawPrice;
    let isCanceling = false;
    // Initial cell activation highlights the full number so typing overwrites it
    let isOverwriteMode = true;
    let hasDecimalPoint = rawPrice.includes('.');

    // Clicking inside the already-open input places the caret at a specific position and exits overwrite mode
    input.addEventListener('mousedown', () => {
      isOverwriteMode = false;
    });

    // Keystroke masking: handle overwrite-on-type and block invalid characters
    input.addEventListener('keydown', (event) => {
      if (event.ctrlKey || event.metaKey) {
        if (event.key.toLowerCase() === 'a') {
          isOverwriteMode = true;
        }
        return;
      }

      if (event.altKey) return;

      if (event.key === 'Escape') {
        event.preventDefault();
        isCanceling = true;
        cancelEdit(cell);
        return;
      }

      if (event.key === 'Enter') {
        event.preventDefault();
        attemptCommit(cell, input);
        return;
      }

      if (event.key === 'Tab') {
        event.preventDefault();
        if (attemptCommit(cell, input)) {
          focusAdjacentEditableCell(cell, event.shiftKey);
        }
        return;
      }

      // Block Down arrow from stepping into negative numbers
      if (event.key === 'ArrowDown' && Number(input.value || 0) <= 0) {
        event.preventDefault();
        return;
      }

      // Moving the cursor with navigation keys places the caret at a specific position
      if (
        event.key === 'ArrowLeft' ||
        event.key === 'ArrowRight' ||
        event.key === 'ArrowUp' ||
        event.key === 'ArrowDown' ||
        event.key === 'Home' ||
        event.key === 'End'
      ) {
        isOverwriteMode = false;
        return;
      }

      // Pressing Backspace/Delete while the full cell value is highlighted clears the field
      if (event.key === 'Backspace' || event.key === 'Delete') {
        if (isOverwriteMode) {
          event.preventDefault();
          isOverwriteMode = false;
          hasDecimalPoint = false;
          input.value = '';
          lastAcceptedValue = '';
          setInputValidityUI(cell, input, false, 'Fare cannot be blank.');
        }
        return;
      }

      if (ALLOWED_CONTROL_KEYS.has(event.key)) return;

      const isDigit = /^[0-9]$/.test(event.key);
      const isDot = event.key === '.';

      if (!isDigit && !isDot) {
        event.preventDefault();
        return;
      }

      // If the cell was just selected and the cursor wasn't placed at a specific position, overwrite the value
      if (isOverwriteMode) {
        isOverwriteMode = false;
        hasDecimalPoint = isDot;
        input.value = '';
        lastAcceptedValue = '';
        return;
      }

      // Block a second decimal point
      if (isDot && (hasDecimalPoint || input.value.includes('.'))) {
        event.preventDefault();
        return;
      }

      if (isDot) {
        hasDecimalPoint = true;
      }
    });

    // Paste handling: strip leading '$' or spaces and validate format
    input.addEventListener('paste', (event) => {
      event.preventDefault();
      isOverwriteMode = false;

      const rawPaste = (event.clipboardData || window.clipboardData).getData('text') || '';
      const cleaned = rawPaste.trim().replace(/^\$\s*/, '');

      if (CURRENCY_REGEX.test(cleaned)) {
        input.value = cleaned;
        lastAcceptedValue = cleaned;
        hasDecimalPoint = cleaned.includes('.');
        setInputValidityUI(cell, input, true);
      } else {
        setInputValidityUI(cell, input, false, 'Pasted value must be a positive amount (max 2 decimals).');
        showGridFeedback(
          `Invalid pasted fare for ${cell.dataset.rowKey} (${cell.dataset.colKey}): must be a positive amount with up to 2 decimal places.`,
          'danger'
        );
      }
    });

    // Real-time input sanitization & validation feedback
    input.addEventListener('input', () => {
      if (input.validity?.badInput) {
        setInputValidityUI(cell, input, false, 'Invalid numeric format.');
        return;
      }

      const val = input.value;
      if (val === '') {
        lastAcceptedValue = '';
        hasDecimalPoint = false;
        setInputValidityUI(cell, input, false, 'Fare cannot be blank.');
        return;
      }

      // Revert if typing exceeds two decimal places
      if (!PARTIAL_INPUT_REGEX.test(val)) {
        input.value = lastAcceptedValue;
        hasDecimalPoint = lastAcceptedValue.includes('.');
        return;
      }

      lastAcceptedValue = val;
      hasDecimalPoint = val.includes('.');

      const check = validateFareInput(val, false);
      setInputValidityUI(cell, input, check.valid, check.reason || '');
    });

    // Blur handling: commit valid edits; keep invalid inputs highlighted until fixed or canceled
    input.addEventListener('blur', () => {
      if (isCanceling || !cell.classList.contains('is-editing')) return;

      const committed = attemptCommit(cell, input);
      if (!committed) {
        requestAnimationFrame(() => {
          if (cell.contains(input)) input.focus();
        });
      }
    });

    cell.appendChild(input);
    input.focus();
    input.select();
  }

  /**
   * Initializes delegated event listeners on all fare matrix tables
   */
  function initFareGridInlineEditing() {
    const tables = document.querySelectorAll('.fare-matrix-table');

    tables.forEach((table) => {
      table.querySelectorAll('td.fare-cell-container[data-inline-editable="true"]').forEach((cell) => {
        if (!cell.hasAttribute('tabindex')) {
          cell.setAttribute('tabindex', '0');
        }
      });

      table.addEventListener('click', (event) => {
        const cell = event.target.closest('td.fare-cell-container');
        if (!cell || !table.contains(cell)) return;

        // Keep focus on any cell that currently has an unresolved invalid input
        const existingInvalidInput = table.querySelector('.fare-inline-input.is-invalid');
        if (existingInvalidInput && !cell.contains(existingInvalidInput)) {
          existingInvalidInput.focus();
          return;
        }

        const isEditable = cell.dataset.inlineEditable === 'true';
        const status = cell.dataset.status;

        if (!isEditable || status !== 'ACTIVE') {
          cell.classList.add('cell-locked-feedback');
          setTimeout(() => cell.classList.remove('cell-locked-feedback'), 600);

          if (status === 'NOT_SOLD') {
            showGridFeedback(
              `Route ${cell.dataset.rowKey} (${cell.dataset.colKey}) is flagged as NOT_SOLD. Enable product availability before editing its price.`,
              'warning'
            );
          } else if (status === 'NA') {
            showGridFeedback(
              `Route ${cell.dataset.rowKey} (${cell.dataset.colKey}) is N/A (unserved combination) and cannot be priced.`,
              'secondary'
            );
          }
          return;
        }

        openInlineEditor(cell);
      });

      table.addEventListener('keydown', (event) => {
        if (event.target.classList.contains('fare-inline-input')) return;
        const cell = event.target.closest('td.fare-cell-container[data-inline-editable="true"]');
        if (!cell) return;

        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          openInlineEditor(cell);
        }
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initFareGridInlineEditing);
  } else {
    initFareGridInlineEditing();
  }
})();