(ns com.example.model.company
  (:refer-clojure :exclude [name])
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]))

(defattr id :company/id :uuid
  {ao/identity? true
   ao/schema    :production})

; name is :entity/name
; email is :entity/email

(defattr classification :company/classification :enum
  {ao/identities        #{:company/id}
   ao/schema            :production
   ao/enumerated-values #{:company.classification/llc :company.classification/c-corp :company.classification/s-corp :company.classification/non-profit}
   ao/enumerated-labels {:company.classification/llc        "LLC"
                         :company.classification/c-corp     "C Corp"
                         :company.classification/s-corp     "S Corp"
                         :company.classification/non-profit "Non-profit"}})

(def attributes [id classification])
