(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :refer [equal-query?]]
            [sparqlab.util :refer [query-file?]]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]))

(add-filter! :markdown (fn [s] [:safe (md-to-html-string s)]))

(defn- remove-comment
  "Remove SPARQL comment sign from `line`."
  [line]
  (string/replace-first line #"^#\s*" ""))

(defn- remove-file-extension
  "Remove file extension from `file-name`."
  [file-name]
  (string/replace-first file-name #"\.[^.]+$" ""))

(defn parse-exercise
  [id]
  (letfn [(parse [[exercise-name exercise-description & query]]
            {:name (remove-comment exercise-name)
             :description (remove-comment exercise-description)
             :id id
             :query (string/trim (string/join \newline query))})]
    (-> (str "exercises/" id ".rq")
      io/resource
      io/reader
      line-seq
      parse)))

(def exercises
  (letfn [(parse-file [file]
            {:id (-> file .getName remove-file-extension)
             :name (-> file io/reader line-seq first remove-comment)})]
    (->> "exercises"
      io/resource
      io/as-file
      file-seq
      (filter query-file?)
      (map parse-file))))

(defn home-page
  []
  (layout/render "home.html" {:exercises exercises}))

(defn evaluate-exercise
  [id query]
  (let [{canonical-query :query
         :as exercise} (parse-exercise id)
        verdict (equal-query? canonical-query query)]
    (layout/render "evaluation.html" (merge exercise verdict))))

(defn show-exercise
  [id]
  (layout/render "exercise.html" (parse-exercise id)))

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
