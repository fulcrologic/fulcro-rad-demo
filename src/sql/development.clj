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
  (jdbc/execute! (get-jdbc-datasource) [(mig/automatic-schema :production all-attributes)])

  (sql/query (get-jdbc-datasource) ["show tables"])
  (sql/query (get-jdbc-datasource) ["show columns from address"])
  (sql/query (get-jdbc-datasource) ["SELECT * FROM account WHERE email = ?" "sam@example.com"])
  (sql/query (get-jdbc-datasource) ["SELECT id,name,active FROM account WHERE id IN ('ffffffff-ffff-ffff-ffff-000000000001','ffffffff-ffff-ffff-ffff-000000000002','ffffffff-ffff-ffff-ffff-000000000003','ffffffff-ffff-ffff-ffff-000000000004')"])
  (sql/insert! (get-jdbc-datasource) "account" {:id (new-uuid 1) :name "Tony"})
  (sql/query (get-jdbc-datasource) ["SELECT * FROM ACCOUNT"]))

;; TASK: See datomic seeding, and match it (development.clj in datomic src dir)
(defn new-account [id name email password salt iterations & {:as add-ons}]
  (merge
    {:id                  id
     :name                name
     :email               email
     :password_salt       salt
     :password_iterations iterations
     :password            (attr/encrypt password salt iterations)}
    add-ons))

(defn seed! []
  (let [db         (get-jdbc-datasource)
        salt       "lkjhasdf908dasyu"
        iterations 1000]
    ;(jdbc/execute! db ["DELETE FROM ACCOUNT"])
    (doseq [row [(new-account (new-uuid 1) "Tony" "tony@example.com" "letmein" salt iterations)
                 (new-account (new-uuid 2) "Sam" "sam@example.com" "letmein" salt iterations)
                 (new-account (new-uuid 3) "Rose" "rose@example.com" "letmein" salt iterations)
                 (new-account (new-uuid 4) "Bill" "bill@example.com" "letmein" salt iterations)]]
      (sql/insert! db "account" row))))

(comment
  (seed!))

(defn start []
  (mount/start-with-args {:config "config/dev.edn"})
  (seed!)
  :ok)

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
    (rad.sql/entity-query {::rad.sql/schema                      :production
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
