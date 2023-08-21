(ns com.example.components.blob-store
  (:require
    [com.fulcrologic.rad.blob-storage :as storage]
    [mount.core :refer [defstate]]))

(defstate temporary-blob-store
  :start
  (storage/transient-blob-store "/preview" 5))

(defstate image-blob-store
  :start
  (storage/transient-blob-store "/images" 10000))

(defstate file-blob-store
  :start
  (storage/transient-blob-store "/files" 10000))

