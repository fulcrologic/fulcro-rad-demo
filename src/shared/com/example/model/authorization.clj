(ns com.example.model.authorization
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :as fmw]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.attributes :as attr]
    [com.example.components.database-queries :as queries]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]
    [com.example.model.timezone :as timezone]))

(defn login!
  "Implementation of login. This is database-specific and is not further generalized for the demo."
  [env {:keys [username password]}]
  (log/info "Attempt login for" username)
  (enc/if-let [{:account/keys   [name]
                :time-zone/keys [zone-id]
                :password/keys  [hashed-value salt iterations]} (queries/get-login-info env username)
               current-hashed-value (attr/encrypt password salt iterations)]
    (if (= hashed-value current-hashed-value)
      (do
        (log/info "Login for" username)
        (let [s {::auth/provider    :local
                 ::auth/status      :success
                 :time-zone/zone-id (-> zone-id :db/ident timezone/datomic-time-zones)
                 :account/name      name}]
          (fmw/augment-response s (fn [resp]
                                    (let [current-session (-> env :ring/request :session)]
                                      (assoc resp :session (merge current-session s)))))))
      (do
        (log/error "Login failure for" username)
        {::auth/provider :local
         ::auth/status   :failed}))
    (do
      (log/fatal "Login cannot find user" username)
      {::auth/provider :local
       ::auth/status   :failed})))

(defn logout!
  "Implementation of logout."
  [env]
  (fmw/augment-response {} (fn [resp] (assoc resp :session {}))))

(defn check-session! [env]
  (log/info "Checking for existing session")
  (or
    (some-> env :ring/request :session)
    {::auth/provider :local
     ::auth/status   :not-logged-in}))

