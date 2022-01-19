(ns com.example.workspaces.testing
  (:require [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.application :as app]))

(def sample-db
  {:people    [[:person/id 1] [:person/id 2]]
   :person/id {1 {:person/name "Bob"}
               2 {:person/name "Judy"}}})

(let [starting-node sample-db]
  (fdn/db->tree [{:people [:person/stuff]}] starting-node sample-db))
(let [starting-node sample-db]
  (fdn/db->tree [{:people [:person/name]}] starting-node sample-db))
(let [starting-entity {}]
  (fdn/db->tree [[:person/id 1]] starting-entity sample-db))

(defsc SampleComp [this props]
  {:ident (fn [] [:component/id ::SampleComp])})

(let [options (comp/component-options SampleComp)
      ident-fn (get options :ident)]
  (ident-fn SampleComp {}))

(defsc Address [this props]
  {:query [:address/id :address/street]
   :ident :address/id})

(defsc Person [this props]
  {:query         [:person/id :person/name {:person/address (comp/get-query Address)}]
   :ident         :person/id
   :initial-state (fn [params] {:person/id   (:id params)
                                :person/name (:name params)})})

(defsc Root [this props]
  {:query         [{:root/people (comp/get-query Person)}]
   :initial-state (fn [_] {:root/people [(comp/get-initial-state Person {:id 1 :name "Bob"})
                                         (comp/get-initial-state Person {:id 2 :name "Judy"})]})})

(def app (app/fulcro-app))

(app/mount! app Root :headless)

(app/current-state app)

(merge/merge-component! app Person {:person/id 3 :person/name "Sally" :person/address {:address/id 33 :address/street "111 Main St"}} :append [:root/people] :replace [:person/id 1 :person/spouse])
(merge/merge-component! app Person {:person/id 3 :person/name "Sally"})
(merge/merge-component! app Person {:person/id 2 :person/name "Jonathan"})