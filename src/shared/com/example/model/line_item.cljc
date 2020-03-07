(ns com.example.model.line-item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :line-item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr item :line-item/item :ref
  {::attr/target                                             :item/id
   ::attr/cardinality                                        :one
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr quantity :line-item/quantity :int
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

;; TODO: initial value should be pushed on from item selection
(defattr quoted-price :line-item/quoted-price :decimal
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

;; TODO: Add attribute that is a derived thing (subtotal)...in THIS case, I want a UI-derivation
(defattr subtotal :line-item/subtotal :decimal {})

(def attributes [id item quantity quoted-price subtotal])
