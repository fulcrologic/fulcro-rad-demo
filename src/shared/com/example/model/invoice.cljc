(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.report-options :as ro]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.type-support.decimal :as math]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]))

(defattr id :invoice/id :uuid
  {ao/identity? true
   ;:com.fulcrologic.rad.database-adapters.datomic/native-id? true
   ao/schema    :production})

(defattr date :invoice/date :instant
  {::form/field-style           :date-at-noon
   ::datetime/default-time-zone "America/Los_Angeles"
   ao/identities                #{:invoice/id}
   ao/schema                    :production})

(defattr line-items :invoice/line-items :ref
  {ao/target                                                       :line-item/id
   :com.fulcrologic.rad.database-adapters.sql/delete-referent?     true
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}
   ao/cardinality                                                  :many
   ao/identities                                                   #{:invoice/id}
   ao/schema                                                       :production})

(defattr total :invoice/total :decimal
  {ao/identities      #{:invoice/id}
   ao/schema          :production
   ro/field-formatter (fn [report v] (math/numeric->currency-str v))
   ao/read-only?      true})

(defattr customer :invoice/customer :ref
  {ao/cardinality :one
   ao/target      :account/id
   ao/required?   true
   ao/identities  #{:invoice/id}
   ao/schema      :production})

;; Fold account details into the invoice details, if desired
#?(:clj
   (pc/defresolver customer-id [env {:invoice/keys [id]}]
     {::pc/input  #{:invoice/id}
      ::pc/output [:account/id]}
     {:account/id (queries/get-invoice-customer-id env id)}))

(defattr all-invoices :invoice/all-invoices :ref
  {ao/target     :invoice/id
   ao/pc-output  [{:invoice/all-invoices [:invoice/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:invoice/all-invoices (queries/get-all-invoices env query-params)}))})

(def attributes [id date line-items customer all-invoices total])
#?(:clj
   (def resolvers [customer-id]))
