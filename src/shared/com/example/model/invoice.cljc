(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.type-support.decimal :as math]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]))

(defattr id :invoice/id :uuid
  {::attr/identity? true
   ;:com.fulcrologic.rad.database-adapters.datomic/native-id? true
   ::attr/schema    :production
   ::auth/authority :local})

(defattr date :invoice/date :instant
  {::form/field-style           :date-at-noon
   ::datetime/default-time-zone "America/Los_Angeles"
   ::attr/identities            #{:invoice/id}
   ::attr/schema                :production})

(defattr line-items :invoice/line-items :ref
  {::attr/target                                                   :line-item/id
   :com.fulcrologic.rad.database-adapters.sql/delete-referent?     true
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}
   ::attr/cardinality                                              :many
   ::attr/identities                                               #{:invoice/id}
   ::attr/schema                                                   :production})

(defattr total :invoice/total :decimal
  {::attr/identities #{:invoice/id}
   ::attr/schema     :production
   ::attr/read-only? true}
  #_{::attr/computed-value (fn [{::form/keys [props]} attr]
                             (let [total (reduce
                                           (fn [t {:line-item/keys [quantity quoted-price]}]
                                             (math/+ t (math/* quantity quoted-price)))
                                           (math/zero)
                                           (:invoice/line-items props))]
                               total))})

(defattr customer :invoice/customer :ref
  {::attr/cardinality :one
   ::attr/target      :account/id
   ::attr/required?   true
   ::attr/identities  #{:invoice/id}
   ::attr/schema      :production})

;; Fold account details into the invoice details, if desired
#?(:clj
   (pc/defresolver customer-id [env {:invoice/keys [id]}]
     {::pc/input  #{:invoice/id}
      ::pc/output [:account/id]}
     {:account/id (queries/get-invoice-customer-id env id)}))

(defattr all-invoices :invoice/all-invoices :ref
  {::attr/target    :invoice/id
   ::auth/authority :local
   ::pc/output      [{:invoice/all-invoices [:invoice/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:invoice/all-invoices (queries/get-all-invoices env query-params)}))})

(def attributes [id date line-items customer all-invoices total])
#?(:clj
   (def resolvers [customer-id]))
