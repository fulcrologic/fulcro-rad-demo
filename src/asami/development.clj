(ns development
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.repl :refer [doc source]]
    [clojure.tools.namespace.repl :as tools-ns :refer [disable-reload! refresh clear set-refresh-dirs]]
    [com.example.components.asami :refer [asami-connections]]
    [com.example.components.config]
    [com.example.components.ring-middleware]
    [com.example.components.server]
    [com.example.model.seed :as seed]
    [com.example.model.account :as account]
    [com.example.model.address :as address]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [cz.holyjak.rad.database-adapters.asami :as asami]
    [cz.holyjak.rad.database-adapters.asami.connect :as asami.conn]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [asami.core :as d]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.type-support.date-time :as dt]))

(defn get-db [] (d/db (:main asami-connections)))

(defn get-db-in-env []
  {::asami/databases {:production (atom (d/db (:main asami-connections)))}})

(set-refresh-dirs "src/main" "src/asami" "src/dev" "src/shared")

(defn delete-db!
  "Delete the DB. BEWARE: This invalidates `com.example.components.asami/asami-connections` and thus requires a restart."
  []
  (d/delete-database (asami.conn/config->url (-> com.example.components.config/config ::asami/databases :main))))

(defn- add-asami-id [entity]
  (let [[id-prop & more] (->> (keys entity) (filter #(and (= "id" (name %)) (namespace %))))
        _ (assert (empty? more) "More than one :xxx/id props found in the entity, don't know which is the id")
        ident [id-prop (id-prop entity)]]
    (assoc entity :id ident)))

(defn seed! []
  (dt/set-timezone! "America/Los_Angeles")
  (let [connection (:main asami-connections)
        fresh?     (zero? (-> connection d/db d/as-of-t))
        date-1     (dt/html-datetime-string->inst "2020-01-01T12:00")
        date-2     (dt/html-datetime-string->inst "2020-01-05T12:00")
        date-3     (dt/html-datetime-string->inst "2020-02-01T12:00")
        date-4     (dt/html-datetime-string->inst "2020-03-10T12:00")
        date-5     (dt/html-datetime-string->inst "2020-03-21T12:00")]
    (when (and connection (not fresh?))
      (log/info "NOT seeding data b/c the DB is already seeded. Run `development/delete-db!` to re-seed."))
    (when fresh?
      (log/info "SEEDING data.")
      (->> [(seed/new-address (new-uuid 1) "111 Main St.")
            (seed/new-account (new-uuid 100) "Tony" "tony@example.com" "letmein"
                              :account/addresses [[:id [:address/id (new-uuid 1)]]]
                              :account/primary-address (seed/new-address (new-uuid 300) "222 Other")
                              :time-zone/zone-id :time-zone.zone-id/America-Los_Angeles)
            (seed/new-account (new-uuid 101) "Sam" "sam@example.com" "letmein")
            (seed/new-account (new-uuid 102) "Sally" "sally@example.com" "letmein")
            (seed/new-account (new-uuid 103) "Barbara" "barb@example.com" "letmein")
            (seed/new-category (new-uuid 1000) "Tools")
            (seed/new-category (new-uuid 1002) "Toys")
            (seed/new-category (new-uuid 1003) "Misc")
            (seed/new-item (new-uuid 200) "Widget" 33.99
                           :item/category [:id [:category/id (new-uuid 1003)]])
            (seed/new-item (new-uuid 201) "Screwdriver" 4.99
                           :item/category [:id [:category/id (new-uuid 1000)]])
            (seed/new-item (new-uuid 202) "Wrench" 14.99
                           :item/category [:id [:category/id (new-uuid 1000)]])
            (seed/new-item (new-uuid 203) "Hammer" 14.99
                           :item/category [:id [:category/id (new-uuid 1000)]])
            (seed/new-item (new-uuid 204) "Doll" 4.99
                           :item/category [:id [:category/id (new-uuid 1002)]])
            (seed/new-item (new-uuid 205) "Robot" 94.99
                           :item/category [:id [:category/id (new-uuid 1002)]])
            (seed/new-item (new-uuid 206) "Building Blocks" 24.99
                           :item/category [:id [:category/id (new-uuid 1002)]])
            (seed/new-invoice date-1 (new-uuid 100) ; Tony
                              (->> [(seed/new-line-item (new-uuid 204) 1 5.0M)
                                    (seed/new-line-item (new-uuid 203) 1 14.99M)]
                                   (mapv add-asami-id)))
            (seed/new-invoice date-2 (new-uuid 102) ; Sally
                              (->> [(seed/new-line-item (new-uuid 202) 1 12.50M)
                                    (seed/new-line-item (new-uuid 200) 2 32.0M)]
                                   (mapv add-asami-id)))
            (seed/new-invoice date-3 (new-uuid 101) ; Sam
                              (->> [(seed/new-line-item (new-uuid 202) 2 12.50M)
                                    (seed/new-line-item (new-uuid 203) 2 12.50M)]
                                   (mapv add-asami-id)))
            (seed/new-invoice date-4 (new-uuid 102) ; Sally
                              (->> [(seed/new-line-item (new-uuid 205) 6 89.99M)]
                                   (mapv add-asami-id)))
            (seed/new-invoice date-5 (new-uuid 103) ; Barbara
                              (->> [(seed/new-line-item (new-uuid 206) 10 20.0M)]
                                   (mapv add-asami-id)))]
           (map add-asami-id)
           (map #(d/transact connection %))
           last
           deref))))

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
  (when (:main asami-connections)
    (delete-db!))
  (stop)
  (tools-ns/refresh :after 'development/start))

(def reset #'restart)

(comment
  (go)
  (restart))
