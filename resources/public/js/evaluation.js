(function ($) {
  var yasqeOptions = {
    createShareLink: false,
    language: yasqeLocale,
    lineNumbers: false,
    persistent: null,
    readOnly: true,
    theme: "sparqlab"
  };

  /** Create YASR using `config` and `queryData`. */
  var createYasr = function (config, queryData) {
    var yasr = YASR(queryData.results.$[0], config);
    yasr.setResponse({
      response: queryData.results.text,
      contentType: queryData.results.mimetype
    });
  };

  /** Format a `binding` from SPARQL results using pairs of prefixes and namespaces. */
  var formatBinding = function (prefixPairs, binding) {
    switch (binding.type) {
      case "literal":
        if (binding.hasOwnProperty("xml:lang")) {
          return '"' + binding.value + '"<sup>@' + binding["xml:lang"] + '</sup>';
        } else if (binding.hasOwnProperty("datatype")) {
          return ('"' + binding.value + '"<sup>^^<a href="' + binding["datatype"] +
                  '">' + formatIri(prefixPairs, binding["datatype"]) + '</a></sup>');
        } else {
          return '"' + binding.value + '"';
        }
      case "uri":
        return '<a href="' + binding.value + '">' + formatIri(prefixPairs, binding.value) + '</a>';
      default: return binding.value;
    }
  };

  /** Format an `iri` either to compact IRI using `prefixPairs`. */
  var formatIri = function (prefixPairs, iri) {
    // Try to compact the IRI using `prefixPairs`.
    for (var i = 0, len = prefixPairs.length; i < len; i++) {
      if (iri.indexOf(prefixPairs[i][1]) == 0) {
        return prefixPairs[i][0] + ":" + iri.substring(prefixPairs[i][1].length);
      }
    }
    // Otherwise return absolute IRI.
    return "&lt;" + iri + "&gt;";
  };

  /** Format results of SPARQL SELECT queries. */
  var formatSelectResults = function (prefixPairs, data) {
    var head = data.head.vars,
        bindings = data.results.bindings;
    return [head].concat(bindings.map(function (binding) {
      return head.map(function (variable) {
        return formatBinding(prefixPairs, binding[variable]);
      });
    }));
  };

  /** Extract exercise ID from the page URL. */
  var getExerciseId = function () {
    var path = document.location.pathname,
        index = path.lastIndexOf("/");
    if (index !== -1) {
      return path.substring(index + 1);
    }
  };

  /** Get data for a query in element with `queryId` and its results in element with `resultsId`. */
  var getQueryData = function (queryId, resultsId) {
    var queryElement = document.getElementById(queryId),
        query = queryElement.value,
        $results = $("#" + resultsId),
        textarea = $results.find("textarea"),
        mimetype = textarea.data("type"),
        text = textarea.val(),
        data = parseQueryResults(mimetype, text);
    return {
      query: {
        element: queryElement,
        query: query
      },
      results: {
        $: $results,
        data: data,
        mimetype: mimetype,
        text: text
      }
    };
  };

  /** Get namespace prefixes sorted from longer namespaces to shorter namespaces. */
  var getSortedPrefixes = function () {
    var prefixPairs = [];
    for (p in prefixes) {
      prefixPairs.push([p, prefixes[p]]);
    }
    // Sort prefixes by namespace length in descending order.
    prefixPairs.sort(function (a, b) {
      return b[1].length - a[1].length;
    });
    return prefixPairs;
  };

  /** Parse SPARQL query results in `text` based on its `mimetype`. */
  var parseQueryResults = function (mimetype, text) {
    return (mimetype === "application/json") ? JSON.parse(text) : text;
  };

  /** Render formatted SPARQL SELECT results in `data` to an HTML table. */
  var renderTable = function (data) {
    var wrapElement = function (element, body) {
          return "<" + element + ">" + body + "</" + element + ">";
        },
        formatVariable = wrapElement.bind(null, "th"),
        formatValue = wrapElement.bind(null, "td"),
        formatRow = function (row) {
          return wrapElement("tr", row.map(formatValue).join(""));
        },
        head = data[0].map(formatVariable).join(""),
        body = data.slice(1).map(formatRow).join("");
    return "<table><thead><tr>" +
           head +
           "</tr></thead><tbody>" +
           body +
           "</tbody></table>";
  };

  /** Show a merge view that displays the diff between `query` and `canonicalQuery`. */
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
    // Monkey-patch translation of CodeMirror's merge addon.
    $(".CodeMirror-merge-scrolllock").attr("title", sparqlabLocale.lockedScrolling);
  };

  /** Convert textarea `element` to YASQE. */
  var textAreaToYasqe = function (element) {
    if (element) {
      var query = element.value,
          yasqe = YASQE.fromTextArea(element, yasqeOptions);
      yasqe.setValue(query);
      return yasqe;
    }
  };

  $(document).ready(function () {
    var query = getQueryData("query", "query-results"),
        canonicalQuery = getQueryData("canonical-query", "canonical-query-results"),
        $revealSolution = $("#reveal-solution"),
        $mergeView = $("#mergeview");

    // Show YASQE if query is incorrect and solution not revealed.
    // Show MergeView if query is correct or solution is revealed.
    if ($mergeView.hasClass("hidden")) {
      textAreaToYasqe(query.query.element);
    } else {
      showMergeView($mergeView, query.query.query, canonicalQuery.query.query);
    }

    // .daff class is used when both results are SELECT results.
    if (query.results.$.hasClass("daff")) {
      // In that case we present a tabular diff.
      var prefixPairs = getSortedPrefixes(),
          queryData = formatSelectResults(prefixPairs, query.results.data),
          canonicalData = formatSelectResults(prefixPairs, canonicalQuery.results.data),
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
      query.results.$.html(tableDiffHtml);
      canonicalQuery.results.$.html(renderTable(canonicalData));
    } else {
      // Otherwise we show the results using YASR
      var config = $.extend(yasrConfig, {getUsedPrefixes: function () { return prefixes; }});
      createYasr(config, query);
      createYasr(config, canonicalQuery);
    }

    $revealSolution.on("click", function (e) {
      var exerciseId = getExerciseId();
      $.get("../../api/exercise-solution",
        {id: exerciseId},
        function (solution) {
          $(".yasqe").hide();
          showMergeView($mergeView, query.query.query, solution);
          $("#reveal-solution-button").hide();
        }
      );
    });
  });
})(jQuery);
