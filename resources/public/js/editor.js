(function ($) {
  $(document).ready(function () {
    var $controlButtons = $(".control-buttons"),
        $codeMirror = $(".CodeMirror"),
        $results = $("#results");

    // YASQE and YASR
    var prefixes = {
      qb: "http://purl.org/linked-data/cube#",
      skos: "http://www.w3.org/2004/02/skos/core#"
    };
    var yasqe = YASQE.fromTextArea(document.getElementById("editor"), {
      autofocus: true,
      sparql: {
        endpoint: "/api/query",
        requestMethod: "GET"
      },
      tabSize: 2,
      theme: "sparqlab"
    });

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

    var yasr = YASR($results[0], {
      getUsedPrefixes: yasqe.getPrefixesFromQuery,
      outputPlugins: ["error", "boolean", "rawResponse", "table"],
      persistency: {
        prefix: null
      },
      useGoogleCharts: false
    });
    yasqe.setValue("\nSELECT * \nWHERE {\n  ?s ?p ?o .\n}\nLIMIT 10");
    yasqe.addPrefixes(prefixes);

    // Event handlers
    $codeMirror.ready(function (e) {
      $controlButtons.removeClass("hidden");
    });
    $controlButtons.delegate("#run-query", "click", function (e) {
      yasqe.query();
    });
    yasqe.options.sparql.callbacks = {
      success: function () {
        $results.removeClass("hidden");
      },
      complete: function (xhr, textStatus) {
        yasr.setResponse(xhr, textStatus);
      },
    };
  });
})(jQuery);
