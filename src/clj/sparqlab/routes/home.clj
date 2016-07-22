(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :as sparql]
            [sparqlab.store :refer [select-query]]
            [sparqlab.prefixes :as prefix]
            [sparqlab.util :refer [query-file?]]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]
            [stencil.core :refer [render-file]]
            [stencil.loader :refer [set-cache]]
            [clojure.string :as string]))

(add-filter! :markdown (fn [s] [:safe (md-to-html-string s)]))

; Disable caching for testing
(set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

(def a-year
  "One year in seconds"
  (* 60 60 24 365))

(def cookie-ns
  "sparqlab-exercise-")

(defn set-cookie
  [response k value & {:keys [max-age]
                       :or {max-age a-year}
                       :as data}]
  (assoc-in response
            [:cookies k]
            (assoc data :value value)))

(defn mark-exercise-as-done
  [response base-url id]
  (set-cookie response (str cookie-ns id) true :domain base-url :path "/"))

(defn mark-exercises-as-done
  [exercises exercises-done]
  (map (fn [{:keys [id]
             :as exercise}]
         (if (exercises-done id)
           (assoc exercise :done true)
           exercise))
       exercises))

(defn ->plain-literals
  [bindings]
  (into {} (map (fn [[variable value]] [variable (get value "@value")]) bindings)))

(defn sparql-template
  "Render a SPARQL template from `file-name` using optional `data`."
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

(defn get-exercises-done
  [request]
  (->> (:cookies request)
       (filter (every-pred (comp #(string/starts-with? % cookie-ns) key)
                           (comp (partial = "true") :value val)))
       (map (comp #(subs % (count cookie-ns)) key))
       (into #{})))

(defn home-page
  [request]
  (let [exercises-done (get-exercises-done request)]
    (layout/render "home.html" {:exercises (mark-exercises-as-done (get-exercises) exercises-done)})))

(defn evaluate-exercise
  [{{query "query"} :form-params
    {base-url :host} :base-url}
   id]
  (let [{canonical-query :query
         :as exercise} (get-exercise id)
        verdict (sparql/evaluate-exercise canonical-query query)]
    (cond-> (layout/render "evaluation.html" (merge exercise verdict))
      (:equal? verdict) (mark-exercise-as-done base-url id))))

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
  (GET "/" request (home-page request))
  (context "/exercise" []
           (GET "/show/:id" [id] (show-exercise id))
           (POST "/evaluate/:id" [id :as request] (evaluate-exercise request id)))
  (GET "/endpoint" [] (sparql-endpoint))
  (GET "/about" [] (about-page)))
