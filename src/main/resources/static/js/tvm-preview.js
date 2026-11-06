// Live preview for the Service Alerts page.
// Everything the TVM screen shows lives in `state`, and render() redraws the screen from it.

const MAX_MESSAGE = 200;
const NEAR_LIMIT = 180;

// Sample schedules from the client spreadsheet, still to be confirmed with Eldon.
// Times are TVM local time, and each window is [first sailing, last sailing).
const SCHEDULES = {
    'EDGEWATER-MIDTOWN': { weekdaysOnly: true, windows: [['06:00', '09:35'], ['16:35', '19:30']] },
    'EDGEWATER-BROOKFD': { weekdaysOnly: true, windows: [['06:00', '09:35'], ['16:35', '19:30']] },
    'EDGEWATER-PIER11': { weekdaysOnly: true, windows: [['06:00', '09:35'], ['16:35', '19:30']] },
    'PORTIMP-MIDTOWN': { weekdaysOnly: false, windows: [['06:00', '22:00']] },
    'PORTIMP-PIER11': { weekdaysOnly: true, windows: [['06:00', '09:30'], ['16:00', '19:30']] }
};

const DAY_NAMES = {
    en: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
    es: ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado']
};

// Where each button action takes the rider on the TVM
const ACTION_SCREENS = { PAYMENT: 'payment', ROUTE_LIST: 'routes', HOME: 'home' };

