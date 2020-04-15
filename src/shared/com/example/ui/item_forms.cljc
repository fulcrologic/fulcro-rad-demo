(ns com.example.ui.item-forms
  (:require
    [com.example.model.item :as item]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
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
   fo/cancel-route  ["landing-page"]
   fo/route-prefix  "item"
   fo/title         "Edit Item"})

(report/defsc-report InventoryReport [this props]
  {ro/title               "Inventory Report"
   ro/source-attribute    :item/all-items
   ro/row-pk              item/id
   ro/columns             [item/item-name category/label item/price item/in-stock]

   ;; denormalized reports are much more performant when there are a large number of rows, but will not show changes that
   ;; are made via forms (can be out of date relative to other on-screen items).
   ro/denormalize?        true
   ro/row-visible?        (fn [filter-parameters row] (let [{::keys [category]} filter-parameters
                                                            row-category (get row :category/label)]
                                                        (or (nil? category) (= category row-category))))

   ro/controls            {::category {:type     :button
                                       :action   (fn [this _]
                                                   (report/set-parameter! this ::category nil)
                                                   (report/filter-rows! this))
                                       :visible? (fn [this]
                                                   (some-> this comp/props :ui/parameters ::category))
                                       :label    "Clear Filter"}}

   ro/control-layout      {:action-buttons [::category]}


   ;; If defined: sort is applied to rows after filtering (client-side)
   ro/initial-sort-params {:sort-by          :item/name
                           :sortable-columns #{:item/name :category/label}
                           :ascending?         true}

   ro/compare-rows        (fn [{:keys [sort-by ascending?] :or {sort-by  :sales/date
                                                              ascending? true}} row-a row-b]
                            (let [a          (get row-a sort-by)
                                  b          (get row-b sort-by)
                                  fwd-result (compare a b)]
                              (cond-> fwd-result
                                (not ascending?) (-))))

   ro/form-links          {item/item-name ItemForm}

   ro/links               {:category/label (fn [this {:category/keys [label]}]
                                             (report/set-parameter! this ::category label)
                                             (report/filter-rows! this))}

   ro/paginate?           true
   ro/page-size           10

   ro/run-on-mount?       true
   ro/route               "item-inventory-report"})
