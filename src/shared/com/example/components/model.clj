(ns com.example.components.model
  (:require
    [com.example.model.account :as account]
    [com.example.model.address :as address]
    [mount.core :refer [defstate args]]))

(defstate all-attributes
  :start
  (let [all-attributes (vec (concat
                              account/attributes
                              address/attributes))]
    all-attributes))
