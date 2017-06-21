// Shared settings

var createCookie = function (name, value) {
  var date = new Date();
  // The cookie will expire in a year.
  date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
  var expires = "; expires=" + date.toUTCString();
  document.cookie = "sparqlab-exercise-" + name + "=" + value + expires + "; path=/";
}

// Global configuration for YASR
yasrConfig = {
  outputPlugins: ["error", "boolean", "rawResponse", "table"],
  persistency: {
    prefix: null
  },
  useGoogleCharts: false
};
YASR.plugins.table.defaults.fetchTitlesFromPreflabel = false;

var sparqlabLocale, yasqeLocale;
if (acceptLang === "cs") {
  YASR.plugins.table.defaults.datatable.language = {
    "url": "/localization/Czech.json"
  };
  sparqlabLocale = {
    expectedToken: "Očekáváno",
    syntaxError: "Chyba syntaxe dotazu",
    timeout: "Dotaz překročil maximální povolenou dobu provádění ({seconds} sekund)."
  };
  yasqeLocale = {
    invalidLine: "Na tomto řádku je chyba. Očekáváno:",
    setFullScreen: "Maximalizovat na celou obrazovku",
    setSmallScreen: "Minimalizovat",
    shareQuery: "Sdílení dotazu",
    shorten: "Zkrátit",
    autocomplete: {
      failedSuggestions: "Stahování dokončení selhalo...",
      fetching: "Stahování dokončení",
      nothing: "Nic k dokončení!",
      trigger: "Stiskněte CTRL - <mezerník> pro automatické dokončování",
      zeroMatches: "Žádná dokončení nenalezena..."
    }
  };
} else {
  sparqlabLocale = {
    expectedToken: "Expected",
    syntaxError: "Query syntax error",
    timeout: "The query exceeded the maximum allowed execution time ({seconds} seconds)."
  };
  yasqeLocale = YASQE.defaults.language;
}
