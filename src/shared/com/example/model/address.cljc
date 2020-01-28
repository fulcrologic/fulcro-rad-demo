(ns com.example.model.address
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :address/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   :com.fulcrologic.rad.database-adapters.datomic/entity ::address
   ::auth/authority                                      :local})

(defattr street :address/street :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:address/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address})

(defattr city :address/city :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:address/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address})

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
  {::attr/enumerated-values                                  (set (keys states))
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:address/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address
   ::attr/labels                                             states})

(defattr zip :address/zip :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:address/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address})

(def attributes [id street city state zip])
