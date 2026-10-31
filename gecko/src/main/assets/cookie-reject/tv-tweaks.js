// Acorta animaciones y transiciones (sin eliminarlas, para no romper los eventos de la web)
// y así la página va más fluida en equipos lentos. Solo se aplica a DAZN (ver manifest.json).
(function () {
  var style = document.createElement('style');
  style.textContent = '*,*::before,*::after{animation-duration:0.001s!important;' +
    'animation-delay:0s!important;transition-duration:0.001s!important;' +
    'transition-delay:0s!important;scroll-behavior:auto!important}';
  (document.head || document.documentElement).appendChild(style);
})();
