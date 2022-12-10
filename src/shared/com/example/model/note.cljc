(ns com.example.model.note
  (:require
    [clojure.string :as str]
    [com.fulcrologic.rad.attributes :refer [defattr]]
    #?(:clj [com.example.components.database-queries :as queries])
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.report-options :as ro]))

(defattr id :note/id :uuid
  {ao/identity? true
   ao/schema    :production})

(defattr content :note/content :string
  {ao/identities #{:note/id}
   ao/schema     :production})

(defattr parties :note/parties :ref
  {ao/identities       #{:note/id}
   ao/cardinality      :many
   ro/column-EQL       {:note/parties [:person/id :person/first-name :company/id :entity/name]}
   ro/column-formatter (fn [_ v _ _] (str/join ", " (map (fn [n] (or (:person/first-name n) (:entity/name n))) v)))
   ao/targets          #{:company/id :person/id}
   ao/schema           :production})

(defattr all-notes :note/all-notes :ref
  {ao/target     :note/id
   ao/pc-output  [{:note/all-notes [:note/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:note/all-notes (queries/get-all-notes env query-params)}))})

(def attributes [id all-notes content parties])
