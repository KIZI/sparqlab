(function ($) {
  $(document).ready(function () {
    var $controlButtons = $(".control-buttons"),
        $codeMirror = $(".CodeMirror");

    // YASQE and YASR
    var prefixes = {
      qb: "http://purl.org/linked-data/cube#",
      skos: "http://www.w3.org/2004/02/skos/core#"
    };
    var yasqe = YASQE.fromTextArea(document.getElementById("editor"), {
      sparql: {
        endpoint: "/api/query", 
        requestMethod: "GET"
      }
    });
    var yasr = YASR(document.getElementById("results"), {
      getUsedPrefixes: yasqe.getPrefixesFromQuery,
      outputPlugins: ["error", "boolean", "rawResponse", "table"],
      persistency: {
        results: null
      },
      useGoogleCharts: false
    });
    yasqe.setValue("\nSELECT * \nWHERE {\n  ?s ?p ?o .\n}\nLIMIT 10");
    yasqe.addPrefixes(prefixes);

    // Event handlers
    $controlButtons.delegate("#run-query", "click", function (e) {
      yasqe.query(yasr.setResponse);
    });
    $codeMirror.ready(function (e) {
      $controlButtons.removeClass("hidden");
    });
  });
})(jQuery);
