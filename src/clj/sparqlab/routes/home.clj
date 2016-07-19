(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :refer [equal-query?]]
            [sparqlab.store :refer [select-query]]
            [sparqlab.prefixes :as prefix]
            [sparqlab.util :refer [query-file?]]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]
            [stencil.core :refer [render-file]]
            [stencil.loader :refer [set-cache]]))

(add-filter! :markdown (fn [s] [:safe (md-to-html-string s)]))

; Disable caching for testing
(set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

(defn ->plain-literals
  [bindings]
  (into {} (map (fn [[variable value]] [variable (get value "@value")]) bindings)))

(defn sparql-template
  ([file-name]
   (sparql-template file-name {}))
  ([file-name data]
   (render-file (str "sparql/" file-name ".mustache") data)))

(defn get-exercise
  [id]
  (-> (sparql-template "get_exercise" {:exercise (prefix/exercise id)})
      select-query
      first
      ->plain-literals))

(defn get-exercises
  []
  (->> "get_exercises"
       sparql-template
       select-query
       (map ->plain-literals)))

(defn home-page
  []
  (layout/render "home.html" {:exercises (get-exercises)}))

(defn evaluate-exercise
  [id query]
  (let [{canonical-query :query
         :as exercise} (get-exercise id)
        verdict (equal-query? canonical-query query)]
    (layout/render "evaluation.html" (merge exercise verdict))))

(defn show-exercise
  [id]
  (layout/render "exercise.html" (get-exercise id)))

(defn sparql-endpoint
  []
  (layout/render "endpoint.html"))

(defn about-page
  []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (context "/exercise" []
           (GET "/show/:id" [id] (show-exercise id))
           (POST "/evaluate/:id" [id :as request]
                 (let [query (get-in request [:form-params "query"])]
                   (evaluate-exercise id query))))
  (GET "/endpoint" [] (sparql-endpoint))
  (GET "/about" [] (about-page)))
