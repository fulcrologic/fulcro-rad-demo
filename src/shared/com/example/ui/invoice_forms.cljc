(ns com.example.ui.invoice-forms
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.rad.picker-options :as po]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.example.model :as model]
    [com.example.model.account :as account]
    [com.example.model.invoice :as invoice]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.example.ui.line-item-forms :refer [LineItemForm]]
    [com.example.ui.account-forms :refer [BriefAccountForm AccountForm]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.routing :as rroute]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]))

(def invoice-validator (fs/make-validator (fn [form field]
                                            (let [value (get form field)]
                                              (case field
                                                :invoice/line-items (> (count value) 0)
                                                (= :valid (model/all-attribute-validator form field)))))))

(defsc AccountQuery [_ _]
  {:query [:account/id :account/name :account/email]
   :ident :account/id})

(defn sum-subtotals* [{:invoice/keys [line-items] :as invoice}]
  (assoc invoice :invoice/total
    (reduce
      (fn [t {:line-item/keys [subtotal]}]
        (math/+ t subtotal))
      (math/zero)
      line-items)))

(form/defsc-form InvoiceForm [this props]
  {fo/id             invoice/id
   ;; So, a special (attr/derived-value key type style) would be useful for form logic display
   ;::form/read-only?     true
   fo/attributes     [invoice/customer invoice/date invoice/line-items invoice/total]
   fo/default-values {:invoice/date (datetime/now)}
   fo/validator      invoice-validator
   fo/layout         [[:invoice/customer :invoice/date]
                      [:invoice/line-items]
                      [:invoice/total]]
   fo/field-styles   {:invoice/customer :pick-one}
   fo/field-options  {:invoice/customer {po/form            BriefAccountForm
                                         fo/title           (fn [i {:account/keys [id]}]
                                                              (if (tempid/tempid? id)
                                                                "New Account"
                                                                "Edit Account"))
                                         po/quick-create    (fn [v] {:account/id        (tempid/tempid)
                                                                     :account/email     (str/lower-case (str v "@example.com"))
                                                                     :time-zone/zone-id :time-zone.zone-id/America-Los_Angeles
                                                                     :account/active?   true
                                                                     :account/name      v})
                                         po/allow-create?   true
                                         po/allow-edit?     true
                                         po/query-key       :account/all-accounts
                                         po/query-component AccountQuery
                                         po/options-xform   (fn [_ options] (mapv
                                                                              (fn [{:account/keys [id name email]}]
                                                                                {:text (str name ", " email) :value [:account/id id]})
                                                                              (sort-by :account/name options)))
                                         po/cache-time-ms   30000}}
   fo/subforms       {:invoice/line-items {fo/ui          LineItemForm
                                           fo/can-delete? (fn [_ _] true)
                                           fo/can-add?    (fn [_ _] true)}}
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
