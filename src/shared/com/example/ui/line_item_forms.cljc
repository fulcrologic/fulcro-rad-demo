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
    [com.fulcrologic.fulcro.application :as app]))

(form/defsc-form LineItemForm [this props]
  {::form/id            line-item/id
   ::form/attributes    [line-item/item line-item/quantity line-item/quoted-price line-item/subtotal]
   ::form/validator     model/all-attribute-validator
   ::form/cancel-route  ["landing-page"]
   ::form/route-prefix  "line-item"
   ::form/title         "Line Items"
   ::form/layout        [[:line-item/item :line-item/quantity :line-item/quoted-price :line-item/subtotal]]
   ::form/triggers      {:on-change (fn [{::form/keys [form-instance props] :as form-env} k old-value new-value]
                                      (case k
                                        :line-item/quoted-price
                                        (let [{:line-item/keys [quantity]} props
                                              subtotal (math/round (math/* quantity new-value) 2)]
                                          (comp/transact! form-instance
                                            `[(m/set-props ~{:line-item/subtotal subtotal})]))

                                        :line-item/quantity
                                        (let [{:line-item/keys [quoted-price]} props
                                              subtotal (math/round (math/* new-value quoted-price) 2)]
                                          (comp/transact! form-instance
                                            `[(m/set-props ~{:line-item/subtotal subtotal})]))

                                        :line-item/item
                                        (let [state-map  (app/current-state form-instance)
                                              {:line-item/keys [quantity]} props
                                              item-price (get-in state-map (conj new-value :item/price))
                                              subtotal   (math/round (math/* quantity item-price) 2)]
                                          (comp/transact! form-instance
                                            `[(m/set-props ~{:line-item/quoted-price item-price
                                                             :line-item/subtotal     subtotal})]))
                                        nil))}
   ::form/field-styles  {:line-item/item :pick-one}
   ::form/field-options {:line-item/item {::picker-options/query-key       :item/all-items
                                          ::picker-options/query-component item-forms/ItemForm
                                          ::picker-options/options-xform   (fn [_ options]
                                                                             (mapv
                                                                               (fn [{:item/keys [id name price]}]
                                                                                 {:text (str name " - " (math/numeric->currency-str price)) :value [:item/id id]})
                                                                               (sort-by :item/name options)))
                                          ::picker-options/cache-time-ms   100}}})
