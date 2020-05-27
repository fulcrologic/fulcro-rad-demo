(ns com.example.ui.dashboard
  (:require
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.control-options :as copt]
    [com.fulcrologic.rad.container-options :as co]
    [com.fulcrologic.rad.options-util :refer [child-classes]]
    [com.fulcrologic.rad.container :as container :refer [defsc-container]]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [com.example.ui.account-forms :as account-forms]
    [com.example.ui.item-forms :as item-forms]
    [com.example.ui.sales-report :as sales]))

(defsc-container Dashboard [this props]
  {co/children         {:sales     sales/RealSalesReport
                        :customers account-forms/AccountList
                        :inventory item-forms/InventoryReport}
   co/layout           [[{:id :sales :width 16}]
                        [{:id :inventory :width 8} {:id :customers :width 8}]]
   co/route            "dashboard"
   co/title            "Dashboard"
   copt/controls       {::refresh {:type   :button
                                   :label  "Refresh"
                                   :action (fn [container] (control/run! container))}}
   copt/control-layout {:action-buttons [::refresh]
                        ;; these inputs are pulled up from nested reports (any control that is not marked local will be)
                        :inputs         [[:start-date :end-date]]}})
