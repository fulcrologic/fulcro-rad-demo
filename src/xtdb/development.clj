(ns development
  (:require
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
   [com.example.components.xtdb :refer [xtdb-nodes]]
   [com.example.components.ring-middleware]
   [com.example.components.server]
   [com.example.model.seed :as seed]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [mount.core :as mount]
   [taoensso.timbre :as log]
   [xtdb.api :as xt]
   [com.fulcrologic.rad.type-support.date-time :as dt]))

(set-refresh-dirs "src/main" "src/xtdb" "src/dev" "src/shared")

(defn seed! []
  (dt/set-timezone! "America/Los_Angeles")
  (let [node (:main xtdb-nodes)
        date-1     (dt/html-datetime-string->inst "2020-01-01T12:00")
        date-2     (dt/html-datetime-string->inst "2020-01-05T12:00")
        date-3     (dt/html-datetime-string->inst "2020-02-01T12:00")
        date-4     (dt/html-datetime-string->inst "2020-03-10T12:00")
        date-5     (dt/html-datetime-string->inst "2020-03-21T12:00")
        add (fnil conj [])]
    (when node
      (log/info "SEEDING data.")
      (let [data (-> {}
                     (update :addresses add (seed/new-address (new-uuid 1) "111 Main St."))
                     (update :addresses add (seed/new-address (new-uuid 300) "222 Other"))
                     (as-> d (update d :accounts add (seed/new-account (new-uuid 100) "Tony" "tony@example.com" "letmein"
                                                                                  :account/addresses #{(get-in d [:addresses 0 :xt/id])}
                                                                                  :account/primary-address (get-in d [:addresses 1 :xt/id])
                                                                                  :time-zone/zone-id :time-zone.zone-id/America-Los_Angeles)))
                     (update :accounts   add (seed/new-account (new-uuid 101) "Sam" "sam@example.com" "letmein"))
                     (update :accounts   add (seed/new-account (new-uuid 102) "Sally" "sally@example.com" "letmein"))
                     (update :accounts   add (seed/new-account (new-uuid 103) "Barbara" "barb@example.com" "letmein"))
                     (update :categories add (seed/new-category (new-uuid 1000) "Tools"))
                     (update :categories add (seed/new-category (new-uuid 1002) "Toys"))
                     (update :categories add (seed/new-category (new-uuid 1003) "Misc"))
                     (as-> d (update d :items add (seed/new-item (new-uuid 200) "Widget" 33.99
                                                                            :item/category (get-in d [:categories 2 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 201) "Screwdriver" 4.99
                                                                            :item/category (get-in d [:categories 0 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 202) "Wrench" 14.99
                                                                            :item/category (get-in d [:categories 0 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 203) "Hammer" 14.99
                                                                            :item/category (get-in d [:categories 0 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 204) "Doll" 4.99
                                                                            :item/category (get-in d [:categories 1 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 205) "Robot" 94.99
                                                                            :item/category (get-in d [:categories 1 :xt/id]))))
                     (as-> d (update d :items add (seed/new-item (new-uuid 206) "Building Blocks" 24.99
                                                                            :item/category (get-in d [:categories 1 :xt/id]))))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 212) (get-in d [:items 4 :xt/id]) 1 5.0M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 213) (get-in d [:items 3 :xt/id]) 1 14.99M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 214) (get-in d [:items 2 :xt/id]) 1 12.50M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 215)(get-in d [:items 0 :xt/id]) 2 32.0M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 216)(get-in d [:items 2 :xt/id]) 2 12.50M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 217)(get-in d [:items 3 :xt/id]) 2 12.50M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 218)(get-in d [:items 5 :xt/id]) 6 89.99M)))
                     (as-> d (update d :line-items add (seed/new-line-item (new-uuid 219)(get-in d [:items 6 :xt/id]) 10 20.0M)))
                     (as-> d (update d :invoices add (seed/new-invoice (new-uuid 207) date-1 (get-in d [:accounts 0 :xt/id])
                                                                                  [(get-in d [:line-items 0]) (get-in d [:line-items 1])])))
                     (as-> d (update d :invoices add (seed/new-invoice (new-uuid 208) date-2 (get-in d [:accounts 2 :xt/id])
                                                                                  [(get-in d [:line-items 2]) (get-in d [:line-items 3])])))
                     (as-> d (update d :invoices add (seed/new-invoice (new-uuid 209) date-3 (get-in d [:accounts 1 :xt/id])
                                                                                  [(get-in d [:line-items 4]) (get-in d [:line-items 5])])))
                     (as-> d (update d :invoices add (seed/new-invoice (new-uuid 210) date-4 (get-in d [:accounts 2 :xt/id])
                                                                                  [(get-in d [:line-items 6])])))
                     (as-> d (update d :invoices add (seed/new-invoice (new-uuid 211) date-5 (get-in d [:accounts 3 :xt/id])
                                                                                  [(get-in d [:line-items 7])]))))]
        (xt/submit-tx node (->> data
                                  vals
                                  flatten
                                  (mapv (fn [d] [::xt/put d]))))))))

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
  (start)
  )
