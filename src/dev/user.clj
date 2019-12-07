(ns user
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl
     :as tools-ns
     :refer [disable-reload! refresh clear set-refresh-dirs]]
    [expound.alpha :as expound]
    [taoensso.timbre :as log]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

