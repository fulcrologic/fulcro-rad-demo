(ns com.example.ui.invoice-forms
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.picker-options :as picker-options]
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
  {::form/id            invoice/id
   ;; So, a special (attr/derived-value key type style) would be useful for form logic display
   ::form/attributes    [invoice/customer invoice/date invoice/line-items]
   ::form/default       {:invoice/date (datetime/now)}
   ::form/validator     invoice-validator
   ::form/layout        [[:invoice/customer :invoice/date]
                         [:invoice/line-items]]
   ::form/field-styles  {:invoice/customer :pick-one}
   ::form/field-options {:invoice/customer {::picker-options/query-key       :account/all-accounts
                                            ::picker-options/query-component AccountQuery
                                            ::picker-options/options-xform   (fn [_ options] (mapv
                                                                                               (fn [{:account/keys [id name email]}]
                                                                                                 {:text (str name ", " email) :value [:account/id id]})
                                                                                               (sort-by :account/name options)))
                                            ::picker-options/cache-time-ms   30000}}
   ::form/subforms      {:invoice/line-items {::form/ui            LineItemForm
                                              ::form/can-delete?   (fn [parent item] true)
                                              ::form/can-add?      (fn [parent] true)
                                              ::form/add-row-title "Add Item"
                                              ;; Use computed props to inform subform of its role.
                                              ::form/subform-style :inline}}
   ::form/cancel-route  ["landing-page"]
   ::form/route-prefix  "invoice"
   ::form/title         "Edit Invoice"})

