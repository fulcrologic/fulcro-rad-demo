(ns com.example.components.blob-store
  (:require
    [com.fulcrologic.rad.blob-storage :as storage]
    [mount.core :refer [defstate]]))

(defstate temporary-blob-store
  :start
  (storage/leaky-blob-store ""))

(defstate image-blob-store
  :start
  (storage/leaky-blob-store "/images"))

