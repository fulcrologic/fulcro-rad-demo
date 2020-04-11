(ns com.example.model.item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.wsscode.pathom.connect :as pc]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]))

(defattr id :item/id :uuid
  {::attr/identity? true
   ::attr/schema    :production
   ::auth/authority :local})

(defattr category :item/category :ref
  {::attr/target      :category/id
   ::attr/cardinality :one
   ::attr/identities  #{:item/id}
   ::attr/schema      :production})

(defattr item-name :item/name :string
  {::attr/identities #{:item/id}
   ::attr/schema     :production})

(defattr description :item/description :string
  {::attr/identities #{:item/id}
   ::attr/schema     :production})

(defattr price :item/price :decimal
  {::attr/identities #{:item/id}
   ::attr/schema     :production})

(defattr in-stock :item/in-stock :int
  {::attr/identities #{:item/id}
   ::attr/schema     :production})

(defattr all-items :item/all-items :ref
  {::attr/target    :item/id
   ::auth/authority :local
   ::pc/output      [{:item/all-items [:item/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:item/all-items (queries/get-all-items env (log/spy :info query-params))}))})

#?(:clj
   (pc/defresolver item-category-resolver [{:keys [parser] :as env} {:item/keys [id]}]
     {::pc/input  #{:item/id}
      ::pc/output [:category/id :category/label]}
     (let [result (parser env [{[:item/id id] [{:item/category [:category/id :category/label]}]}])]
       (get-in (log/spy :info result) [[:item/id id] :item/category]))))

(def attributes [id item-name category description price in-stock all-items])

#?(:clj
   (def resolvers [item-category-resolver]))

