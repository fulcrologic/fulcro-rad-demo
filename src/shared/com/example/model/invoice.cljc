(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    #?(:clj [com.example.components.database-queries :as queries])))

(defattr id :invoice/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr date :invoice/date :instant
  {::form/field-style                                        :date-at-noon
   ::datetime/default-time-zone                              "America/Los_Angeles"
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr line-items :invoice/line-items :ref
  {::attr/target                                             :line-item/id
   ::attr/cardinality                                        :many
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr customer :invoice/customer :ref
  {::attr/cardinality                                        :one
   ::attr/target                                             :account/id
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr all-invoices :invoice/all-invoices :ref
  {::attr/target    :invoice/id
   ::auth/authority :local
   ::pc/output      [{:invoice/all-invoices [:invoice/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:invoice/all-invoices (queries/get-all-invoices env query-params)}))})

(def attributes [id date line-items customer all-invoices])

