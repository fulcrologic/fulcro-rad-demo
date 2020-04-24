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
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.middleware.save-middleware :as save-middleware]
    [com.fulcrologic.rad.blob :as blob]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.type-support.date-time :as datetime]))

(defattr id :account/id :uuid
  {ao/identity?                                     true
   ;; NOTE: These are spelled out so we don't have to have either on classpath, which allows
   ;; independent experimentation. In a normal project you'd use ns aliasing.
   ao/schema                                        :production
   :com.fulcrologic.rad.database-adapters.sql/table "account"})

(defattr email :account/email :string
  {ao/identities                                                   #{:account/id}
   ao/required?                                                    true
   ao/schema                                                       :production
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/unique :db.unique/value}
   })


(defattr active? :account/active? :boolean
  {ao/identities                                          #{:account/id}
   ao/schema                                              :production
   :com.fulcrologic.rad.database-adapters.sql/column-name "active"
   fo/default-value                                       true})

(defattr password :password/hashed-value :string
  {ao/required?                                           true
   ao/identities                                          #{:account/id}
   ::auth/permissions                                     (fn [_] #{})
   :com.fulcrologic.rad.database-adapters.sql/column-name "password"
   ao/schema                                              :production})

(defattr password-salt :password/salt :string
  {:com.fulcrologic.rad.database-adapters.sql/column-name "password_salt"
   ::auth/permissions                                     (fn [_] #{})
   ao/schema                                              :production
   ao/identities                                          #{:account/id}
   ao/required?                                           true})

(defattr password-iterations :password/iterations :int
  {ao/identities                                          #{:account/id}
   ::auth/permissions                                     (fn [_] #{})
   :com.fulcrologic.rad.database-adapters.sql/column-name "password_iterations"
   ao/schema                                              :production
   ao/required?                                           true})

(def account-roles {:account.role/superuser "Superuser"
                    :account.role/user      "Normal User"})

(defattr role :account/role :enum
  {ao/identities        #{:account/id}
   ao/enumerated-values (set (keys account-roles))
   ao/enumerated-labels account-roles
   ao/schema            :production})

(defattr name :account/name :string
  {fo/field-label "Name"
   ;::report/field-formatter (fn [v] (str "ATTR" v))
   ao/identities  #{:account/id}
   ;ao/valid?      (fn [v] (str/starts-with? v "Bruce"))
   ;::attr/validation-message                                 (fn [v] "Your name's not Bruce then??? How 'bout we just call you Bruce?")
   ao/schema      :production

   ao/required?   true})

(defattr primary-address :account/primary-address :ref
  {ao/target                                                       :address/id
   ao/cardinality                                                  :one
   ao/identities                                                   #{:account/id}
   :com.fulcrologic.rad.database-adapters.sql/delete-referent?     true
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}
   ao/schema                                                       :production})

;; NOTE: How to do file SHA->URL stuff...
#_(pc/defresolver image-resolver [env input]
    {::pc/input  #{:file/sha ::blob/store}
     ::pc/output [:file/url]})

;; NOTE: Not quite done yet...
(defattr avatar :account/avatar :string
  {
   ;; The field style give you a specific control, and the blob settings
   ;; are used by middleware to target a particular store (you must config).
   fo/field-style           ::blob/file-upload
   ::blob/accept-file-types "image/*"
   ::blob/store             :avatar-images
   ::blob/remote            :remote

   ao/identities            #{:account/id}
   ao/schema                :production})

(defattr files :account/files :ref
  {ao/target      :file/id
   ao/cardinality :many
   ao/identities  #{:account/id}
   ao/schema      :production})

(defattr addresses :account/addresses :ref
  {ao/target                                                       :address/id
   ao/cardinality                                                  :many
   ao/identities                                                   #{:account/id}
   ao/schema                                                       :production
   :com.fulcrologic.rad.database-adapters.sql/delete-referent?     true
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}})

(defattr all-accounts :account/all-accounts :ref
  {ao/target     :account/id
   ao/pc-output  [{:account/all-accounts [:account/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:account/all-accounts (queries/get-all-accounts env query-params)}))})

(defattr account-invoices :account/invoices :ref
  {ao/target     :account/id
   ao/pc-output  [{:account/invoices [:invoice/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:account/invoices (queries/get-customer-invoices env query-params)}))})

#?(:clj
   (defmutation logout [env _]
     {}
     (exauth/logout! env))
   :cljs
   (defmutation logout [_]
     (remote [env]
       true)))

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

(declare disable-account)

#?(:clj
   (defmutation set-account-active [env {:account/keys [id active?]}]
     {::pc/params #{:account/id}
      ::pc/output [:account/id]}
     (form/save-form* env {::form/id        id
                           ::form/master-pk :account/id
                           ::form/delta     {[:account/id id] {:account/active? {:before (not active?) :after (boolean active?)}}}}))
   :cljs
   (defmutation set-account-active [{:account/keys [id active?]}]
     (action [{:keys [state]}]
       (swap! state assoc-in [:account/id id :account/active?] active?))
     (remote [_] true)))

(def attributes [id name primary-address role email password password-iterations password-salt active?
                 addresses all-accounts avatar files account-invoices])

#?(:clj
   (def resolvers [login logout check-session set-account-active]))
