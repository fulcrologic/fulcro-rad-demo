(ns com.example.model.address
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id ::id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   :com.fulcrologic.rad.database-adapters.datomic/entity ::address
   ::auth/authority                                      :local
   :com.fulcrologic.rad.database-adapters.sql/schema     :production
   :com.fulcrologic.rad.database-adapters.sql/tables     #{"addresses"}})

(defattr street ::street :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"addresses"}})

(defattr city ::city :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"addresses"}})

(def states #:com.example.model.address.state {:AZ "Arizona"
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

(defattr state ::state :enum
  {::attr/enumerated-values                                  (set (keys states))
   ::attr/labels                                             states
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"addresses"}})

(defattr zip ::zip :string
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity     ::address
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"addresses"}})

(def attributes [id street city state zip])
