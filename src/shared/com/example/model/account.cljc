(ns com.example.model.account
  (:refer-clojure :exclude [name])
  (:require
    #?@(:clj
        [[com.wsscode.pathom.connect :as pc :refer [defmutation]]
         [com.example.components.database-queries :as queries]]
        :cljs
        [[com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]])
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [taoensso.timbre :as log]))

(defattr id ::id :uuid
  {::attr/identity?                                      true
   ;; NOTE: These are spelled out so we don't have to have either on classpath, which allows
   ;; independent experimentation. In a normal project you'd use ns aliasing.
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   :com.fulcrologic.rad.database-adapters.sql/schema     :production
   :com.fulcrologic.rad.database-adapters.sql/tables     #{"account"}
   ::auth/authority                                      :local})

(defattr email ::email :string
  {:com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}
   :db/unique                                                :db.unique/value
   ::attr/required?                                          true
   ::auth/authority                                          :local})

(defattr active? ::active? :boolean
  {::auth/authority                                          :local
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/column-name    "active"
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}
   ::form/default-value                                      true})

(defattr password ::password :password
  {;; TODO: context sense to allow for owner to write
   ::auth/authority                                          :local
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Permissions are typically only trusted at the server, but cases where we can
   ;; know permissions at the UI layer are also useful.
   ;;
   ;; The parser layer is certainly pluggable, so really any kind of read/write security
   ;; scheme could be enforced there (even the general saves have a well-known delta format
   ;; that could be checked before running the mutation).
   ;;
   ;; It probably makes sense to have some simple declarative CLJC version that can
   ;; give limited localized utility (i.e. knowing to hide a field for a user based on app state
   ;; in the client), but also the ability to declare things like a parser-level verification that
   ;; only happens on the server, a full-stack way for the client to ask what the permissions are, etc.
   ;; Each use-case can have a custom parameter if necessary. For example, if you want the client
   ;; to use a resolver property with parameters to query the server for the "current permissions" you could
   ;; certainly add such a system, have it pull that data on startup and put it in state, and then
   ;; examine that in the CLJS side of a lambda here.
   ;;
   ;; A mechanism that will work on the server for almost all cases is to include, in the `env`,
   ;; everything from the databases to the Ring request to the attribute name being checked. This
   ;; could then allow logic to figure out the permissions on an attribute for any circumstance.
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ::auth/permissions                                        (fn [env] #{})

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Things like this may or may not belong at the attr level.  Support for a
   ;; one-way hashed value is so common as to probably merit built-in support,
   ;; though the open maps make it possible to simply make this an extension point
   ;; for some kind of plugin.  The form augmentation of `::form/beforeWrite` shown
   ;; elsewhere is potentially a more appropriate generalization for this.
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ::attr/encrypt-iterations                                 100
   ::attr/required?                                          true})

(defattr name ::name :string
  {::auth/authority                                          :local
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{::id}
   :com.fulcrologic.rad.database-adapters.sql/schema         :production
   :com.fulcrologic.rad.database-adapters.sql/tables         #{"account"}
   ::attr/required?                                          true})

(defattr addresses ::addresses :ref
  {::attr/target                                                   :com.example.model.address/id
   ::attr/cardinality                                              :many
   :com.fulcrologic.rad.database-adapters.datomic/schema           :production
   :com.fulcrologic.rad.database-adapters.datomic/intended-targets #{:com.example.model.address/id}
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{::id}
   :com.fulcrologic.rad.database-adapters.sql/schema               :production
   :com.fulcrologic.rad.database-adapters.sql/tables               #{"account"}
   :db/isComponent                                                 true
   ::auth/authority                                                :local})

#_(defattr tags ::tags :ref
    {::attr/target                                                   :tag/id
     ::attr/cardinality                                              :many
     :com.fulcrologic.rad.database-adapters.datomic/schema           :production
     :com.fulcrologic.rad.database-adapters.datomic/intended-targets #{:com.example.model.tag/id}
     :com.fulcrologic.rad.database-adapters.datomic/entity-ids       #{::id}
     :com.fulcrologic.rad.database-adapters.sql/schema               :production
     :com.fulcrologic.rad.database-adapters.sql/tables               #{"account"}
     ::auth/authority                                                :local})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; If there is no database-specific representation of an attribute then one must
;; define the Pathom-specific mechanism for resolving it. We can (and may) hang write-level stuff
;; here, but Fulcro reified mutations are probably sufficient in many cases where virtual attributes
;; support any kind of "create/update".
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defattr all-accounts ::all-accounts :ref
  {::attr/target    ::id
   ::auth/authority :local
   ::pc/output      [{::all-accounts [::id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {::all-accounts (queries/get-all-accounts env query-params)}))})

#?(:clj
   (defmutation login [env {:keys [username password]}]
     {::pc/params #{:username :password}}
     (log/info "Attempt to login for " username)
     {::auth/provider  :local
      ::auth/real-user "Tony"})
   :cljs
   (defmutation login [params]
     (ok-action [{:keys [app result]}]
       (log/info "Login result" result)
       (auth/logged-in! app :local))
     (remote [env]
       (-> env
         (m/returning auth/Session)))))

(def attributes [id name email password active? addresses all-accounts])
