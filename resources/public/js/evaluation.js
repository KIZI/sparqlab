(function ($) {
  var yasqeOptions = {
    createShareLink: false,
    readOnly: true,
    persistent: null,
    theme: "sparqlab"
  };
  var textAreaToYasqe = function (element) {
    if (element) {
      var query = element.value,
          yasqe = YASQE.fromTextArea(element, yasqeOptions);
      yasqe.setValue(query);
      return yasqe;
    }
  };

  $(document).ready(function () {
    var query = textAreaToYasqe(document.getElementById("query")),
        $canonicalQuery = $("#canonical-query"),
        queryResults = YASR(document.getElementById("query-results"), yasrConfig),
        $results = $("#results"),
        canonicalQueryResults = YASR(document.getElementById("canonical-query-results"), yasrConfig),
        $canonicalResults = $("#canonical-results"),
        $revealSolution = $("#reveal-solution");
       
    if (!$canonicalQuery.hasClass("invisible")) {
      canonicalQuery = textAreaToYasqe($canonicalQuery[0]);
    }
    queryResults.setResponse({
      response: $results.val(),
      contentType: $results.data("type")
    });
    canonicalQueryResults.setResponse({
      response: $canonicalResults.val(),
      contentType: $canonicalResults.data("type")
    });
    $revealSolution.on("click", function (e) {
      $canonicalQuery.removeClass("invisible");
      canonicalQuery = textAreaToYasqe($canonicalQuery[0]);
      $("#reveal-solution-button").hide();
    });
  });
})(jQuery);
