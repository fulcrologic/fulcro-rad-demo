(ns com.example.ui.line-item-forms
  (:require
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.example.model :as model]
    [com.example.model.category :as category]
    [com.example.model.line-item :as line-item]
    [com.example.ui.item-forms :as item-forms]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as fns]))

(defn add-subtotal* [{:line-item/keys [quantity quoted-price] :as item}]
  (assoc item :line-item/subtotal (math/* quantity quoted-price)))

(form/defsc-form LineItemForm [this props]
  {fo/id            line-item/id
   fo/attributes    [line-item/category line-item/item line-item/quantity line-item/quoted-price line-item/subtotal]
   fo/validator     model/all-attribute-validator
   fo/route-prefix  "line-item"
   fo/title         "Line Items"
   fo/layout        [[:line-item/category :line-item/item :line-item/quantity :line-item/quoted-price :line-item/subtotal]]
   fo/triggers      {:derive-fields (fn [new-form-tree] (add-subtotal* new-form-tree))
                     :on-change     (fn [{::uism/keys [state-map fulcro-app] :as uism-env} form-ident k old-value new-value]
                                      (case k
                                        :line-item/category
                                        (let [cls         (comp/ident->any fulcro-app form-ident)
                                              target-path (conj form-ident :line-item/item)
                                              props       (fns/ui->props state-map cls form-ident)]
                                          (picker-options/load-options! fulcro-app cls props line-item/item)
                                          (-> uism-env
                                            (uism/apply-action assoc-in target-path nil)))
                                        :line-item/item
                                        (let [item-price  (get-in state-map (conj new-value :item/price))
                                              target-path (conj form-ident :line-item/quoted-price)]
                                          (uism/apply-action uism-env assoc-in target-path item-price))
                                        uism-env))}
   fo/field-styles  {:line-item/item     :pick-one
                     :line-item/category :pick-one}
   fo/field-options {:line-item/category {::picker-options/query-key       :category/all-categories
                                          ::picker-options/query-component category/Category
                                          ::picker-options/options-xform   (fn [_ options]
                                                                             (mapv
                                                                               (fn [{:category/keys [id label]}]
                                                                                 {:text (str label) :value [:category/id id]})
                                                                               (sort-by :category/label options)))
                                          ::picker-options/cache-time-ms   10000}
                     :line-item/item     {::picker-options/query-key        :item/all-items
                                          ::picker-options/cache-key        (fn [_ {:line-item/keys [id] :as props}]
                                                                              (keyword "item-list" (or
                                                                                                     (some-> props :line-item/category :category/id str)
                                                                                                     "all")))
                                          ::picker-options/query-component  item-forms/ItemForm
                                          ::picker-options/options-xform    (fn [_ options]
                                                                              (mapv
                                                                                (fn [{:item/keys [id name price]}]
                                                                                  {:text (str name " - " (math/numeric->currency-str price)) :value [:item/id id]})
                                                                                (sort-by :item/name options)))
                                          ::picker-options/query-parameters (fn [app form-class props]
                                                                              (let [category (get props :line-item/category)]
                                                                                category))
                                          ::picker-options/cache-time-ms    60000}}})
