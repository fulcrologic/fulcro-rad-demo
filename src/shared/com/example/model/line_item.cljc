(ns com.example.model.line-item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.authorization :as auth]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]
    #?(:clj [com.example.components.database-queries :as queries])))

(defattr id :line-item/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr category :line-item/category :ref
  {::attr/target      :category/id
   ::pc/input         #{:line-item/id}
   ::pc/output        [{:line-item/category [:category/id]}]
   ::pc/resolve       (fn [env {:line-item/keys [id]}]
                        #?(:clj
                           (when-let [cid (queries/get-line-item-category env id)]
                             {:line-item/category {:category/id cid}})))
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

(def attributes [id category item quantity quoted-price subtotal])
