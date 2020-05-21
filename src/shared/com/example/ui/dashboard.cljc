(ns com.example.ui.dashboard
  (:require
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.control-options :as copt]
    [com.fulcrologic.rad.container-options :as co]
    [com.fulcrologic.rad.container :as container :refer [defsc-container]]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [com.example.ui.sales-report :as sales]))

(defsc-container Dashboard [this props]
  {co/children         {:a sales/RealSalesReport
                        :b sales/RealSalesReport}
   co/layout           [[{:id :a :width 6} {:id :b :width 10}]]
   co/route            "dashboard"
   co/title            "Dashboard"
   copt/controls       {::refresh {:type   :button
                                   :label  "Refresh"
                                   :action (fn [container] (control/run! container))}}
   copt/control-layout {:action-buttons [::refresh]
                        :inputs         [[:start-date :end-date]]}})


(comment
  (comp/get-initial-state Dashboard)
  (container/shared-controls Dashboard)
  (comp/component-options Dashboard copt/controls))
