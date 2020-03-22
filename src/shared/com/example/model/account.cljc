(ns com.example.model.account
  (:refer-clojure :exclude [name])
  (:require
    #?@(:clj
        [[com.wsscode.pathom.connect :as pc :refer [defmutation]]
         [com.example.model.authorization :as exauth]
         [com.example.components.database-queries :as queries]]
        :cljs
        [[com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]])
    [clojure.string :as str]
    [com.example.model.timezone :as timezone]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.middleware.save-middleware :as save-middleware]
    [com.fulcrologic.rad.blob :as blob]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.type-support.date-time :as datetime]))

(defattr id :account/id :uuid
  {::attr/identity?                                      true
   ;; NOTE: These are spelled out so we don't have to have either on classpath, which allows
   ;; independent experimentation. In a normal project you'd use ns aliasing.
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   :com.fulcrologic.rad.database-adapters.sql/schema     :production
   :com.fulcrologic.rad.database-adapters.sql/table      "account"
   ::auth/authority                                      :local})

(defattr email :account/email :string
  {:com.fulcrologic.rad.database-adapters.datomic/schema           :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{:account/id}
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/unique :db.unique/value}
   :com.fulcrologic.rad.database-adapters.sql/schema               :production
   ::attr/identities                                               #{:account/id}
   ::attr/required?                                                true
   ::auth/authority                                                :local})


(defattr active? :account/active? :boolean
  {::auth/authority                                          :local
   ::attr/identities                                         #{:account/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/column-name    "active"
   ::form/default-value                                      true})

(defattr password :password/hashed-value :string
  {::attr/required?                                          true
   ::attr/identities                                         #{:account/id}
   ::auth/authority                                          :local
   :com.fulcrologic.rad.database-adapters.sql/column-name    "password"
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}})

(defattr password-salt :password/salt :string
  {::auth/authority                                          :local
   ::attr/identities                                         #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/column-name    "password_salt"
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   ::attr/required?                                          true})

(defattr password-iterations :password/iterations :int
  {::auth/authority                                          :local
   ::attr/identities                                         #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/column-name    "password_iterations"
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   ::attr/required?                                          true})

(def account-roles {:account.role/superuser "Superuser"
                    :account.role/user      "Normal User"})

(defattr role :account/role :enum
  {::auth/authority                                          :local
   ::attr/identities                                         #{:account/id}
   ::attr/enumerated-values                                  (set (keys account-roles))
   ::attr/enumerated-labels                                  account-roles
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}})

(defattr name :account/name :string
  {::auth/authority                                          :local
   ::form/field-label                                        "Name"
   ::attr/identities                                         #{:account/id}
   ;::attr/valid?                                             (fn [v] (str/starts-with? v "Bruce"))
   ;::attr/validation-message                                 (fn [v] "Your name's not Bruce then??? How 'bout we just call you Bruce?")
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   ::attr/required?                                          true})

(defattr primary-address :account/primary-address :ref
  {::attr/target                                                   :address/id
   ::attr/cardinality                                              :one
   ::attr/identities                                               #{:account/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema           :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{:account/id}
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}
   :com.fulcrologic.rad.database-adapters.sql/schema               :production
   ::auth/authority                                                :local})

;; NOTE: How to do file SHA->URL stuff...
#_(pc/defresolver image-resolver [env input]
    {::pc/input  #{:file/sha ::blob/store}
     ::pc/output [:file/url]})

;; NOTE: Not quite done yet...
(defattr avatar :account/avatar :string
  {
   ;; The field style give you a specific control, and the blob settings
   ;; are used by middleware to target a particular store (you must config).
   ::form/field-style                                        ::blob/file-upload
   ::blob/accept-file-types                                  "image/*"
   ::blob/store                                              :avatar-images
   ::blob/remote                                             :remote

   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}
   ::auth/authority                                          :local})

(defattr files :account/files :ref
  {::attr/target                                             :file/id
   ::attr/cardinality                                        :many
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}})

(defattr addresses :account/addresses :ref
  {::attr/target                                             :address/id
   ::attr/cardinality                                        :many
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}
   :db/isComponent                                           true
   ::auth/authority                                          :local})

(defattr all-accounts :account/all-accounts :ref
  {::attr/target    :account/id
   ::auth/authority :local
   ::pc/output      [{:account/all-accounts [:account/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:account/all-accounts (queries/get-all-accounts env query-params)}))})

#?(:clj
   (defmutation login [env params]
     {::pc/params #{:username :password}}
     (exauth/login! env params))
   :cljs
   (defmutation login [params]
     (ok-action [{:keys [app state]}]
       (let [{:time-zone/keys [zone-id]
              ::auth/keys     [status]} (some-> state deref ::auth/authorization :local)]
         (if (= status :success)
           (do
             (when zone-id
               (log/info "Setting UI time zone" zone-id)
               (datetime/set-timezone! zone-id))
             (auth/logged-in! app :local))
           (auth/failed! app :local))))
     (error-action [{:keys [app]}]
       (log/error "Login failed.")
       (auth/failed! app :local))
     (remote [env]
       (m/returning env auth/Session))))

#?(:clj
   (defmutation check-session [env _]
     {}
     (exauth/check-session! env))
   :cljs
   (defmutation check-session [_]
     (ok-action [{:keys [state app result]}]
       (let [{::auth/keys [provider]} (get-in result [:body `check-session])
             {:time-zone/keys [zone-id]
              ::auth/keys     [status]} (some-> state deref ::auth/authorization (get provider))]
         (when (= status :success)
           (when zone-id
             (log/info "Setting UI time zone" zone-id)
             #_(datetime/set-timezone! time-zone)))
         (uism/trigger! app auth/machine-id :event/session-checked {:provider provider})))
     (remote [env]
       (m/returning env auth/Session))))

#?(:clj
   (defmethod save-middleware/rewrite-value :account/id
     [env [_ id] {:account/keys [avatar-url] :as value}]
     (let [{:keys [before after]} avatar-url]
       value)))

(def attributes [id name primary-address role email password password-iterations password-salt active?
                 addresses all-accounts avatar files])

(def resolvers [login check-session])
