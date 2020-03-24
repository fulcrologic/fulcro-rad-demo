(ns com.example.model.category
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.wsscode.pathom.connect :as pc]
    #?(:clj [com.example.components.database-queries :as queries])))

(defsc Category [_ _]
  {:query [:category/id :category/label]
   :ident :category/id})

(defattr id :category/id :uuid
  {::attr/identity? true
   ::attr/schema    :production})

(defattr label :category/label :string
  {::attr/required?                                      true
   ::attr/identities                                     #{:category/id}
   :com.fulcrologic.rad.database-adapters.sql/max-length 120
   ::attr/schema                                         :production})

(defattr all-categories :category/all-categories :ref
  {::attr/target :category/id
   ::pc/output   [{:category/all-categories [:category/id]}]
   ::pc/resolve  (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:category/all-categories (queries/get-all-categories env query-params)}))})

(def attributes [id label all-categories])

