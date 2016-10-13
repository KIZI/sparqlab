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
  "sEmptyTable":     "Tabulka neobsahuje žádná data",
  "sInfo":           "Zobrazuji _START_ až _END_ z celkem _TOTAL_ záznamů",
  "sInfoEmpty":      "Zobrazuji 0 až 0 z 0 záznamů",
  "sInfoFiltered":   "(filtrováno z celkem _MAX_ záznamů)",
  "sInfoPostFix":    "",
  "sInfoThousands":  " ",
  "sLengthMenu":     "Zobraz záznamů _MENU_",
  "sLoadingRecords": "Načítám...",
  "sProcessing":     "Provádím...",
  "sSearch":         "Hledat:",
  "sZeroRecords":    "Žádné záznamy nebyly nalezeny",
  "oPaginate": {
    "sFirst":    "První",
    "sLast":     "Poslední",
    "sNext":     "Další",
    "sPrevious": "Předchozí"
  },
  "oAria": {
    "sSortAscending":  ": aktivujte pro řazení sloupce vzestupně",
    "sSortDescending": ": aktivujte pro řazení sloupce sestupně"
  }
};
