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
YASR.plugins.table.defaults.datatable.language = {
  "url": "/localization/Czech.json"
};
