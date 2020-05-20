(ns com.example.ui.sales-report
  (:require
    [com.example.model.sales :as sales]
    [com.example.model.invoice :as invoice]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.report :as report]
    [taoensso.timbre :as log]))

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
                                             :action (fn [this] (control/run! this))}
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

   ro/paginate?           true
   ro/page-size           10

   ro/run-on-mount?       true
   ro/route               "sales-report"})

;; This report uses Pathom resolvers that return a grouped result (for ease of implementation on the back-end).
;; Using such a result requires we turn off normalization and do a raw result transform to make the rows appear
;; as the report logic expects.
(report/defsc-report RealSalesReport [this props]
  {ro/title               "Sales Report"
   ro/source-attribute    :invoice-statistics
   ro/row-pk              invoice/date-groups
   ro/columns             [invoice/date-groups invoice/gross-sales invoice/items-sold]

   ;; Make sure Fulcro leaves it denormalized, otherwise the raw result will get mangled and our rotate of the result won't work.
   ro/denormalize?        true
   ro/raw-result-xform    report/rotate-result

   ro/column-headings     {:invoice-statistics/date-groups (fn [report-instance]
                                                             (let [grouping (get-in (comp/props report-instance) [:ui/parameters :group-by])]
                                                               (case grouping
                                                                 :month "Month Starting"
                                                                 :day "Date"
                                                                 :year "Year Starting"
                                                                 "All")))
                           :invoice-statistics/gross-sales "Gross Sales"
                           :invoice-statistics/items-sold  "Total Items Sold"}

   ro/controls            {::refresh   {:type   :button
                                        :label  "Refresh"
                                        :action (fn [this] (control/run! this))}
                           ::rotate?   {:type          :boolean
                                        :local?        true
                                        :label         "Pivot?"
                                        :default-value false}
                           :start-date {:type          :instant
                                        :style         :starting-date
                                        :default-value (fn [app] (dt/now))
                                        :label         "From"}
                           :end-date   {:type          :instant
                                        :style         :ending-date
                                        :default-value (fn [app] (dt/now))
                                        :label         "To"}
                           :group-by   {:type          :picker
                                        :local?        true
                                        :default-value :month
                                        :options       [{:text "Month" :value :month}
                                                        {:text "Day" :value :day}
                                                        {:text "Year" :value :year}
                                                        {:text "All" :value :summary}]
                                        :action        (fn [this] (control/run! this))
                                        :label         "Group By"}}

   ro/control-layout      {:action-buttons [::refresh]
                           :inputs         [[:start-date :end-date]
                                            [:group-by ::rotate?]]}

   ro/initial-sort-params {:sort-by          :invoice-statistics/date-groups
                           :sortable-columns #{:invoice-statistics/date-groups :invoice-statistics/gross-sales :invoice-statistics/items-sold}
                           :ascending?       true}

   ro/run-on-mount?       true
   ro/rotate?             (fn [rpt] (boolean (control/current-value rpt ::rotate?)))
   ro/route               "invoice-report"})
