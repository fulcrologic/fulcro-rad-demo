(ns user
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl
     :as tools-ns]
    [expound.alpha :as expound]))

(tools-ns/set-refresh-dirs "src/dev" "src/shared" "src/datomic"
  "../fulcro-rad/src/main"
  "../fulcro-rad/src/test"
  "../fulcro-rad-datomic/src/main"
  "../fulcro-rad-datomic/src/test")

(alter-var-root #'s/*explain-out* (constantly expound/printer))
