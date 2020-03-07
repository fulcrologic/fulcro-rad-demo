(ns com.example.ui.invoice-forms
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [edn-query-language.core :as eql]
    [com.example.model :as model]
    [com.example.model.invoice :as invoice]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.example.ui.line-item-forms :refer [LineItemForm]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.type-support.date-time :as datetime]))

(def invoice-validator (fs/make-validator (fn [form field]
                                            (let [value (get form field)]
                                              (case field
                                                :invoice/customer (eql/ident? value)
                                                :invoice/line-items (> (count value) 0)
                                                (= :valid (model/all-attribute-validator form field)))))))

(defsc AccountQuery [_ _]
  {:query [:account/id :account/name :account/email]
   :ident :account/id})

(form/defsc-form InvoiceForm [this props]
  {::form/id           invoice/id
   ;; So, a special (attr/derived-value key type style) would be useful for form logic display
   ::form/attributes   [invoice/customer invoice/date invoice/line-items]
   ::form/default      {:invoice/date (datetime/now)}
   ::form/validator    invoice-validator
   ::form/layout       [[:invoice/customer :invoice/date]
                        [:invoice/line-items]]
   ::form/subforms     {:invoice/customer   {::form/ui            form/ToOneEntityPicker
                                             ::form/pick-one      {:options/query-key :account/all-accounts
                                                                   :options/subquery  AccountQuery
                                                                   :options/transform (fn [{:account/keys [id name email]}]
                                                                                        {:text (str name ", " email) :value [:account/id id]})}
                                             ::form/label         "Customer"
                                             ;; Use computed props to inform subform of its role.
                                             ::form/subform-style :inline}
                        :invoice/line-items {::form/ui            LineItemForm
                                             ::form/can-delete?   (fn [parent item] true)
                                             ::form/can-add?      (fn [parent] true)
                                             ::form/add-row-title "Add Item"
                                             ;; Use computed props to inform subform of its role.
                                             ::form/subform-style :inline}}
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "invoice"
   ::form/title        "Edit Invoice"})

