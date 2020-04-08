(ns com.example.reports.sales
  "Currently a mock report that generates a large number of rows so we can play with client-side report
   processing of a list of rows."
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [com.fulcrologic.rad.type-support.decimal :as math]))

(def various-dates [#inst "2020-01-02T12:00Z"
                    #inst "2020-01-03T14:00Z"
                    #inst "2020-01-04T11:00Z"
                    #inst "2020-01-05T10:00Z"
                    #inst "2020-01-06T17:00Z"
                    #inst "2020-01-07T19:00Z"
                    #inst "2020-01-08T21:00Z"
                    #inst "2020-01-09T12:00Z"])

(defresolver sales-report-resolver [env {:invoice/keys [id]}]
  {::pc/output [{:sales-report/rows [:sales/date
                                     :sales/revenue
                                     :sales/cost]}]}
  {:sales-report/rows
   (vec
     (repeatedly 1000 (fn []
                        (let [rev (math/numeric (+ 54 (rand-int 1000)))]
                          {:sales/date    (rand-nth various-dates)
                           :sales/revenue rev
                           :sales/cost    (math/round
                                            (math/* rev (math/div 1 (+ 2 (rand-int 4))))
                                            2)}))))})

(def resolvers [sales-report-resolver])
