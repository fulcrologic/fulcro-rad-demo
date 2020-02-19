(ns com.example.components.save-middleware
  (:require
    [clojure.pprint :refer [pprint]]
    [edn-query-language.core :as eql]
    [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.blob-storage :as bs]
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [com.rpl.specter :as sp]
    [clojure.set :as set]))

(defn wrap-persist-images
  [handler {:keys [image-keys]}]
  (log/info "Wrapping persist-images with image keys" image-keys)
  (fn [{::blob/keys [temporary-storage image-store] :as pathom-env} params]
    (try
      (log/info "Check for images to persist in " params)
      (when-not temporary-storage
        (log/error "No temporary storage in pathom env."))
      (when-not image-store
        (log/error "No image storage in pathom env. Cannot save images."))
      ;; TODO: Move temp file to image file
      (when-not (seq image-keys)
        (log/warn "wrap-persist-images is installed in form middleware, but it has been given not image keys."))
      (pprint params)
      (let [delta        (::form/delta params)
            image-keys   [:account/avatar-url]
            image-sha    (set (sp/select [sp/MAP-VALS (sp/pred map?) ::blob/file-sha :after string?] delta))
            pruned-delta (sp/transform [sp/MAP-VALS (sp/pred map?)] #(select-keys % image-keys) delta)
            to-save      (set (sp/select [sp/MAP-VALS sp/MAP-VALS
                                          (sp/pred #(and
                                                      (map? %)
                                                      ;(nil? (:before %))
                                                      (contains? image-sha (:after %)))) :after]
                                pruned-delta))]
        (doseq [sha to-save]
          (log/info "Moving image to permanent storage" sha)
          (bs/move-blob! temporary-storage sha image-store)))
      (catch Exception e
        (log/error e "Failed to save image.")))
    (handler pathom-env params)))

(def middleware
  (-> r.s.middleware/rewrite-values
    (wrap-persist-images {:image-keys #{:account/avatar-url}})))
