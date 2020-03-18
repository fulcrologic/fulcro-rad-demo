(ns com.example.model.line-item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.authorization :as auth]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.type-support.decimal :as math]))

(defattr id :line-item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr category :line-item/category :ref
  {::attr/target      :category/id
   ::attr/cardinality :one})

(defattr item :line-item/item :ref
  {::attr/target                                             :item/id
   ::attr/required?                                          true
   ::attr/cardinality                                        :one
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr quantity :line-item/quantity :int
  {::attr/required?                                          true
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr quoted-price :line-item/quoted-price :decimal
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr subtotal :line-item/subtotal :decimal
  {::attr/read-only?                                         true
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:line-item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production}
  #_{::attr/computed-value (fn [{::form/keys [props] :as form-env} attr]
                             (let [{:line-item/keys [quantity quoted-price]} props]
                               (math/round (math/* quantity quoted-price) 2)))})

(def attributes [id item quantity quoted-price subtotal])
