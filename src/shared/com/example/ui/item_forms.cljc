(ns com.example.ui.item-forms
  (:require
    [com.example.model.item :as item]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [com.fulcrologic.rad.form :as form]))

(defsc CategoryQuery [_ _]
  {:query [:category/id :category/label]
   :ident :category/id})

(form/defsc-form ItemForm [this props]
  {::form/id            item/id
   ::form/attributes    [item/item-name
                         item/category
                         item/description
                         item/in-stock
                         item/price]
   ::form/field-styles  {:item/category :pick-one}
   ::form/field-options {:item/category {::picker-options/query-key       :category/all-categories
                                         ::picker-options/query-component CategoryQuery
                                         ::picker-options/options-xform   (fn [_ options] (mapv
                                                                                            (fn [{:category/keys [id label]}]
                                                                                              {:text (str label) :value [:category/id id]})
                                                                                            (sort-by :category/label options)))
                                         ::picker-options/cache-time-ms   30000}}
   ::form/cancel-route  ["landing-page"]
   ::form/route-prefix  "item"
   ::form/title         "Edit Item"})
