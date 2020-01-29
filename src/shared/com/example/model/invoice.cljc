(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :invoice/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

#_(defattr invoice-date :invoice/date :inst
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr invoice-items :invoice/items :ref
  {::attr/target                                                   :item/id
   ::attr/cardinality                                              :many
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/intended-targets #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema           :production})

(defattr customer :invoice/customer :ref
  {::attr/cardinality                                              :one
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/intended-targets #{:account/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema           :production})

(def attributes [id #_invoice-date invoice-items customer])
