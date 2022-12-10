(ns com.example.model.entity
  (:refer-clojure :exclude [name])
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]))

(defattr name :entity/name :string
  {ao/identities #{:company/id}
   ao/schema     :production})

(defattr email :entity/email :string
  {ao/identities #{:company/id :person/id}
   ao/schema     :production})

(def attributes [name email])
