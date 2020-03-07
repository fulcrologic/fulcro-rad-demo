(ns com.example.rules
  (:require
    [clara.rules :as clara :refer [defrule defquery]]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.scheduling :as sched]
    [clojure.set :as set]
    [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General purpose functions that any app could use.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn table-diff
  "Calculate the facts that should be added and retracted from the rules session based on the changes in the
  Fulcro table at `table-key` between `old-state-map` and `new-state-map`. The `fact-constructor` must be a function
  that can take a table entry of that table and convert it to a Clara Rules fact (typically a defrecord instance). "
  [table-key fact-constructor old-state-map new-state-map]
  (let [old-table    (get old-state-map table-key)
        new-table    (get new-state-map table-key)
        old-item-ids (set (keys old-table))
        new-item-ids (set (keys new-table))
        removed-ids  (set/difference old-item-ids new-item-ids)
        added-ids    (set/difference new-item-ids old-item-ids)
        common-ids   (set/intersection old-item-ids new-item-ids)
        changed-ids  (filter #(not=
                                (get old-table %)
                                (get new-table %)) common-ids)]
    {:retracts (mapv (fn [id] (fact-constructor (get old-table id))) (concat removed-ids changed-ids))
     :adds     (mapv (fn [id] (fact-constructor (get new-table id))) (concat added-ids changed-ids))}))

(defn retract-all
  "Make up for the fact that Clara doesn't come with this function."
  [session fact-seq]
  (apply clara/retract session fact-seq))

(defn update-session
  "Given a rules session, a map from Fulcro table name to fact constructors (a function that can create a Clara Rules
  fact from a Fulcro db table entry), and the state maps from before
  and after: returns a new rules session with the facts updated about those tables."
  [session fact-constructors old-state-map new-state-map]
  (let [tables  (set (keys fact-constructors))
        updates (map
                  (fn [table]
                    (table-diff table (get fact-constructors table) old-state-map new-state-map))
                  tables)]
    (reduce
      (fn [sess {:keys [adds retracts]}]
        (log/debug "Inserting new facts" adds)
        (log/debug "Retracting old facts" retracts)
        (cond-> sess
          (seq adds) (clara/insert-all adds)
          (seq retracts) (retract-all retracts)))
      session
      updates)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DEMO STUFF SPECIFIC TO THIS APP
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; :item/id and :line-item/id tables in Fulcro will be represented by these Clara Fact records:
(defrecord Item [item-id price])
(defrecord LineItem [line-item-id item-id quantity quoted-price])

;; Data that the rules engine is meant to calculate. The output must be derived things we can query on.
(defrecord CalculatedSubtotal [line-item-id amount])

(def fact-constructors
  "A map from Fuclro table names to functions that can convert entries in those tables to Clara Facts."
  {:item/id      (fn [{:item/keys [id price]}] (->Item id price))
   :line-item/id (fn [{:line-item/keys [id item quantity quoted-price]}]
                   (->LineItem id (second item) quantity (or quoted-price (math/zero))))})

(defrule calculate-subtotal
  "Look for line items that have sufficient information for calculating a subtotal, and create truth-maitenance
  facts about what their subtotal amounts should be."
  [LineItem (= ?line-item-id line-item-id) (= ?item-id item-id) (= ?quantity quantity) (= ?quoted-price quoted-price)]
  [Item (= ?item-id item-id) (= ?price price)]
  [:test (> ?quantity 0)]
  =>
  (let [price  (if (math/> ?quoted-price (math/zero))
                 ?quoted-price
                 ?price)
        amount (math/round (math/* ?quantity price) 2)]
    (clara/insert! (->CalculatedSubtotal ?line-item-id amount))))

(defquery calculated-subtotals []
  [?subtotal <- CalculatedSubtotal])

(defn distribute-calculated-subtotals
  "Given a state map and a current clara session: Return a new state map with the Clara calulated subtotals
   distributed to the Fulcro database."
  [state-map session]
  (let [cs (clara/query session calculated-subtotals)]
    (reduce
      (fn [s {:keys [?subtotal] :as st}]
        (let [{:keys [line-item-id amount]} ?subtotal]
          (log/debug "Assigning amount" line-item-id " = " amount)
          (assoc-in s [:line-item/id line-item-id :line-item/subtotal] amount)))
      state-map
      cs)))

(defn process-rules!
  "Run the Clara rules engine against the Fulcro app. We're storing the session and \"old state\" in the Fulcro
  app runtime atom."
  [{::app/keys [state-atom runtime-atom]}]
  (log/info "Firing rules")
  (let [new-state-map @state-atom
        {::keys [old-state]} @runtime-atom]
    (swap! runtime-atom (fn [runtime]
                          (-> runtime
                            (dissoc ::old-state)
                            (update ::rules-session (fn [sess]
                                                      (-> sess
                                                        (update-session fact-constructors old-state new-state-map)
                                                        (clara/fire-rules)))))))
    (swap! state-atom distribute-calculated-subtotals (::rules-session @runtime-atom))))

(clara/defsession initial-session 'com.example.rules)

(defn install-rules-engine! [app]
  (let [{::app/keys [state-atom runtime-atom]} app]
    (swap! runtime-atom assoc ::rules-session initial-session)
    (add-watch state-atom ::rules-update (fn [_ _ old-state-map new-state-map]
                                           ;; CAREFUL: Our swap to distribute results will re-trigger this watch, but
                                           ;; if nothing changed we don't want to create a loop!
                                           (when-not (= old-state-map new-state-map)
                                             ;; Save the starting point for the rules session update, only once per
                                             ;; scheduled update, which we run only once every 500ms.
                                             (when-not (::old-state @runtime-atom)
                                               (swap! runtime-atom assoc ::old-state old-state-map))
                                             (sched/schedule! app ::fire-rules!
                                               (fn []
                                                 (process-rules! app)
                                                 ;; rendering is not based on state map updates, so we have to ask for one.
                                                 (app/schedule-render! app))
                                               500))))))
