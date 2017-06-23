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
  var isSelectResult = function (mimetype, text) {
    if (mimetype === "application/json") {
      var data = JSON.parse(text);
      return (data.hasOwnProperty("head") &&
              data.head.hasOwnProperty("vars") &&
              data.head.vars.length > 0);
    } else {
      return false;
    }
  };
  var formatBinding = function (binding) {
    switch (binding.type) {
      case "literal":
        if (binding.hasOwnProperty("xml:lang")) {
          return '"' + binding.value + '"<sup>@' + binding["xml:lang"] + '</sup>';
        } else if (binding.hasOwnProperty("datatype")) {
          return '"' + binding.value + '"<sup>^^' + binding["datatype"] + '</sup>';
        } else {
          return '"' + binding.value + '"';
        }
      case "uri":
        return '<a href="' + binding.value + '">' + binding.value + '</a>';
      default: return binding.value;
    }
  };
  var parseSelectResults = function (data) {
    var head = data.head.vars,
        bindings = data.results.bindings;
    return [head].concat(bindings.map(function (binding) {
      return head.map(function (variable) { return formatBinding(binding[variable]); });
    }));
  };
  var renderTable = function (data) {
    var head = data[0].map(function (variable) {
          return "<th>" + variable + "</th>"; }
        ).join(""),
        body = data.slice(1).map(function (row) {
          return "<tr>" + row.map(function (value) { return "<td>" + value + "</td>"; }).join("") + "</tr>";
        }).join("");
    return "<table><thead><tr>" +
           head +
           "</tr></thead><tbody>" +
           body +
           "</tbody></table>";
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
    var json = "application/json",
        query = document.getElementById("query"),
        canonicalQuery = document.getElementById("canonical-query"),
        $qr = $("#results"),
        qrType = $qr.data("type"),
        qr = (qrType === json) ? JSON.parse($qr.val()) : $qr.val();
        $qrResults = $("#query-results"),
        $cr = $("#canonical-results"),
        crType = $cr.data("type"),
        cr = (crType === json) ? JSON.parse($cr.val()) : $cr.val();
        $crResults = $("#canonical-query-results"),
        $revealSolution = $("#reveal-solution"),
        $mergeView = $("#mergeview");
       
    // Show YASQE if query is incorrect and solution not revealed.
    // Show MergeView if query is correct or solution is revealed.
    if ($mergeView.hasClass("hidden")) {
      textAreaToYasqe(query);
    } else {
      showMergeView($mergeView, query.value, canonicalQuery.value);
    }
  
    // .daff class is used when both results are SELECT results.
    if ($qrResults.hasClass("daff")) {
      // In that case we present a tabular diff. 
      var queryData = parseSelectResults(qr),
          canonicalData = parseSelectResults(cr),
          queryTable = new daff.TableView(queryData),
          canonicalTable = new daff.TableView(canonicalData);
      queryTable.trim();
      canonicalTable.trim();
      var alignment = daff.compareTables(canonicalTable, queryTable).align(),
          tableDiff = new daff.TableView([]);
          flags = new daff.CompareFlags(),
      flags.show_unchanged = false;
      flags.always_show_header = true;
      (new daff.TableDiff(alignment,flags)).hilite(tableDiff);
      var diff2html = new daff.DiffRender();
      diff2html.render(tableDiff);
      var tableDiffHtml = diff2html.html();
      $qrResults.html(tableDiffHtml);
      $crResults.html(renderTable(canonicalData));
    } else {
      // Otherwise we show the results using YASR
      var qrYasr = YASR($qrResults[0], yasrConfig),
          crYasr = YASR($crResults[0], yasrConfig);
      qrYasr.setResponse({
        response: qr,
        contentType: qrType
      });
      crYasr.setResponse({
        response: cr,
        contentType: crType
      });
    }

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
