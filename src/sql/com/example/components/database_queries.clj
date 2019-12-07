(ns com.example.components.database-queries
  (:require
    [clojure.java.jdbc :as jdbc]))

(defn add-namespace [nspc k] (keyword nspc (name k)))

(defn get-all-accounts
  [env query-params]
  (let [db             (get-in env [:sql/databases :production])
        show-inactive? (:ui/show-inactive? query-params)
        sql            (str "SELECT id FROM account " (when show-inactive? "WHERE active = ?"))
        params         (cond-> [sql] show-inactive? (conj true))
        rows           (jdbc/query db params {:keywordize    false
                                              :result-set-fn vec
                                              :identifiers   (partial add-namespace "com.example.model.account")})]
    rows))

