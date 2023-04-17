(ns com.example.model.authorization
  (:require
    [com.example.components.database-queries :as queries]
    [com.example.model.timezone :as timezone]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.authorization :as auth]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(defn login!
  "Implementation of login. This is database-specific and is not further generalized for the demo."
  [env {:keys [username password]}]
  (log/info "Attempt login for" username)
  (enc/if-let [{:account/keys   [name]
                :time-zone/keys [zone-id]
                :password/keys  [hashed-value salt iterations]} (log/spy :info (queries/get-login-info env username))
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
                                      (assoc resp :session (vary-meta (merge current-session s) assoc :recreate true)))))))
      (do
        (log/error "Login failure for" username)
        {::auth/provider :local
         ::auth/status   :failed}))
    (do
      (log/fatal "Login cannot find user" username)
      {::auth/provider :local
       ::auth/status   :failed})))

(defn logout!
  "Implementation of logout. Retains CSRF token and rotates session key"
  [env]
  (fmw/augment-response {} (fn [resp] (assoc resp :session (vary-meta (select-keys (-> env :ring/request :session) [:ring.middleware.anti-forgery/anti-forgery-token]) assoc :recreate true)))))

(defn check-session! [env]
  (log/info "Checking for existing session")
  (or
    (some-> env :ring/request :session)
    {::auth/provider :local
     ::auth/status   :not-logged-in}))
