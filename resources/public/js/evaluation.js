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
      var exerciseId = getExerciseId();
      $.get("../../api/exercise-solution",
        {id: exerciseId},
        function (solution) {
          $canonicalQuery.val(solution);
          $canonicalQuery.removeClass("invisible");
          canonicalQuery = textAreaToYasqe($canonicalQuery[0]);
          $("#reveal-solution-button").hide();
        }
      );
    });
  });
})(jQuery);
