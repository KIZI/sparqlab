(ns sparqlab.layout
  (:require [selmer.parser :as parser]
            [selmer.filters :as filters]
            [markdown.core :refer [md-to-html-string]]
            [ring.util.http-response :refer [content-type ok]]))

(declare ^:dynamic *app-context*)

(parser/set-resource-path!  (clojure.java.io/resource "templates"))

(filters/add-filter! :markdown (fn [content] [:safe (md-to-html-string content)]))

(filters/add-filter! :inline-markdown
                     (fn [content]
                       (let [wrapped (md-to-html-string content)]
                         [:safe (subs wrapped 3 (- (count wrapped) 4))])))

(defn render
  "renders the HTML template located relative to resources/templates"
  [template & [params]]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :page template
          :servlet-context *app-context*)))
    "text/html; charset=utf-8"))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details)})