const state = {
    lang: 'en',
    route: '',
    clock: '',
    screen: 'main', // main = warning or ready screen, depending on the rules
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

// '16:35' -> 995 minutes after midnight, so times compare as plain numbers
function toMinutes(hhmm) {
    const parts = hhmm.split(':');
    return Number(parts[0]) * 60 + Number(parts[1]);
}

// '16:35' -> '4:35 PM'
function to12h(hhmm) {
    const parts = hhmm.split(':');
    let hours = Number(parts[0]);
    const suffix = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12 || 12;
    return hours + ':' + parts[1] + ' ' + suffix;
}

// The rule we're checking with Eldon: if the route isn't running at this time,
// warn the rider and say when it next runs. Returns null when no warning is needed.
// Doesn't touch the page, so it can be tested on its own in the console.
function getWarning(schedule, date) {
    const day = date.getDay(); // 0 = Sunday ... 6 = Saturday
    const now = date.getHours() * 60 + date.getMinutes();
    const runsOn = function (d) {
        return !schedule.weekdaysOnly || (d !== 0 && d !== 6);
    };

    if (runsOn(day)) {
        const inService = schedule.windows.some(function (w) {
            return now >= toMinutes(w[0]) && now < toMinutes(w[1]);
        });
        if (inService) {
            return null;
        }
        const laterToday = schedule.windows.find(function (w) {
            return toMinutes(w[0]) > now;
        });
        if (laterToday) {
            return { type: 'OFF_HOURS', daysAhead: 0, day: day, time: laterToday[0] };
        }
    }

    // Nothing left today, so find the next day the route runs
    for (let i = 1; i <= 7; i++) {
        const nextDay = (day + i) % 7;
        if (runsOn(nextDay)) {
            return {
                type: runsOn(day) ? 'OFF_HOURS' : 'WEEKEND',
                daysAhead: i,
                day: nextDay,
                time: schedule.windows[0][0]
            };
        }
    }
    return null;
}

// "Next service tomorrow at 6:00 AM" / "Próximo servicio mañana a las 6:00 AM"
function nextValidText(warning) {
    const time = to12h(warning.time);
    const dayName = DAY_NAMES[state.lang][warning.day];
    if (state.lang === 'es') {
        if (warning.daysAhead === 0) return 'Próximo servicio hoy a las ' + time;
        if (warning.daysAhead === 1) return 'Próximo servicio mañana a las ' + time;
        return 'Válido el ' + dayName + ' a las ' + time;
    }
    if (warning.daysAhead === 0) return 'Next service today at ' + time;
    if (warning.daysAhead === 1) return 'Next service tomorrow at ' + time;
    return 'Valid ' + dayName + ' at ' + time;
}

// Short "Sat 10:00 AM" for the strip at the top of the screen
function shortTime(date) {
    const day = DAY_NAMES[state.lang][date.getDay()].slice(0, 3);
    const hhmm = String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
    return day.charAt(0).toUpperCase() + day.slice(1) + ' ' + to12h(hhmm);
}

// Date -> '2026-11-07T10:00', the format a datetime-local input expects
function toInputValue(date) {
    const pad = function (n) {
        return String(n).padStart(2, '0');
    };
    return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
        'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

// Next given weekday at a given time, e.g. next Saturday at 10:00
function nextWeekday(targetDay, hours, minutes) {
    const date = new Date();
    date.setDate(date.getDate() + ((targetDay - date.getDay() + 7) % 7));
    date.setHours(hours, minutes, 0, 0);
    return date;
}

// "84" in the counter, orange when close to the limit, red at the limit
function updateCounter(counterId, text) {
    const counter = el(counterId);
    counter.textContent = text.length;
    counter.parentElement.classList.toggle('text-warning-emphasis', text.length >= NEAR_LIMIT && text.length < MAX_MESSAGE);
    counter.parentElement.classList.toggle('text-danger', text.length >= MAX_MESSAGE);
}

function render() {
    const now = state.clock ? new Date(state.clock) : new Date();
    const warning = getWarning(SCHEDULES[state.route], now);

    el('tvmRoute').textContent = el('route').selectedOptions[0].text;
    el('tvmTime').textContent = shortTime(now);

    // Show a note instead of a blank screen when a translation is missing
    const message = state.lang === 'es' ? state.messageEs : state.messageEn;
    const missingNote = state.lang === 'es' ? 'Missing Spanish translation' : 'Missing English message';
    el('tvmMessage').textContent = message || missingNote;
    el('tvmMessage').classList.toggle('tvm-missing', !message);
    el('tvmNextValid').textContent = warning ? nextValidText(warning) : '';

    el('tvmContinue').textContent = pick(state.continueEn, state.continueEs, 'Continue', 'Continuar');
    el('tvmBack').textContent = pick(state.backEn, state.backEs, 'Go back', 'Regresar');

    // Fixed screen text (titles, "Start over"...) has both languages in data-en / data-es
    document.querySelectorAll('[data-en]').forEach(function (node) {
        node.textContent = node.dataset[state.lang];
    });

    // Only one screen at a time
    const showing = state.screen === 'main' ? (warning ? 'warning' : 'ready') : state.screen;
    el('screenWarning').hidden = showing !== 'warning';
    el('screenReady').hidden = showing !== 'ready';
    el('screenPayment').hidden = showing !== 'payment';
    el('screenRoutes').hidden = showing !== 'routes';
    el('screenHome').hidden = showing !== 'home';

    updateCounter('countEn', state.messageEn);
    updateCounter('countEs', state.messageEs);

    // Highlight the language that's showing, and tell screen readers too
    el('langEn').classList.toggle('active', state.lang === 'en');
    el('langEs').classList.toggle('active', state.lang === 'es');
    el('langEn').setAttribute('aria-pressed', state.lang === 'en');
    el('langEs').setAttribute('aria-pressed', state.lang === 'es');
}

// Any edit sends the preview back to the first screen so you see the change
function update(key, value) {
    state[key] = value;
    state.screen = 'main';
    render();
}

// Every text box updates the preview as you type
const textFields = ['messageEn', 'messageEs', 'continueEn', 'continueEs', 'backEn', 'backEs'];
textFields.forEach(function (id) {
    state[id] = el(id).value; // start with the example text already in the form
    el(id).addEventListener('input', function (event) {
        update(id, event.target.value);
    });
});

el('route').addEventListener('change', function (event) {
    update('route', event.target.value);
});
el('clock').addEventListener('change', function (event) {
    update('clock', event.target.value);
});

// Quick time buttons for the demo
const PRESETS = {
    'sat-10': function () { return nextWeekday(6, 10, 0); },
    'wed-13': function () { return nextWeekday(3, 13, 0); },
    'wed-08': function () { return nextWeekday(3, 8, 0); },
    'now': function () { return new Date(); }
};
document.querySelectorAll('[data-preset]').forEach(function (button) {
    button.addEventListener('click', function () {
        const value = toInputValue(PRESETS[button.dataset.preset]());
        el('clock').value = value;
        update('clock', value);
    });
});

el('langEn').addEventListener('click', function () {
    state.lang = 'en';
    render();
});
el('langEs').addEventListener('click', function () {
    state.lang = 'es';
    render();
});

// Buttons on the TVM itself: go where the form says they go
el('tvmContinue').addEventListener('click', function () {
    state.screen = ACTION_SCREENS[el('continueAction').value];
    render();
});
el('tvmBack').addEventListener('click', function () {
    state.screen = ACTION_SCREENS[el('backAction').value];
    render();
});
el('tvmBuy').addEventListener('click', function () {
    state.screen = 'payment';
    render();
});
document.querySelectorAll('[data-restart]').forEach(function (button) {
    button.addEventListener('click', function () {
        state.screen = 'main';
        render();
    });
});

// Start on the selected route at the current time
state.route = el('route').value;
state.clock = toInputValue(new Date());
el('clock').value = state.clock;
render();
