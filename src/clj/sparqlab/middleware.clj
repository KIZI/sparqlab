(ns sparqlab.middleware
  (:require [sparqlab.env :refer [defaults]]
            [sparqlab.config :refer [env]]
            [sparqlab.layout :refer [*app-context* error-page]]
            [sparqlab.i18n :refer [tconfig]]
            [clojure.tools.logging :as log]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.tempura :as tempura])
  (:import (javax.servlet ServletContext)))

(defn wrap-context
  [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (let [tr (:tempura/tr req)
              message {:status 500
                       :title (tr [:internal-error/title])
                       :message (tr [:internal-error/message])}]
          (error-page req message))))))

(defn wrap-formats
  [handler]
  (let [wrapped (wrap-restful-format
                  handler
                  {:formats [:json-kw :transit-json :transit-msgpack]})]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-accept-language
  [handler]
  (let [{:keys [default-locale dict]} tconfig
        available-locales (set (map name (keys dict)))]
    (fn [{accept-languages :tempura/accept-langs
          :as request}]
      (let [accept-lang (or (some available-locales accept-languages) (name default-locale))]
        (handler (assoc request :accept-lang accept-lang))))))

(defn wrap-base
  [handler]
  (-> ((:middleware defaults) handler)
      wrap-webjars
      (wrap-defaults
        (-> site-defaults (assoc-in [:session :store] (ttl-memory-store (* 60 30)))
                          (assoc-in [:security :anti-forgery] false)))
      wrap-context
      wrap-accept-language
      (tempura/wrap-ring-request {:tr-opts tconfig})
      wrap-internal-error))
