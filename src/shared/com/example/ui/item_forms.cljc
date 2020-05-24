(ns com.example.ui.item-forms
  (:require
    [com.example.model.item :as item]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [taoensso.timbre :as log]
    [com.example.model.category :as category]))

(defsc CategoryQuery [_ _]
  {:query [:category/id :category/label]
   :ident :category/id})

(form/defsc-form ItemForm [this props]
  {fo/id            item/id
   fo/attributes    [item/item-name
                     item/category
                     item/description
                     item/in-stock
                     item/price]
   fo/field-styles  {:item/category :pick-one}
   fo/field-options {:item/category {::picker-options/query-key       :category/all-categories
                                     ::picker-options/query-component CategoryQuery
                                     ::picker-options/options-xform   (fn [_ options] (mapv
                                                                                        (fn [{:category/keys [id label]}]
                                                                                          {:text (str label) :value [:category/id id]})
                                                                                        (sort-by :category/label options)))
                                     ::picker-options/cache-time-ms   30000}}
   fo/route-prefix  "item"
   fo/title         "Edit Item"})

(report/defsc-report InventoryReport [this props]
  {ro/title               "Inventory Report"
   ro/source-attribute    :item/all-items
   ro/row-pk              item/id
   ro/columns             [item/item-name category/label item/price item/in-stock]

   ro/row-visible?        (fn [filter-parameters row] (let [{::keys [category]} filter-parameters
                                                            row-category (get row :category/label)]
                                                        (or (= "" category) (= category row-category))))

   ;; A sample server-query based picker that sets a local parameter that we use to filter rows.
   ro/controls            {::category {:type                          :picker
                                       :local?                        true
                                       :label                         "Category"
                                       :default-value                 ""
                                       :action                        (fn [this] (report/filter-rows! this))
                                       picker-options/cache-time-ms   30000
                                       picker-options/cache-key       :all-category-options
                                       picker-options/query-key       :category/all-categories
                                       picker-options/query-component category/Category
                                       picker-options/options-xform   (fn [_ categories]
                                                                        (into [{:text "All" :value ""}]
                                                                          (map
                                                                            (fn [{:category/keys [label]}]
                                                                              {:text label :value label}))
                                                                          categories))}}

   ;; If defined: sort is applied to rows after filtering (client-side)
   ro/initial-sort-params {:sort-by          :item/name
                           :sortable-columns #{:item/name :category/label}
                           :ascending?       true}

   ro/form-links          {item/item-name ItemForm}

   ro/links               {:category/label (fn [this {:category/keys [label]}]
                                             (control/set-parameter! this ::category label)
                                             (report/filter-rows! this))}

   ro/run-on-mount?       true
   ro/route               "item-inventory-report"})
