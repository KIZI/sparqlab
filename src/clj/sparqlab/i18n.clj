(ns sparqlab.i18n
  (:require [sparqlab.util :as util]
            [taoensso.tempura :refer [tr]]))

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
                            :title "Vyhodnocení cvičení: %1"
                            :your-answer "Vaše odpověď"}
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
                                     :expected-label "Očekáváno"}}
          :en {:missing "Missing English text"
               :about {:title "About"}
               :and "and"
               :base {:beta-version "Beta version"
                      :by-category "By categories"
                      :by-difficulty "By difficulty"
                      :by-language-constructs "By language constructs"
                      :data-title "Data"
                      :exercise "Exercises"
                      :search "Search"
                      :search-query "Search term"
                      :sparql-endpoint "SPARQL endpoint"}
               :close "Close"
               :data {:title "Data"}
               :endpoint {:title "SPARQL endpoint"
                          :run-query "Run the query"}
               :error {:title "Error"}
               :error-modal {:label "Error in query execution"
                             :message "There was an error in query execution."}
               :evaluation {:confirm-reveal {:label "Confirm revealing of the solution"
                                             :no "No"
                                             :question "Are you sure you want to reveal the solution?"
                                             :yes "Yes"}
                            :correct-answer "Right answer"
                            :incorrect-answer "Wrong answer"
                            :missing-constructs "Missing constructs"
                            :prohibited-constructs "Prohibited constructs"
                            :results "Results"
                            :reveal-solution "Reveal solution"
                            :solution "Solution"
                            :title "Exercise evaluation: %1"
                            :your-answer "Your answer"}
               :exercises {:difficulty "difficulty"
                           :note-title "Note"
                           :prohibited-constructs "Prohibited constructs"
                           :required-constructs "Required constructs"
                           :revealed "Revealed"
                           :similar-exercises "Related exercises"
                           :solved "Solved"
                           :submit "Submit"}
               :exercises-by-category {:title "Exercises by category"}
               :exercises-by-difficulty {:title "Exercises by difficulty"}
               :exercises-by-language-constructs {:note "The exercises are sorted by the number of language constructs they require; starting with those that require the least."
                                                  :title "Exercises by language constructs"}
               :internal-error {:message "Internal application error"
                                :title "Application error"}
               :loading "Loading"
               :not-found {:title "Page not found"}
               :search-results {:not-found "No exercises were found."
                                :search-query "Search term"
                                :search-query-constructs "Searched SPARQL constructs"
                                :title "Exercises found"}
               :sparql-syntax-error {:title "Invalid query syntax"
                                     :expected-label "Expected"}}}})

(defn base-locale
  [{lang :accept-lang}]
  (let [dict (get-in tconfig [:dict (keyword lang)])]
    (merge (:base dict)
           (util/select-nested-keys dict
                                    [[:and]
                                     [:about :title]
                                     [:close]
                                     [:data :title]
                                     [:endpoint :title]
                                     [:loading]]))))

(defn base-exercise-locale
  [{lang :accept-lang}]
  (let [dict (get-in tconfig [:dict (keyword lang)])]
    (merge (:exercises dict)
           (util/select-nested-keys dict [[:endpoint :run-query]
                                          [:error-modal :label]
                                          [:error-modal :message]]))))

(defn base-evaluation-locale
  [{lang :accept-lang}]
  (get-in tconfig [:dict (keyword lang) :evaluation]))

(defn base-search-locale
  [{lang :accept-lang}]
  (get-in tconfig [:dict (keyword lang) :search-results]))
