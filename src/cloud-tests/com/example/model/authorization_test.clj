(ns com.example.model.authorization-test
  (:require
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.example.model.seed :as seed]
    [com.example.model :refer [all-attributes]]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    [com.example.model.authorization :as exauth]
    [datomic.client.api :as d]
    [fulcro-spec.core :refer [specification assertions component]]))

(specification "login!"
  (let [c   (datomic/empty-db-connection all-attributes :production)
        _   (d/transact c {:tx-data [(seed/new-account (new-uuid 1) "tony" "tony@example.com" "letmein"
                                                       :account/name "Tony")]})
        env (datomic/mock-resolver-env :production c)]
    (component "Valid credentials"
      (let [actual (exauth/login! env {:username "tony@example.com" :password "letmein"})]
        (assertions
          "Returns an auth success indicator"
          (::auth/status actual) => :success
          "Returns the auth provider's identity"
          (::auth/provider actual) => :local
          "Returns the account holder's first name"
          (:account/name actual) => "Tony")))
    (component "Bad username"
      (let [actual (exauth/login! env {:username "tny@eample.com" :password "crap"})]
        (assertions
          "Returns an auth failure indicator"
          (::auth/status actual) => :failed
          "Returns the auth provider's identity"
          (::auth/provider actual) => :local)))
    (component "Invalid credentials"
      (let [actual (exauth/login! env {:username "tony@example.com" :password "crap"})]
        (assertions
          "Returns an auth failure indicator"
          (::auth/status actual) => :failed
          "Returns the auth provider's identity"
          (::auth/provider actual) => :local)))))

