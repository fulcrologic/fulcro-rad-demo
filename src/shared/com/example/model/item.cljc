(ns com.example.model.item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.authorization :as auth]
    [com.wsscode.pathom.connect :as pc]
    #?(:clj [com.example.components.database-queries :as queries])))

(defattr id :item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr item-name :item/name :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-description :item/description :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-price :item/price :decimal
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr item-in-stock :item/in-stock :int
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:item/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr all-items :item/all-items :ref
  {::attr/target    :item/id
   ::auth/authority :local
   ::pc/output      [{:item/all-items [:item/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:item/all-items (queries/get-all-items env query-params)}))})

(def attributes [id item-name item-description item-price item-in-stock all-items])

