(defproject sparqlab "0.5.0"
  :description "Lab for exercising SPARQL" 
  :url "http://github.com/jindrichmynarz/sparqlab"
  :dependencies [[ring/ring-servlet "1.5.0"]
                 [luminus/ring-ttl-session "0.3.1"]
                 [org.clojure/clojure "1.8.0"]
                 [selmer "1.10.0"]
                 [markdown-clj "0.9.91"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.8.0"]
                 [bouncer "1.0.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.3"]
                 [org.webjars/font-awesome "4.7.0"]
                 [org.webjars.bower/tether "1.3.7"]
                 [org.webjars/jquery "2.2.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.5.1"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.1"]
                 [mount "0.1.10"]
                 [cprop "0.1.8"]
                 [org.clojure/tools.cli "0.3.5"]
                 [luminus-nrepl "0.1.4"]
                 [clj-http "3.1.0"]
                 [org.apache.jena/jena-core "3.1.1"]
                 [org.apache.jena/jena-arq "3.1.1"]
                 [org.apache.jena/jena-querybuilder "3.1.1"]
                 [cheshire "5.6.3"]
                 [stencil "0.5.0"]
                 [org.topbraid/spin "2.0.0"]
                 [slingshot "0.12.2"]
                 [luminus-immutant "0.2.2"]
                 [org.aksw.jena-sparql-api/jena-sparql-api-cache "3.1.1-1-SNAPSHOT"]]

  :repositories [["org.topbraid" "http://topquadrant.com/repository/spin"]
                 ["maven.aksw.snapshots" "http://maven.aksw.org/archiva/repository/snapshots"]]
  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main sparqlab.core

  :plugins [[lein-cprop "1.0.1"]
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
                                 [ring/ring-devel "1.5.0"]
                                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                                 [pjstadig/humane-test-output "0.8.1"]
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
