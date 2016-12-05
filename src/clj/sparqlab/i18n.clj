(ns sparqlab.i18n
  (:require [taoensso.tempura :as tempura :refer [tr]]))

(def tconfig
  {:default-locale :cs
   :dict {:cs {:missing "Chybějící český text"
               :about {:title "O SPARQLabu"}
               :data {:title "Data SPARQLabu"}
               :endpoint {:title "SPARQL endpoint"}
               :error {:title "Chyba"}
               :evaluation {:title "Vyhodnocení cvičení: %1"}
               :exercises-by-category {:title "Cvičení dle kategorií"}
               :exercises-by-difficulty {:title "Cvičení dle obtížnosti"}
               :exercises-by-language-constructs {:title "Cvičení dle jazykových konstruktů"}
               :internal-error {:title "Chyba aplikace"
                                :message "Interní chyba aplikace"}
               :not-found {:title "Stránka nenalezena"}
               :search-results {:title "Nalezená cvičení"}
               :sparql-syntax-error {:title "Syntaktická chyba ve SPARQL dotazu"}}}})
