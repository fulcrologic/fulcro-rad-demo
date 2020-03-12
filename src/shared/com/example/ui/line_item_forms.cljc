(ns com.example.ui.line-item-forms
  (:require
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.example.model :as model]
    [com.example.model.line-item :as line-item]
    [com.example.ui.item-forms :as item-forms]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.form :as form]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]))

(defn add-subtotal* [{:line-item/keys [quantity quoted-price] :as item}]
  (assoc item :line-item/subtotal (math/* quantity quoted-price)))

(form/defsc-form LineItemForm [this props]
  {::form/id            line-item/id
   ::form/attributes    [line-item/item line-item/quantity line-item/quoted-price line-item/subtotal]
   ::form/validator     model/all-attribute-validator
   ::form/cancel-route  ["landing-page"]
   ::form/route-prefix  "line-item"
   ::form/title         "Line Items"
   ::form/layout        [[:line-item/item :line-item/quantity :line-item/quoted-price :line-item/subtotal]]
   ::form/triggers      {;; TASK: Cascading dropdowns, with load of column!!!
                         ;; 1. Make: clear list price and model, populate picker options
                         ;; 2. Model: trigger load of list price
                         ;; 3. List Price populated from load
                         :derive-fields (fn [new-form-tree] (add-subtotal* new-form-tree))
                         :on-change     (fn [{::uism/keys [state-map] :as uism-env} form-ident k old-value new-value]
                                          (case k
                                            :line-item/item
                                            (let [item-price  (get-in state-map (conj new-value :item/price))
                                                  target-path (conj form-ident :line-item/quoted-price)]
                                              (uism/apply-action uism-env assoc-in target-path item-price))
                                            uism-env))}
   ::form/field-styles  {:line-item/item :pick-one}
   ::form/field-options {:line-item/item {::picker-options/query-key       :item/all-items
                                          ::picker-options/query-component item-forms/ItemForm
                                          ::picker-options/options-xform   (fn [_ options]
                                                                             (mapv
                                                                               (fn [{:item/keys [id name price]}]
                                                                                 {:text (str name " - " (math/numeric->currency-str price)) :value [:item/id id]})
                                                                               (sort-by :item/name options)))
                                          ::picker-options/cache-time-ms   100}}})
