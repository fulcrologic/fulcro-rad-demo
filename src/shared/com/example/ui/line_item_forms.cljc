(ns com.example.ui.line-item-forms
  (:require
    [com.example.model :as model]
    [com.example.model.line-item :as line-item]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form LineItemForm [this props]
  {::form/id           line-item/id
   ::form/attributes   [line-item/item line-item/quantity ]
   ::form/validator    model/all-attribute-validator
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "line-item"
   ::form/title        "Line Items"
   ::form/layout       [[:line-item/item :line-item/quantity]]
   ::form/subforms     {:line-item/item {::form/ui       form/ToOneEntityPicker
                                         ::form/pick-one {:options/query-key :item/all-items
                                                          :options/subquery  [:item/id :item/name :item/price :item/n-in-stock]
                                                          :options/transform (fn [{:item/keys [id name price]}]
                                                                               {:text (str name " - " (math/numeric->currency-str price)) :value [:item/id id]})}
                                         ::form/label    "Item"}}})
