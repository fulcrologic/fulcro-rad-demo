(ns com.example.ui.sales-report
  (:require
    [com.example.model.sales :as sales]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.report :as report]))

(report/defsc-report SalesReport [this props]
  {ro/title               "Sales Report"
   ro/source-attribute    :sales-report/rows
   ro/row-pk              sales/row-index
   ro/columns             [sales/date sales/revenue sales/cost]

   ;; denormalized reports are much more performant when there are a large number of rows, but will not show changes that
   ;; are made via forms (can be out of date relative to other on-screen items).
   ro/denormalize?        true

   ;; Client filter/sort is only useful if you are not doing that already via report parameters.

   ;; If defined: This (client-side) filter will be applied to rows on load/refresh, or on change to current filter parameters.
   ro/row-visible?        (fn [filter-parameters row] (let [{::keys [revenue-filter]} filter-parameters
                                                            sales (get row :sales/revenue)]
                                                        (case revenue-filter
                                                          :good-sales (math/> sales 1000)
                                                          :bad-sales (math/<= sales 100)
                                                          true)))

   ro/controls            {::refresh        {:type   :button
                                             :label  "Refresh"
                                             :action (fn [this] (report/reload! this))}
                           ::revenue-filter {:type          :picker
                                             :default-value :all-sales
                                             :options       [{:text "All" :value :all-sales}
                                                             {:text "Bad Sales" :value :bad-sales}
                                                             {:text "Good Sales" :value :good-sales}]
                                             :onChange      (fn [this _] (report/filter-rows! this))
                                             :label         "Revenue Filter"}}

   ro/control-layout      {:action-buttons [::refresh]
                           :inputs         [[::revenue-filter]]}


   ;; If defined: sort is applied to rows after filtering (client-side)
   ro/initial-sort-params {:sort-by          :sales/date
                           :sortable-columns #{:sales/date :sales/revenue :sales/cost}
                           :ascending?       true}

   ro/compare-rows        (fn [{:keys [sort-by ascending?] :or {sort-by    :sales/date
                                                                ascending? true}} row-a row-b]
                            (let [a          (get row-a sort-by)
                                  b          (get row-b sort-by)
                                  fwd-result (case sort-by
                                               (:sales/revenue :sales/cost) (cond
                                                                              (math/< a b) -1
                                                                              (math/> a b) 1
                                                                              (math/= a b) 0)
                                               (compare a b))]
                              (cond-> fwd-result
                                (not ascending?) (-))))

   ro/paginate?           true
   ro/page-size           10

   ro/run-on-mount?       true
   ro/route               "sales-report"})
