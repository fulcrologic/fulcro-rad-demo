(ns development
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.repl :refer [doc source]]
    [clojure.tools.namespace.repl :as tools-ns :refer [disable-reload! refresh clear set-refresh-dirs]]
    [com.example.components.ring-middleware]
    [com.example.components.server]
    [com.example.components.connection-pools]
    [com.example.components.database-queries :as queries]
    [com.example.model.account :as account]
    [com.example.model.category :as category]
    [com.example.model :refer [all-attributes]]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.database-adapters.sql :as rad.sql]
    [com.fulcrologic.rad.database-adapters.sql.migration :as mig]
    [com.fulcrologic.rad.database-adapters.sql.schema :as schema]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.resolvers :as res]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [com.example.components.connection-pools :as pools])
  (:import (com.zaxxer.hikari HikariDataSource)))

(set-refresh-dirs "src/main" "src/sql" "src/dev" "src/shared" "../fulcro-rad/src/main" "../fulcro-rad-sql/src/main")

(defn get-jdbc-datasource
  "Returns a clojure jdbc compatible data source config."
  []
  (let [ds ^HikariDataSource (some-> pools/connection-pools :main)]
    ds))

(defn add-namespace [nspc k] (keyword nspc (name k)))
(comment
  (schema/tables-and-columns (attr/attribute-map account/attributes) account/name)
  (mig/attr->ops :production (attr/attribute-map account/attributes) account/name)
  (doseq [stmt (mig/automatic-schema :production nil all-attributes)]
    (log/info stmt)
    (jdbc/execute! (get-jdbc-datasource) [stmt]))

  (sql/query (get-jdbc-datasource) ["show tables"])
  (sql/query (get-jdbc-datasource) ["show columns from item"])
  (sql/query (get-jdbc-datasource) ["select * from INFORMATION_SCHEMA.TABLE_CONSTRAINTS"])
  (sql/query (get-jdbc-datasource) ["select i.*, ic.COLUMN_NAME from INFORMATION_SCHEMA.INDEXES i JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic on i.INDEX_NAME=ic.INDEX_NAME where i.table_name='item'"])
  (jdbc/execute! (get-jdbc-datasource) ["CREATE SEQUENCE account_id_seq;"])
  (jdbc/execute! (get-jdbc-datasource) ["CREATE SEQUENCE account_id_seq;"])
  (jdbc/execute! (get-jdbc-datasource) ["SELECT NEXTVAL('account_id_seq') AS id"])
  (sql/query (get-jdbc-datasource) ["show columns from address"])
  (sql/query (get-jdbc-datasource) ["SELECT account.id, array_agg(address.id) AS addrs FROM account LEFT JOIN address ON address.account_addresses_account_id = account.id
  WHERE email IN ('sam@example.com', 'rose@example.com') GROUP BY account.id"])
  (sql/query (get-jdbc-datasource) ["SELECT id,name,active FROM account WHERE id IN ('ffffffff-ffff-ffff-ffff-000000000001','ffffffff-ffff-ffff-ffff-000000000002','ffffffff-ffff-ffff-ffff-000000000003','ffffffff-ffff-ffff-ffff-000000000004')"])
  (sql/insert! (get-jdbc-datasource) "account" {:id (new-uuid 1) :name "Tony"})
  (sql/query (get-jdbc-datasource) ["SELECT * FROM ACCOUNT"]))

(defn new-account [id name email password salt iterations & {:as add-ons}]
  [:account (merge
              {:id                  id
               :name                name
               :email               email
               :active              true
               :password_salt       salt
               :password_iterations iterations
               :password            (attr/encrypt password salt iterations)}
              add-ons)])

(defn new-address [id street & {:as add-ons}]
  [:address (merge
              {:id     id
               :street street}
              add-ons)])

(defn new-category [id label & {:as add-ons}]
  [:category (merge
               {:id    id
                :label label}
               add-ons)])

(defn new-item [id label price category-id]
  [:item {:id       id
          :name     label
          :price    price
          :category category-id}])

(defn seed! []
  (let [db         (get-jdbc-datasource)
        salt       "lkjhasdf908dasyu"
        iterations 1000
        misc-id    (new-uuid 1003)
        toys-id    (new-uuid 1002)
        tools-id   (new-uuid 1000)
        exists?    (boolean (sql/get-by-id db :account (new-uuid 100)))]
    (if exists?
      (log/info "Database already seeded. Skipping")
      (do
        (log/info "Seeding development data")
        (doseq [row [(new-address (new-uuid 300) "222 Other")
                     (new-account (new-uuid 100) "Tony" "tony@example.com" "letmein" salt iterations
                       :account/primary_address (new-uuid 300)
                       :account/role ":account.role/superuser"
                       :zone_id ":time-zone.zone-id/America-Los_Angeles")
                     (new-address (new-uuid 1) "111 Main St." :account_addresses_account_id (new-uuid 100))
                     (new-account (new-uuid 101) "Sam" "sam@example.com" "letmein" salt iterations
                       :account/role ":account.role/user")
                     (new-account (new-uuid 102) "Sally" "sally@example.com" "letmein" salt iterations)
                     (new-account (new-uuid 103) "Barbara" "barb@example.com" "letmein" salt iterations)
                     (new-category (new-uuid 1000) "Tools")
                     (new-category (new-uuid 1002) "Toys")
                     (new-category (new-uuid 1003) "Misc")
                     (new-item (new-uuid 200) "Widget" 33.99 misc-id)
                     (new-item (new-uuid 201) "Screwdriver" 4.99 tools-id)
                     (new-item (new-uuid 202) "Wrench" 14.99 tools-id)
                     (new-item (new-uuid 203) "Hammer" 14.99 tools-id)
                     (new-item (new-uuid 204) "Doll" 4.99 toys-id)
                     (new-item (new-uuid 205) "Robot" 94.99 toys-id)
                     (new-item (new-uuid 206) "Building Blocks" 24.99 toys-id)]]
          (try
            (let [[table entity] row]
              (sql/insert! db table entity))
            (catch Exception e
              (log/error e row))))))))

(comment
  (seed!))

(defn start []
  (mount/start-with-args {:config "config/dev.edn"})
  (seed!)
  :ok)

(defn cli-start "Start & seed the app from the CLI using `clojure -X ..`" [_] (start))

(defn stop
  "Stop the server."
  []
  (mount/stop))

(def go start)

(defn restart
  "Stop, refresh, and restart the server."
  []
  (stop)
  (tools-ns/refresh :after 'development/start))

(def reset #'restart)

(comment
  (let [db {:dbtype   "postgresql"
            :dbname   "example"
            :host     "localhost"
            :user     "postgres"
            :password ""}]
    #_(queries/get-all-accounts {:sql/databases {:production db}} {:ui/show-inactive? true})
    (rad.sql/entity-query {::attr/schema                         :production
                           ::rad.sql/attributes                  account/attributes
                           :com.wsscode.pathom.core/parent-query [::account/name ::account/active?]
                           ::rad.sql/id-attribute                account/id
                           ::rad.sql/databases                   {:production db}} {::account/id #uuid "ffffffff-ffff-ffff-ffff-000000000001"})
    #_(jdbc/query db ["SELECT * FROM account"])
    #_(jdbc/execute! db [(rad.sql/automatic-schema :production account/attributes)]))

  (rad.sql/generate-resolvers account/attributes :production)

  (rad.sql/column-names account/attributes [::account/id ::account/active?])

  (contains? #{::account/name} (::attr/qualified-key account/name))
  )
