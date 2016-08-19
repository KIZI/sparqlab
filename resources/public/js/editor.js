(function ($) {
  $(document).ready(function () {
    var $controlButtons = $(".control-buttons"),
        $controlButtonsControls = $controlButtons.find("button"),
        $codeMirror = $(".CodeMirror"),
        $results = $("#results"),
        $errorModal = $("#error-modal"),
        timeout = 3000;

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

    var yasr = YASR($results[0], jQuery.extend(yasrConfig, {
      getUsedPrefixes: yasqe.getPrefixesFromQuery
    }));
    var prefilledQuery = yasqe.getTextArea().value.trim();
    if (prefilledQuery !== "") {
      yasqe.setValue(yasqe.getTextArea().value.trim());
    } else {
      yasqe.addPrefixes(prefixes);
    }

    // Event handlers
    $codeMirror.ready(function (e) {
      $controlButtons.removeClass("hidden");
    });
    $controlButtons.delegate("#run-query", "click", function (e) {
      yasqe.query();
    });
    yasqe.options.sparql.callbacks = {
      error: function (xhr, textStatus, errorThrown) {
        if (textStatus === "timeout") {
          $errorModal
            .find(".modal-body .modal-message")
            .text("Dotaz překročil maximální povolenou dobu provádění (" + timeout / 1000 + " sekund).");
          $errorModal.modal("show");
        };
      },
      success: function () {
        $results.removeClass("hidden");
      },
      complete: function (xhr, textStatus) {
        yasr.setResponse(xhr, textStatus);
      },
      timeout: timeout
    };
    yasqe.on("change", function (e) {
      if (yasqe.queryValid) {
        $controlButtonsControls.removeClass("disabled");
      } else {
        $controlButtonsControls.addClass("disabled");
      }
    });
  });
})(jQuery);
