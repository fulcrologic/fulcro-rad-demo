(ns com.example.model.tag
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [clojure.string :as str]))

(defattr id ::id :uuid
  {::attr/authority :local
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::attr/identity? true})

(defattr label ::label :string
  {::attr/authority     :local
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :db/unique           :db.unique/value
   :db/index            true
   ::attr/normalize     (fn [v] (str/capitalize v))
   ::attr/required?     true})

(def attributes [id label])

