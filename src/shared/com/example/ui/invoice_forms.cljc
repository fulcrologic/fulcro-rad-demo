(ns com.example.ui.invoice-forms
  (:require
    #?(:cljs [cljs.loader :as loader])
    [com.example.client :as client]
    [com.example.model.account :as account]
    [com.example.model.invoice :as invoice]
    [com.example.ui :refer [MainRouter]]
    [com.example.ui.account-forms :refer [AccountForm]]
    [com.example.ui.line-item-forms]                        ; Required, since we're getting to it in a completely dynamic way
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form-render-options :as fro]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.routing :as rroute]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [taoensso.timbre :as log]))

(defn sum-subtotals* [{:invoice/keys [line-items] :as invoice}]
  (assoc invoice :invoice/total
                 (reduce
                   (fn [t {:line-item/keys [subtotal]}]
                     (math/+ t subtotal))
                   (math/zero)
                   line-items)))

(form/defsc-form InvoiceForm [this props]
  {fo/id             invoice/id
   fro/style         :rad/multirender
   ;; So, a special (attr/derived-value key type style) would be useful for form logic display
   ;::form/read-only?     true
   fo/attributes     [invoice/customer invoice/date invoice/line-items invoice/total]
   fo/default-values {:invoice/date (datetime/now)}
   fo/layout         [[:invoice/customer :invoice/date]     ; See invoice/customer for field-style and field-options.
                      [:invoice/line-items]                 ; See invoice/line-items for field-style and field-options.
                      [:invoice/total]]
   fo/triggers       {:derive-fields (fn [new-form-tree] (sum-subtotals* new-form-tree))}
   fo/route-prefix   "invoice"
   fo/title          (fn [_ {:invoice/keys [id]}]
                       (if (tempid/tempid? id)
                         (str "New Invoice")
                         (str "Invoice " id)))})

;; Sample of report that can be generated on-the-fly at runtime. Looks just like normal report options, but could
;; be called in code using data derived from persistent storage, etc.
(def AccountInvoices
  (report/report ::AccountInvoices
    {ro/title            "Customer Invoices"
     ro/source-attribute :account/invoices
     ro/row-pk           invoice/id
     ro/columns          [invoice/id invoice/date invoice/total]
     ro/column-headings  {:invoice/id "Invoice Number"}

     ro/form-links       {:invoice/id InvoiceForm}
     ro/controls         {:account/id {:type   :uuid
                                       :local? true
                                       :label  "Account"}}
     ;; No control layout...we don't actually let the user control it

     ro/run-on-mount?    true
     ro/route            "account-invoices"}))

(report/defsc-report InvoiceList [this props]
  {ro/title               "All Invoices"
   ro/source-attribute    :invoice/all-invoices
   ro/row-pk              invoice/id
   ro/columns             [invoice/id invoice/date account/name invoice/total]

   ro/row-query-inclusion [:account/id]

   ro/column-headings     {:invoice/id   "Invoice Number"
                           :account/name "Customer Name"}

   ro/controls            {::new-invoice {:label  "New Invoice"
                                          :type   :button
                                          :action (fn [this] (form/create! this InvoiceForm))}
                           ::new-account {:label  "New Account"
                                          :type   :button
                                          :action (fn [this] (form/create! this AccountForm))}}

   ro/control-layout      {:action-buttons [::new-invoice ::new-account]}

   ro/row-actions         [{:label  "Account Invoices"
                            :action (fn [this {:account/keys [id] :as row}]
                                      (rroute/route-to! this AccountInvoices {:account/id id}))}
                           {:label  "Delete"
                            :action (fn [this {:invoice/keys [id] :as row}] (form/delete! this :invoice/id id))}]

   ro/form-links          {:invoice/total InvoiceForm
                           :account/name  AccountForm}

   ro/run-on-mount?       true
   ro/route               "invoices"})

(defn init-module []
  #?(:cljs
     (do
       (dr/add-route-target!! client/app {:router MainRouter :target InvoiceForm})
       (dr/add-route-target!! client/app {:router MainRouter :target AccountInvoices})
       (dr/add-route-target!! client/app {:router MainRouter :target InvoiceList})
       (loader/set-loaded! :invoicing))))
