(ns com.example.ui.invoice-forms
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.rad.picker-options :as picker-options]
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.example.model :as model]
    [com.example.model.account :as account]
    [com.example.model.invoice :as invoice]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.example.ui.line-item-forms :refer [LineItemForm]]
    [com.example.ui.account-forms :refer [AccountForm]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.routing :as rroute]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.report :as report]))

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
  {::form/id             invoice/id
   ;; So, a special (attr/derived-value key type style) would be useful for form logic display
   ;::form/read-only?     true
   ::form/attributes     [invoice/customer invoice/date invoice/line-items invoice/total]
   ::form/default-values {:invoice/date (datetime/now)}
   ::form/validator      invoice-validator
   ::form/layout         [[:invoice/customer :invoice/date]
                          [:invoice/line-items]
                          [:invoice/total]]
   ::form/field-styles   {:invoice/customer :pick-one}
   ::form/field-options  {:invoice/customer {::picker-options/query-key       :account/all-accounts
                                             ::picker-options/query-component AccountQuery
                                             ::picker-options/options-xform   (fn [_ options] (mapv
                                                                                                (fn [{:account/keys [id name email]}]
                                                                                                  {:text (str name ", " email) :value [:account/id id]})
                                                                                                (sort-by :account/name options)))
                                             ::picker-options/cache-time-ms   30000}}
   ::form/subforms       {:invoice/line-items {::form/ui            LineItemForm
                                               ::form/can-delete?   (fn [parent item] true)
                                               ::form/can-add?      (fn [parent] true)
                                               ::form/add-row-title "Add Item"
                                               ;; Use computed props to inform subform of its role.
                                               ::form/subform-style :inline}}
   ::form/triggers       {:derive-fields (fn [new-form-tree] (sum-subtotals* new-form-tree))}

   ::form/cancel-route   ["landing-page"]
   ::form/route-prefix   "invoice"
   ::form/title          (fn [{:invoice/keys [id]}]
                           (if (tempid/tempid? id)
                             (str "New Invoice")
                             (str "Invoice " id)))})

(report/defsc-report AccountInvoices [this props]
  {::report/title            "Customer Invoices"
   ::report/source-attribute :account/invoices
   ::report/row-pk           invoice/id
   ::report/columns          [invoice/id invoice/date invoice/total]
   ::report/column-headings  {:invoice/id "Invoice Number"}
   ::report/parameters       {:account/id {:type  :uuid
                                           :label "Account"}}
   ::report/run-on-mount?    true
   ::report/route            "account-invoices"})

(report/defsc-report InvoiceList [this props]
  {::report/title               "All Invoices"
   ::report/source-attribute    :invoice/all-invoices
   ::report/row-pk              invoice/id
   ::report/columns             [invoice/id invoice/date account/name invoice/total]

   ::report/row-query-inclusion [:account/id]

   ::report/column-headings     {:invoice/id   "Invoice Number"
                                 :account/name "Customer Name"}
   ::report/actions             [{:label  "New Invoice"
                                  :action (fn [this] (form/create! this InvoiceForm))}
                                 {:label  "New Account"
                                  :action (fn [this] (form/create! this AccountForm))}]
   ::report/row-actions         [{:label  "Account Invoices"
                                  :action (fn [this {:account/keys [id] :as row}]
                                            (rroute/route-to! this AccountInvoices {:account/id id}))}
                                 {:label  "Delete"
                                  :action (fn [this {:invoice/keys [id] :as row}] (form/delete! this :invoice/id id))}]

   ;; TASK: How to indicate form should be read-only when viewed through a link. Could just use ::report/link lambda,
   ;; and reserve this specifically for edit links
   ::report/form-links          {:invoice/total InvoiceForm
                                 :account/name  AccountForm}

   ::report/link                {:invoice/date (fn [report-instance {:invoice/keys [date] :as row-props}]
                                                 (log/spy :info row-props)
                                                 ;; TASK: Change filter to just this date
                                                 )}
   ::report/run-on-mount?       true
   ::report/route               "invoices"})

(comment
  (comp/get-query InvoiceList-Row))
