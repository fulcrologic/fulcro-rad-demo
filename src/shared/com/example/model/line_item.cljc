(ns com.example.model.line-item
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    #?(:clj [com.example.components.database-queries :as queries])))

(defattr id :line-item/id :uuid
  {ao/identity? true
   ao/schema    :production})

(defattr category :line-item/category :ref
  {ao/target      :category/id
   ao/pc-input    #{:line-item/id}
   ao/pc-output   [{:line-item/category [:category/id]}]
   ao/pc-resolve  (fn [env {:line-item/keys [id]}]
                    #?(:clj
                       (when-let [cid (queries/get-line-item-category env id)]
                         {:line-item/category {:category/id cid}})))
   ao/cardinality :one})

(defattr item :line-item/item :ref
  {ao/target      :item/id
   ao/required?   true
   ao/cardinality :one
   ao/identities  #{:line-item/id}
   ao/schema      :production})

(defattr quantity :line-item/quantity :int
  {ao/required?  true
   ao/identities #{:line-item/id}
   ao/schema     :production})

(defattr quoted-price :line-item/quoted-price :decimal
  {ao/identities #{:line-item/id}
   ao/schema     :production})

(defattr subtotal :line-item/subtotal :decimal
  {ao/read-only? true
   ao/identities #{:line-item/id}
   ao/schema     :production})

(def attributes [id category item quantity quoted-price subtotal])
