(ns com.example.model.address
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :address/id :uuid
  {ao/identity?                                     true
   :com.fulcrologic.rad.database-adapters.sql/table "address"
   ao/schema                                        :production})

(defattr street :address/street :string
  {ao/schema     :production
   ao/identities #{:address/id}})

(defattr city :address/city :string
  {ao/schema     :production
   ao/identities #{:address/id}})

(def states #:address.state {:AZ "Arizona"
                             :AL "Alabama"
                             :AK "Alaska"
                             :CA "California"
                             :CT "Connecticut"
                             :DE "Deleware"
                             :GA "Georgia"
                             :HI "Hawaii"
                             :KS "Kansas"
                             :MS "Mississippi"
                             :MO "Missouri"
                             :OR "Oregon"
                             :WA "Washington"})

(defattr state :address/state :enum
  {ao/enumerated-values (set (keys states))
   ao/identities        #{:address/id}
   ao/schema            :production
   ao/enumerated-labels states})

(defattr zip :address/zip :string
  {ao/identities #{:address/id}
   ao/schema     :production})

(def attributes [id street city state zip])
