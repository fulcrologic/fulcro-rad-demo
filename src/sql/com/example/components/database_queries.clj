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
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params (if id
                       ["SELECT id FROM item WHERE category = ?" id]
                       ["SELECT id FROM item"])
        rows         (mapv #(hash-map :item/id (:ITEM/ID %)) (jdbc/query data-source query-params))]
    rows))

(defn get-all-invoices
  [env _]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params ["SELECT id FROM invoice"]
        rows         (mapv #(hash-map :invoice/id (:INVOICE/ID %)) (jdbc/query data-source query-params))]
    rows))

(defn get-all-categories
  [env _]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params ["SELECT id FROM category"]
        rows         (mapv #(hash-map :category/id (:CATEGORY/ID %)) (jdbc/query data-source query-params))]
    rows))

(defn get-line-item-category [env line-item-id]
  (let [data-source  (get-in env [::sql/connection-pools :production])
        query-params [(str
                        "SELECT item.category FROM item "
                        "INNER JOIN line_item ON line_item.item = item.id "
                        "WHERE line_item.id = ?") line-item-id]]
    {:category/id (:ITEM/CATEGORY (first (jdbc/query data-source query-params)))}))

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
