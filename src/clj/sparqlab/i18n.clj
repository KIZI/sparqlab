(ns sparqlab.i18n
  (:require [taoensso.tempura :as tempura :refer [tr]]))

(def tconfig
  {:default-locale :cs
   :dict {:cs {:missing "Chybějící český text"
               :about {:title "O aplikaci"}
               :and "a"
               :base {:beta-version "Beta verze"
                      :by-category "Dle kategorií"
                      :by-difficulty "Dle obtížnosti"
                      :by-language-constructs "Dle jazykových konstruktů"
                      :data-title "O datech"
                      :exercise "Cvičení"
                      :search "Hledat"
                      :search-query "Hledaný výraz"
                      :sparql-endpoint "SPARQL endpoint"}
               :close "Zavřít"
               :data {:title "Data"}
               :endpoint {:title "SPARQL endpoint"
                          :run-query "Spustit dotaz"}
               :error {:title "Chyba"}
               :error-modal {:label "Chyba v provádění dotazu"
                             :message "Došlo k chybě v provádění dotazu."}
               :evaluation {:confirm-reveal {:label "Potvrdit prozrazení řešení"
                                             :no "Ne"
                                             :question "Opravdu chcete prozradit řešení?"
                                             :yes "Ano"}
                            :correct-answer "Správná odpověď"
                            :incorrect-answer "Nesprávná odpověď"
                            :missing-constructs "Chybějící konstrukty"
                            :prohibited-constructs "Zakázané konstrukty"
                            :results "Výsledky"
                            :reveal-solution "Prozradit řešení"
                            :solution "Řešení"
                            :title "Vyhodnocení cvičení: %1"}
               :exercises {:difficulty "obtížnost"
                           :note-title "Poznámka"
                           :prohibited-constructs "Zakázané konstrukty"
                           :required-constructs "Povinné konstrukty"
                           :revealed "Prozrazeno"
                           :similar-exercises "Podobná cvičení"
                           :solved "Vyřešeno"
                           :submit "Odeslat"}
               :exercises-by-category {:title "Cvičení dle kategorií"}
               :exercises-by-difficulty {:title "Cvičení dle obtížnosti"}
               :exercises-by-language-constructs {:note "Nejprve jsou uvedena cvičení využívající méně konstruktů"
                                                  :title "Cvičení dle jazykových konstruktů"}
               :internal-error {:message "Interní chyba aplikace"
                                :title "Chyba aplikace"}
               :loading "Načítám"
               :not-found {:title "Stránka nenalezena"}
               :search-results {:not-found "Žádná cvičení nebyla nalezena."
                                :search-query "Hledaný výraz"
                                :search-query-constructs "Hledané SPARQL konstrukty"
                                :title "Nalezená cvičení"}
               :sparql-syntax-error {:title "Chyba syntaxe dotazu"
                                     :expected-label "Očekáváno"}}}})
