(ns com.example.ui.sales-report
  (:require
    [com.example.model.sales :as sales]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.report :as report]))

(report/defsc-report SalesReport [this props]
  {::report/title                 "Sales Report"
   ::report/source-attribute      :sales-report/rows
   ::report/row-pk                sales/row-index
   ::report/columns               [sales/date sales/revenue sales/cost]

   ;; denormalized reports are much more performant when there are a large number of rows, but will not show changes that
   ;; are made via forms (can be out of date relative to other on-screen items).
   ::report/denormalize?          true

   ;; Client filter/sort is only useful if you are not doing that already via report parameters.

   ;; If defined: This (client-side) filter will be applied to rows on load/refresh, or on change to current filter parameters.
   ::report/initial-filter-params {:mode :good-sales}       ; made-up for this report
   ::report/row-visible?          (fn [filter-parameters row] (let [{:keys [mode]} filter-parameters
                                                                    sales (get row :sales/revenue)]
                                                                (case mode
                                                                  :good-sales (math/> sales 1000)
                                                                  :bad-sales (math/<= sales 100)
                                                                  true)))

   ::report/actions               [{:label  "All Sales"
                                    :action (fn [this {:account/keys [id] :as row}]
                                              (report/filter-rows! {:report-instance this} {:mode :all-sales}))}
                                   {:label  "Good Sales"
                                    :action (fn [this {:account/keys [id] :as row}]
                                              (report/filter-rows! {:report-instance this} {:mode :good-sales}))}
                                   {:label  "Bad Sales"
                                    :action (fn [this {:account/keys [id] :as row}]
                                              (report/filter-rows! {:report-instance this} {:mode :bad-sales}))}]

   ;; If defined: sort is applied to rows after filtering (client-side) (NOT YET IMPLEMENTED
   ::report/initial-sort-params   {:sort-by  :sales/date
                                   :forward? true}

   ::report/compare-rows          (fn [{:keys [sort-by forward?] :or {sort-by  :sales/date
                                                                      forward? true}} row-a row-b]
                                    (let [a          (get row-a sort-by)
                                          b          (get row-b sort-by)
                                          fwd-result (case sort-by
                                                       (:sales/revenue :sales/cost) (cond
                                                                                      (math/< a b) -1
                                                                                      (math/> a b) 1
                                                                                      (math/= a b) 0)
                                                       (compare a b))]
                                      (cond-> fwd-result
                                        (not forward?) (-))))

   ;; Pagination can be implemented as server params, and the implementation depends on the report state machine and
   ;; the render plugin used for the report. The default is to paginate on the client.
   ::report/paginate?             true
   ::report/page-size             10

   ::report/run-on-mount?         true
   ::report/route                 "sales-report"})
