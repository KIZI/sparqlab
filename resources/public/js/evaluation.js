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
    var query = textAreaToYasqe("query");
    var canonicalQuery = textAreaToYasqe("canonical-query");
  });
})(jQuery);
