(ns com.example.ui.dashboard
  (:require
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.control-options :as copt]
    [com.fulcrologic.rad.container-options :as co]
    [com.fulcrologic.rad.container :as container :refer [defsc-container]]
    [com.example.ui.sales-report :as sales]))

(defsc-container Dashboard [this props]
  {co/children         [sales/RealSalesReport]
   co/layout           [[(comp/class->registry-key sales/RealSalesReport)]]
   copt/controls       {::refresh {:type   :button
                                   :action (fn [container])}}
   copt/control-layout {:action-buttons []
                        :inputs         [[:start-date :end-date]]}})

(comment
  (container/shared-controls Dashboard)
  (comp/component-options Dashboard copt/controls))
