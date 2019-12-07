(ns com.example.components.database-queries
  (:require
    [com.fulcrologic.rad.database-adapters.sql :as sql]
    [clojure.java.jdbc :as jdbc]))

(defn add-namespace [nspc k] (keyword nspc (name k)))

(defn get-all-accounts
  [env query-params]
  (let [db             (get-in env [::sql/databases :production])
        show-inactive? (:ui/show-inactive? query-params)
        sql            (str "SELECT id FROM account" (when-not show-inactive? " WHERE active = true"))
        rows           (jdbc/query db sql {:keywordize    false
                                           :result-set-fn vec
                                           :identifiers   (partial add-namespace "com.example.model.account")})]
    rows))

