(ns com.example.ui.notes
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [com.example.model :as model]
    [com.example.model.company :as company]
    [com.example.model.entity :as entity]
    [com.example.model.person :as person]
    [com.example.model.note :as note]
    [com.example.model.timezone :as timezone]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.example.ui.file-forms :refer [FileForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.attributes :as attr]))

(form/defsc-form PersonForm [this props]
  {fo/id           person/id
   fo/attributes   [person/first-name
                    person/last-name
                    entity/email]
   fo/route-prefix "person"
   fo/title        "Person"})

(def ui-person-form (comp/computed-factory PersonForm {:keyfn :person/id}))

(form/defsc-form CompanyForm [this props]
  {fo/id           company/id
   fo/attributes   [company/classification
                    entity/email
                    entity/name]
   fo/route-prefix "company"
   fo/title        "Company"})

(def ui-company-form (comp/computed-factory CompanyForm {:keyfn :company/id}))

(defsc PartyUnion [this props cprops]
  {:ident (fn []
            (cond
              (:person/id props) [:person/id (:person/id props)]
              (:company/id props) [:company/id (:company/id props)]))
   :query (fn [] {:person/id  (comp/get-query PersonForm)
                  :company/id (comp/get-query CompanyForm)})}
  (cond
    (:person/id props) (ui-person-form props cprops)
    (:company/id props) (ui-company-form props cprops)))

(form/defsc-form NoteForm [this props]
  {fo/id             note/id
   fo/attributes     [note/content
                      note/parties]
   fo/default-values {:account/active?         true
                      :account/primary-address {}
                      :account/addresses       [{}]}
   fo/route-prefix   "note"
   fo/title          "Edit Note"
   fo/subforms       {:note/parties {fo/ui          PartyUnion
                                     fo/can-delete? true
                                     fo/title       "Interested Parties"}}})

(comment
  (comp/get-ident CompanyForm {:company/id 2}))

(report/defsc-report NoteList [this props]
  {ro/title            "All Notes"
   ro/columns          [note/content note/parties]
   ro/row-pk           note/id
   ro/source-attribute :note/all-notes
   ro/run-on-mount?    true
   ro/form-links       {:note/content NoteForm}
   #_#_ro/controls {::new-note {:type   :button
                                :local? true
                                :label  "New Note"
                                :action (fn [this _] (form/create! this AccountForm))}}

   ro/control-layout   {:action-buttons [::new-note]}
   ro/route            "notes"})

(comment
  (comp/get-query NoteList))
