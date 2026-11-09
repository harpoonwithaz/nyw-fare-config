(function () {
  'use strict';

  function formatPrice(value) {
    return '$' + Number(value).toFixed(2);
  }

  function sanitizeNumericValue(value) {
    var sanitized = value.replace(/[^\d.]/g, '');
    var decimalIndex = sanitized.indexOf('.');

    if (decimalIndex !== -1) {
      sanitized = sanitized.slice(0, decimalIndex + 1)
        + sanitized.slice(decimalIndex + 1).replace(/\./g, '');
      sanitized = sanitized.slice(0, decimalIndex + 3);
    }

    return sanitized;
  }

  function isAllowedNumericValue(value) {
    return /^\d*(\.\d{0,2})?$/.test(value);
  }

  function isValidFareValue(value) {
    return /^\d+(\.\d{1,2})?$/.test(value) && Number(value) >= 0;
  }

  function startEditing(cell) {
    var saveButton = document.getElementById('saveChangesBtn');
    var changesCounter = document.getElementById('changesCounter');

    var valueElement = cell.querySelector('.fare-cell-value');
    if (!valueElement || cell.dataset.inlineEditable !== 'true' || cell.querySelector('input')) {
      return;
    }

    // Convert span to input box
    var originalValue = cell.dataset.value;
    var input = document.createElement('input');
    input.type = 'number';
    input.min = '0.00';
    input.step = '0.01';
    input.value = isValidFareValue(originalValue) ? originalValue : '';
    input.className = 'form-control form-control-sm text-center font-monospace fare-cell-input';
    input.inputMode = 'decimal';
    input.setAttribute('aria-label', 'Edit fare for ' + cell.dataset.rowKey + ' to ' + cell.dataset.colKey);
    var errorId = 'fare-cell-error-' + cell.dataset.rowKey + '-' + cell.dataset.colKey;
    input.setAttribute('aria-describedby', errorId);

    var errorMessage = document.createElement('div');
    errorMessage.id = errorId;
    errorMessage.className = 'invalid-feedback';
    errorMessage.setAttribute('role', 'alert');
    errorMessage.textContent = 'Enter a non-negative number with up to two decimal places.';
    errorMessage.hidden = true;
    var lastAlertedInvalidValue = null;
    var isFinishing = false;

    valueElement.replaceWith(input);
    cell.appendChild(errorMessage);
    input.focus();
    input.select();

    function setInvalidState(invalid) {
      input.classList.toggle('is-invalid', invalid);
      input.setAttribute('aria-invalid', String(invalid));
      errorMessage.hidden = !invalid;
      if (invalid) {
        if (saveButton) {
          saveButton.disabled = true;
        }
      }
    }

    // Validate users inline cell inputs
    input.addEventListener('keydown', function (event) {
      if (event.key === 'Enter' || event.key === 'Escape') {
        return;
      }

      if (event.ctrlKey || event.metaKey || event.altKey
        || ['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
        return;
      }

      if (event.key.length !== 1) {
        event.preventDefault(); // disable browser default behavior to enforce our custom input masking
        return;
      }

      var selectionStart = input.selectionStart || 0;
      var selectionEnd = input.selectionEnd || selectionStart;
      var proposedValue = input.value.slice(0, selectionStart)
        + event.key
        + input.value.slice(selectionEnd);

      if (!isAllowedNumericValue(proposedValue)) {
        event.preventDefault();
      }
    });

    input.addEventListener('input', function () {
      var wasMalformed = !isAllowedNumericValue(input.value);
      var sanitizedValue = sanitizeNumericValue(input.value);
      if (input.value !== sanitizedValue) {
        input.value = sanitizedValue;
      }
      lastAlertedInvalidValue = null;
      setInvalidState(wasMalformed || !isValidFareValue(input.value));
    });

    function incrementChangesCounter() {
      // TODO: Change the state of the changes made in the draft state data structure (Story 2.6)
      changesCounter.textContent = Number(changesCounter.textContent) + 1;
    }

    function finishEditing() {
      if (!input.isConnected || isFinishing) {
        return;
      }
      isFinishing = true;

      // TODO: Contact draft manager to draft changed value (Story 2.6)
      var value = input.value.trim();
      if (!isValidFareValue(value)) {
        setInvalidState(true);
        if (lastAlertedInvalidValue !== value) {
          lastAlertedInvalidValue = value;
          window.alert('Please enter a non-negative number with up to two decimal places.');
        }
        isFinishing = false;
        return;
      }

      // Convert input box back to span
      var span = document.createElement('span');
      span.className = valueElement.className;
      span.textContent = formatPrice(value);
      cell.dataset.value = value;
      errorMessage.remove();
      input.replaceWith(span);
      isFinishing = false;
    }

    input.addEventListener('blur', finishEditing);
    input.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        finishEditing();
      } else if (event.key === 'Escape') {
        event.preventDefault();
        errorMessage.remove();
        input.replaceWith(valueElement);
      }
    });

    setInvalidState(!isValidFareValue(input.value));
  }

  document.addEventListener('click', function (event) {
    var cell = event.target.closest('.fare-cell-container');
    if (cell) {
      startEditing(cell);
    }
  });
}());
