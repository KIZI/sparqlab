(ns sparqlab.test.handler
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [ring.mock.request :refer :all]
            [sparqlab.handler :refer :all]))

(use-fixtures :once (fn [f] (init) (f) (destroy)))

(deftest ^:integration test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))
