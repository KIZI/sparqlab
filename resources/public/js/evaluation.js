(function ($) {
  var yasqeOptions = {
    createShareLink: false,
    language: yasqeLocale,
    lineNumbers: false,
    persistent: null,
    readOnly: true,
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
  var getExerciseId = function () {
    var path = document.location.pathname,
        index = path.lastIndexOf("/");
    if (index !== -1) {
      return path.substring(index + 1);
    } 
  };
  var showMergeView = function ($mergeView, query, canonicalQuery) {
    $mergeView.removeClass("hidden");
    CodeMirror.MergeView($mergeView[0], { 
      connect: "align",
      mode: "sparql11",
      origLeft: query,
      revertButtons: false,
      theme: "sparqlab",
      value: canonicalQuery
    });
    $(".CodeMirror-merge-scrolllock").attr("title", sparqlabLocale.lockedScrolling);
  };

  $(document).ready(function () {
    var query = document.getElementById("query"),
        canonicalQuery = document.getElementById("canonical-query"),
        queryResults = YASR(document.getElementById("query-results"), yasrConfig),
        $results = $("#results"),
        canonicalQueryResults = YASR(document.getElementById("canonical-query-results"), yasrConfig),
        $canonicalResults = $("#canonical-results"),
        $revealSolution = $("#reveal-solution"),
        $mergeView = $("#mergeview");
       
    // Show YASQE if query is incorrect and solution not revealed.
    // Show MergeView if query is correct or solution is revealed.
    if ($mergeView.hasClass("hidden")) {
      textAreaToYasqe(query);
    } else {
      showMergeView($mergeView, query.value, canonicalQuery.value);
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
      var exerciseId = getExerciseId();
      $.get("../../api/exercise-solution",
        {id: exerciseId},
        function (solution) {
          $(".yasqe").hide();
          showMergeView($mergeView, query.value, solution);
          $("#reveal-solution-button").hide();
        }
      );
    });
  });
})(jQuery);
