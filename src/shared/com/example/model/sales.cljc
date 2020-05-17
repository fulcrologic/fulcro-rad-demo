(ns com.example.model.sales
  "Currently a mock report that generates a large number of rows so we can play with client-side report
 processing of a list of rows."
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.ids :as ids]))

(defattr date :sales/date :instant
  {ro/column-heading "Date"
   ao/style          :date})

(defattr revenue :sales/revenue :decimal
  {ro/column-heading "Revenue"
   ao/style          :USD})

(defattr cost :sales/cost :decimal
  {ro/column-heading "Costs"
   ao/style          :USD})

(defattr row-index :sales/row-index :uuid
  {})

(def attributes [date revenue cost row-index])

(def various-dates [#inst "2020-01-02T12:00Z"
                    #inst "2020-01-03T14:00Z"
                    #inst "2020-01-04T11:00Z"
                    #inst "2020-01-05T10:00Z"
                    #inst "2020-01-06T17:00Z"
                    #inst "2020-01-07T19:00Z"
                    #inst "2020-01-08T21:00Z"
                    #inst "2020-01-09T12:00Z"])

#?(:clj
   (defresolver sales-report-resolver [env {:invoice/keys [id]}]
     {::pc/output [{:sales-report/rows [:sales/date
                                        :sales/revenue
                                        :sales/cost]}]}
     {:sales-report/rows
      (vec
        (repeatedly 1000 (fn []
                           (let [rev (math/numeric (+ 54 (rand-int 1000)))]
                             {:sales/row-index (ids/new-uuid)
                              :sales/date      (rand-nth various-dates)
                              :sales/revenue   rev
                              :sales/cost      (math/round
                                                 (math/* rev (math/div 1 (+ 2 (rand-int 4))))
                                                 2)}))))}))

#?(:clj
   (def resolvers [sales-report-resolver]))
