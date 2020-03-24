(ns com.example.components.database-queries
  (:require
    [com.fulcrologic.rad.database-adapters.sql :as sql]
    [com.fulcrologic.rad.database-adapters.sql.query :as query]
    [next.jdbc.sql :as jdbc]
    [taoensso.timbre :as log]))

(defn add-namespace [nspc k] (keyword nspc (name k)))

(defn get-all-accounts
  [env query-params]
  (let [data-source    (get-in env [::sql/connection-pools :production])
        show-inactive? (:ui/show-inactive? query-params)
        sql            (str "SELECT id FROM account" (when-not show-inactive? " WHERE active = true"))
        rows           (mapv #(hash-map :account/id (:id %)) (jdbc/query data-source [sql] {:builder-fn query/row-builder}))]
    rows))

(defn get-all-items
  [env {:category/keys [id]}]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params (if id
                       ["SELECT id FROM item WHERE category = ?" (log/spy :info id)]
                       ["SELECT id FROM item"])
        rows         (mapv #(hash-map :item/id (:id %)) (jdbc/query data-source query-params {:builder-fn query/row-builder}))]
    rows))

(defn get-all-invoices
  [env _]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params ["SELECT id FROM invoice"]
        rows         (mapv #(hash-map :invoice/id (:id %)) (jdbc/query data-source query-params {:builder-fn query/row-builder}))]
    rows))

(defn get-all-categories
  [env _]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params ["SELECT id FROM category"]
        rows         (mapv #(hash-map :category/id (:id %)) (jdbc/query data-source query-params {:builder-fn query/row-builder}))]
    rows))

(defn get-line-item-category [env line-item-id]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params [(str
                        "SELECT item.category FROM item "
                        "INNER JOIN line_item ON line_item.item = item.id "
                        "WHERE line_item.id = ?") line-item-id]]
    (:category (first (jdbc/query data-source query-params {:builder-fn query/row-builder})))))

(defn get-login-info
  "Get the account name, time zone, and password info via a username (email)."
  [env username]
  (let [data-source (get-in env [::sql/connection-pools :production])
        rows        (jdbc/query data-source ["SELECT name, password, password_salt, password_iterations FROM account WHERE email = ?" username] {:builder-fn query/row-builder})
        {:keys [name password password_salt password_iterations]} (first rows)]
    (when name
      {:account/name          name
       :password/hashed-value password
       :password/salt         password_salt
       :password/iterations   password_iterations})))
