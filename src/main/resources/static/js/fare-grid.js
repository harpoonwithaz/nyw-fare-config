(function () {
  'use strict';

  function formatPrice(value) {
    return '$' + Number(value).toFixed(2);
  }

  function startEditing(cell) {
    var valueElement = cell.querySelector('.fare-cell-value');
    if (!valueElement || cell.dataset.inlineEditable !== 'true' || cell.querySelector('input')) {
      return;
    }

    var originalValue = cell.dataset.value;
    var input = document.createElement('input');
    input.type = 'number';
    input.min = '0.00';
    input.step = '0.01';
    input.value = originalValue;
    input.className = 'form-control form-control-sm text-center font-monospace fare-cell-input';
    input.setAttribute('aria-label', 'Edit fare for ' + cell.dataset.rowKey + ' to ' + cell.dataset.colKey);

    valueElement.replaceWith(input);
    input.focus();
    input.select();

    function finishEditing() {
      if (!input.isConnected) {
        return;
      }
      var value = input.value.trim() || originalValue;
      var span = document.createElement('span');
      span.className = valueElement.className;
      span.textContent = formatPrice(value);
      cell.dataset.value = value;
      input.replaceWith(span);
    }

    input.addEventListener('blur', finishEditing);
    input.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        finishEditing();
      } else if (event.key === 'Escape') {
        event.preventDefault();
        input.replaceWith(valueElement);
      }
    });
  }

  document.addEventListener('click', function (event) {
    var cell = event.target.closest('.fare-cell-container');
    if (cell) {
      startEditing(cell);
    }
  });
}());
