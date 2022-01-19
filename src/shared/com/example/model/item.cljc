(ns com.example.model.item
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.eql :as p.eql]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]))

(defattr id :item/id :uuid
  {ao/identity? true
   ao/schema    :production})

(defattr category :item/category :ref
  {ao/target      :category/id
   ao/cardinality :one
   ao/identities  #{:item/id}
   ao/schema      :production})

(defattr item-name :item/name :string
  {ao/identities #{:item/id}
   ao/schema     :production})

(defattr description :item/description :string
  {ao/identities #{:item/id}
   ao/schema     :production})

(defattr price :item/price :decimal
  {ao/identities #{:item/id}
   ao/schema     :production})

(defattr in-stock :item/in-stock :int
  {ao/identities #{:item/id}
   ao/schema     :production})

(defattr min-level :item/min-level :int
  {ao/identities #{:item/id}
   ao/schema     :production})

(defattr location :item/location :string
  {ao/identities #{:item/id}
   ao/style :viewable-password
   ao/schema     :production})

(defattr all-items :item/all-items :ref
  {ao/target     :item/id
   ao/pc-output  [{:item/all-items [:item/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:item/all-items (queries/get-all-items env query-params)}))})

#?(:clj
   (pco/defresolver item-category-resolver [env {:item/keys [id]}]
     {::pco/input  [:item/id]
      ::pco/output [:category/id :category/label]}
     (let [result (p.eql/process env [{[:item/id id] [{:item/category [:category/id :category/label]}]}])]
       (get-in result [[:item/id id] :item/category]))))

(def attributes [id item-name category description price in-stock min-level location all-items])

#?(:clj
   (def resolvers [item-category-resolver]))

