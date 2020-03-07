(ns com.example.rules
  (:require
    [clara.rules :as clara :refer [defrule defquery]]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.scheduling :as sched]
    [clojure.set :as set]
    [taoensso.timbre :as log]))

(defn table-diff [table-key fact-constructor old-state-map new-state-map]
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

(defn retract-all [session fact-seq]
  (apply clara/retract session fact-seq))

(defn update-session
  "Given a rules session, a map from table name to fact constructors, and the state maps from before
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

;; Stuff in the normal database
(defrecord Item [item-id price])
(defrecord LineItem [line-item-id item-id quantity quoted-price])

;; Data that the rules engine is meant to calculate
(defrecord CalculatedSubtotal [line-item-id amount])

(def fact-constructors
  {:item/id      (fn [{:item/keys [id price]}] (->Item id price))
   :line-item/id (fn [{:line-item/keys [id item quantity quoted-price]}]
                   (->LineItem id (second item) quantity (or quoted-price (math/zero))))})

(defrule calculate-subtotal
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

(defn distribute-calculated-subtotals [state-map session]
  (let [cs (clara/query session calculated-subtotals)]
    (reduce
      (fn [s {:keys [?subtotal] :as st}]
        (let [{:keys [line-item-id amount]} ?subtotal]
          (log/debug "Assigning amount" line-item-id " = " amount)
          (assoc-in s [:line-item/id line-item-id :line-item/subtotal] amount)))
      state-map
      cs)))

(defn process-rules! [{::app/keys [state-atom runtime-atom]}]
  (log/info "Firing rules")
  (let [new-state-map @state-atom
        {::keys [old-state rules-session]} @runtime-atom]
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
                                           (when-not (= old-state-map new-state-map)
                                             ;; Save the starting point for the rules session update, only once per
                                             ;; scheduled update
                                             (when-not (::old-state @runtime-atom)
                                               (swap! runtime-atom assoc ::old-state old-state-map))
                                             (sched/schedule! app ::fire-rules!
                                               (fn []
                                                 (process-rules! app)
                                                 (app/schedule-render! app))
                                               500))))))
