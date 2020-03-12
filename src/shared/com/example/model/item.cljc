(ns com.example.model.item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.wsscode.pathom.connect :as pc]
    #?(:clj [com.example.components.database-queries :as queries])))

(defattr id :item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr category :item/category :ref
  {::attr/target                                             :category/id
   ::attr/cardinality                                        :one
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-name :item/name :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr description :item/description :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr price :item/price :decimal
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr in-stock :item/in-stock :int
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr all-items :item/all-items :ref
  {::attr/target    :item/id
   ::auth/authority :local
   ::pc/output      [{:item/all-items [:item/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:item/all-items (queries/get-all-items env query-params)}))})

(def attributes [id item-name category description price in-stock all-items])

