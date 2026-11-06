// Live preview for the Service Alerts page.
// Everything the TVM screen shows lives in `state`, and render() redraws the screen from it.

const MAX_MESSAGE = 200;
const NEAR_LIMIT = 180;

const state = {
    lang: 'en',
    messageEn: '',
    messageEs: '',
    continueEn: '',
    continueEs: '',
    backEn: '',
    backEs: ''
};

function el(id) {
    return document.getElementById(id);
}

// English or Spanish version of a field, falling back to a default if it's blank
function pick(en, es, defaultEn, defaultEs) {
    if (state.lang === 'es') {
        return es || defaultEs;
    }
    return en || defaultEn;
}

// "84" in the counter, orange when close to the limit, red at the limit
function updateCounter(counterId, text) {
    const counter = el(counterId);
    counter.textContent = text.length;
    counter.parentElement.classList.toggle('text-warning-emphasis', text.length >= NEAR_LIMIT && text.length < MAX_MESSAGE);
    counter.parentElement.classList.toggle('text-danger', text.length >= MAX_MESSAGE);
}

function render() {
    el('tvmRoute').textContent = el('route').selectedOptions[0].text;

    // Show a note instead of a blank screen when a translation is missing
    const message = state.lang === 'es' ? state.messageEs : state.messageEn;
    const missingNote = state.lang === 'es' ? 'Missing Spanish translation' : 'Missing English message';
    el('tvmMessage').textContent = message || missingNote;
    el('tvmMessage').classList.toggle('tvm-missing', !message);

    el('tvmContinue').textContent = pick(state.continueEn, state.continueEs, 'Continue', 'Continuar');
    el('tvmBack').textContent = pick(state.backEn, state.backEs, 'Go back', 'Regresar');

    updateCounter('countEn', state.messageEn);
    updateCounter('countEs', state.messageEs);

    // Highlight the language that's showing, and tell screen readers too
    el('langEn').classList.toggle('active', state.lang === 'en');
    el('langEs').classList.toggle('active', state.lang === 'es');
    el('langEn').setAttribute('aria-pressed', state.lang === 'en');
    el('langEs').setAttribute('aria-pressed', state.lang === 'es');
}

// Every text box updates the preview as you type
const textFields = ['messageEn', 'messageEs', 'continueEn', 'continueEs', 'backEn', 'backEs'];
textFields.forEach(function (id) {
    state[id] = el(id).value; // start with the example text already in the form
    el(id).addEventListener('input', function (event) {
        state[id] = event.target.value;
        render();
    });
});

el('route').addEventListener('change', render);

el('langEn').addEventListener('click', function () {
    state.lang = 'en';
    render();
});
el('langEs').addEventListener('click', function () {
    state.lang = 'es';
    render();
});

render();
