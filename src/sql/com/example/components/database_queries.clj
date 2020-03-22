(ns com.example.components.database-queries
  (:require
    [com.fulcrologic.rad.database-adapters.sql :as sql]
    [next.jdbc.sql :as jdbc]))

(defn add-namespace [nspc k] (keyword nspc (name k)))

(defn get-all-accounts
  [env query-params]
  (let [data-source    (get-in env [::sql/connection-pools :production])
        show-inactive? (:ui/show-inactive? query-params)
        sql            (str "SELECT id FROM account" (when-not show-inactive? " WHERE active = true"))
        rows           (mapv #(hash-map :account/id (:ACCOUNT/ID %)) (jdbc/query data-source [sql]))]
    rows))

(defn get-all-items
  [env {:category/keys [id]}]
  )

(defn get-all-invoices
  [env query-params]
  )

(defn get-all-categories
  [env query-params]
  )

(defn get-line-item-category [env line-item-id]
  )

(defn get-login-info
  "Get the account name, time zone, and password info via a username (email)."
  [env username]
  (let [data-source (get-in env [::sql/connection-pools :production])
        rows        (jdbc/query data-source ["SELECT * FROM account WHERE email = ?" username])
        {:ACCOUNT/keys [NAME PASSWORD PASSWORD_SALT PASSWORD_ITERATIONS]} (first rows)]
    (when NAME
      {:account/name          NAME
       :password/hashed-value PASSWORD
       :password/salt         PASSWORD_SALT
       :password/iterations   PASSWORD_ITERATIONS})))
