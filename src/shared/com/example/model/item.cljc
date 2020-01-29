(ns com.example.model.item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr item-name :item/name :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-price :item/price :money
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-in-stock :item/in-stock :int
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(def attributes [id item-name item-price item-in-stock])
