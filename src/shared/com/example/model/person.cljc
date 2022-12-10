(ns com.example.model.person
  (:refer-clojure :exclude [name])
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]))

(defattr id :person/id :uuid
  {ao/identity? true
   ao/schema    :production})

; email is :entity/email

(defattr first-name :person/first-name :string
  {ao/identities #{:person/id}
   ao/schema     :production})

(defattr last-name :person/last-name :string
  {ao/identities #{:person/id}
   ao/schema     :production})

(def attributes [id first-name last-name])
