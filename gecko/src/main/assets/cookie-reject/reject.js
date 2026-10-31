// Rechaza automáticamente los avisos de cookies: solo pulsa botones de "rechazar",
// nunca de "aceptar". Se ejecuta unos segundos tras cargar cada página.
(function () {
  if (window.__atvRejectCookies) { return; }
  window.__atvRejectCookies = true;

  // Botones conocidos de rechazo (OneTrust, banner de Amazon).
  var SELECTORS = [
    '#onetrust-reject-all-handler',
    '#sp-cc-rejectall-link'
  ];

  // Los botones por texto solo cuentan si están dentro de un aviso de cookies/consentimiento.
  var CONTAINERS = '[id*="cookie" i],[class*="cookie" i],[id*="consent" i],[class*="consent" i],' +
    '[id*="onetrust" i],[id*="sp-cc" i],[id*="privacy" i],[class*="privacy" i],' +
    '[role="dialog"],[aria-modal="true"]';

  var REJECT = /^(rechazar( todas?| todo)?( (las )?cookies( opcionales| no esenciales)?)?|rechazar (opcionales|no esenciales)|continuar sin aceptar|solo (las )?(cookies )?(necesarias|esenciales)|reject( all)?( (non-?essential|optional))?( cookies)?|decline( all)?( optional)?( cookies)?|deny( all)?|only (necessary|essential)( cookies)?|refuse( all)?)$/i;

  function visible(el) {
    return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
  }

  function normalize(text) {
    return (text || '').replace(/[\s ]+/g, ' ').replace(/[.!?¡¿]+$/g, '').trim();
  }

  function tryReject() {
    var i;
    for (i = 0; i < SELECTORS.length; i++) {
      var el = document.querySelector(SELECTORS[i]);
      if (el && visible(el)) {
        el.click();
        return true;
      }
    }
    var nodes = document.querySelectorAll(
      'button, a, [role="button"], input[type="button"], input[type="submit"]');
    for (i = 0; i < nodes.length; i++) {
      var n = nodes[i];
      var label = normalize(n.innerText || n.value || n.getAttribute('aria-label'));
      if (label && REJECT.test(label) && visible(n) && n.closest(CONTAINERS)) {
        n.click();
        return true;
      }
    }
    return false;
  }

  var observer = null;
  var finished = false;

  function run() {
    if (finished) { return; }
    if (tryReject()) {
      finished = true;
      if (observer) { observer.disconnect(); }
    }
  }

  observer = new MutationObserver(run);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  run();
  setTimeout(function () {
    finished = true;
    if (observer) { observer.disconnect(); }
  }, 30000);
})();
