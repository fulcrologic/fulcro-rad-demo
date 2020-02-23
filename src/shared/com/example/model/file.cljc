(ns com.example.model.file
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.blob :as blob]))

(defattr id :file/id :uuid
  {::attr/identity?                                          true
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"file"}})

(blob/defblobattr sha :file/sha :files :remote
  {:com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"file"}})

(defattr filename :file.sha/filename :string
  {:com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"file"}})

(defattr uploaded-on :file/uploaded-on :instant
  {:com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"file"}})

(def attributes [id sha filename uploaded-on])
