(ns com.example.model.entity
  (:refer-clojure :exclude [name])
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form-options :as fo]))

(defattr name :entity/name :string
  {ao/identities         #{:company/id}
   ao/required?          true
   ao/valid?             (fn [v] (and (string? v)
                                   (> (count v) 2)))
   fo/validation-message "Must be at least 2 long."
   ao/schema             :production})

(defattr email :entity/email :string
  {ao/identities #{:company/id :person/id}
   ao/required?  true
   ao/schema     :production})

(def attributes [name email])
