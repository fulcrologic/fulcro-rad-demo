(ns com.example.model.file
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.blob :as blob]))

(defattr id :file/id :uuid
  {ao/identity?                                      true
   ao/identities                                     #{:file/id}
   ao/schema                                         :production
   :com.fulcrologic.rad.database-adapters.sql/tables #{"file"}})

(blob/defblobattr sha :file/sha :files :remote
  {ao/identities                                     #{:file/id}
   ao/schema                                         :production
   :com.fulcrologic.rad.database-adapters.sql/tables #{"file"}})

(defattr filename :file.sha/filename :string
  {ao/schema                                         :production
   ao/identities                                     #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/tables #{"file"}})

(defattr uploaded-on :file/uploaded-on :instant
  {ao/schema                                         :production
   ao/identities                                     #{:file/id}
   :com.fulcrologic.rad.database-adapters.sql/tables #{"file"}})

(def attributes [id sha filename uploaded-on])
