(function ($) {
  var yasqeOptions = {
    createShareLink: false,
    readOnly: true,
    persistent: null
  };
  var textAreaToYasqe = function (id) {
    var textArea = document.getElementById(id);
    
    if (textArea) {
      var query = textArea.value,
          yasqe = YASQE.fromTextArea(textArea, yasqeOptions);
      yasqe.setValue(query);
      return yasqe;
    }
  };

  $(document).ready(function () {
    var query = textAreaToYasqe("query"),
        canonicalQuery = textAreaToYasqe("canonical-query"),
        queryResults = YASR(document.getElementById("query-results")),
        $results = $("#results"),
        canonicalQueryResults = YASR(document.getElementById("canonical-query-results")),
        $canonicalResults = $("#canonical-results");
    queryResults.setResponse({
      response: $results.val(),
      contentType: $results.data("type")
    });
    canonicalQueryResults.setResponse({
      response: $canonicalResults.val(),
      contentType: $canonicalResults.data("type")
    });
  });
})(jQuery);
