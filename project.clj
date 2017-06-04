(defproject sparqlab "0.5.0"
  :description "Lab for exercising SPARQL" 
  :url "http://github.com/jindrichmynarz/sparqlab"
  :dependencies [[ring/ring-servlet "1.6.1"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [org.clojure/clojure "1.8.0"]
                 [selmer "1.10.7"]
                 [markdown-clj "0.9.99"]
                 [ring-middleware-format "0.7.2"]
                 [metosin/ring-http-response "0.9.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.6-1"]
                 [org.webjars/font-awesome "4.7.0"]
                 [org.webjars.bower/tether "1.4.0"]
                 [org.webjars/jquery "2.2.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.6.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-defaults "0.3.0"]
                 [mount "0.1.11"]
                 [cprop "0.1.10"]
                 [org.clojure/tools.cli "0.3.5"]
                 [luminus-nrepl "0.1.4"]
                 [clj-http "3.6.0"]
                 [org.apache.jena/jena-core "3.3.0"]
                 [org.apache.jena/jena-arq "3.3.0"]
                 [org.apache.jena/jena-querybuilder "3.3.0"]
                 [cheshire "5.7.1"]
                 [stencil "0.5.0"]
                 [org.topbraid/spin "2.0.0"]
                 [slingshot "0.12.2"]
                 [luminus-immutant "0.2.3"]
                 [org.aksw.jena-sparql-api/jena-sparql-api-cache "3.1.1-1"]
                 [com.taoensso/tempura "1.1.2"]]

  :repositories [["org.topbraid" "http://topquadrant.com/repository/spin"]
                 ["maven.aksw.snapshots" "http://maven.aksw.org/archiva/repository/snapshots"]]
  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main sparqlab.core

  :plugins [[lein-cprop "1.0.3"]
            [lein-immutant "2.1.0"]
            [lein-uberwar "0.2.0"]]
  :uberwar
  {:handler sparqlab.handler/app
   :init sparqlab.handler/init
   :destroy sparqlab.handler/destroy
   :name "sparqlab.war"}
  
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}
  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "sparqlab.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.4"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.6.1"]
                                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                                 [pjstadig/humane-test-output "0.8.2"]
                                 [directory-naming/naming-java "0.8"]]
                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:dependencies   [[org.clojure/test.check "0.9.0"]]
                  :plugins        [[com.jakemccrary/lein-test-refresh "0.14.0"]]
                  :resource-paths ["env/dev/resources" "env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
