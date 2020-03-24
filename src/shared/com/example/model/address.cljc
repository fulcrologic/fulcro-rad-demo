(ns com.example.model.address
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :address/id :uuid
  {::attr/identity?                                          true
   :com.fulcrologic.rad.database-adapters.sql/table          "address"
   ::attr/schema                                             :production
   ::auth/authority                                          :local})

(defattr street :address/street :string
  {::attr/schema     :production
   ::attr/identities #{:address/id} })

(defattr city :address/city :string
  {::attr/schema     :production
   ::attr/identities #{:address/id} })

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
  {::attr/enumerated-values (set (keys states))
   ::attr/identities        #{:address/id}
   ::attr/schema            :production
   ::attr/enumerated-labels states})

(defattr zip :address/zip :string
  {::attr/identities #{:address/id}
   ::attr/schema     :production})

(def attributes [id street city state zip])
