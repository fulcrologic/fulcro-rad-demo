(ns com.example.ui.notes
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
       :cljs [com.fulcrologic.fulcro.dom :as dom])
    [com.example.model :as model]
    [com.example.model.company :as company]
    [com.example.model.entity :as entity]
    [com.example.model.person :as person]
    [com.example.model.note :as note]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]))

(form/defsc-form PersonForm [this props]
  {fo/id           person/id
   fo/attributes   [person/first-name
                    person/last-name
                    entity/email]
   fo/add-label    (fn [_ add!] (dom/button :.ui.basic.icon.button {:onClick add!}
                                  (dom/i :.plus.icon)
                                  "Add Person"))
   fo/route-prefix "person"
   fo/title        "Person"})

(def ui-person-form (comp/computed-factory PersonForm {:keyfn :person/id}))

(form/defsc-form CompanyForm [this props]
  {fo/id           company/id
   fo/attributes   [company/classification
                    entity/email
                    entity/name]
   fo/add-label    (fn [_ add!] (dom/button :.ui.basic.icon.button {:onClick add!}
                                  (dom/i :.plus.icon)
                                  "Add Company"))
   fo/route-prefix "company"
   fo/title        "Company"})

(def ui-company-form (comp/computed-factory CompanyForm {:keyfn :company/id}))

(form/defunion PartyUnion PersonForm CompanyForm)

(form/defsc-form NoteForm [this props]
  {fo/id             note/id
   fo/validator      model/all-attribute-validator
   fo/attributes     [note/author
                      note/content
                      note/parties]
   fo/default-values {:account/active?         true
                      :account/primary-address {}
                      :account/addresses       [{}]}
   fo/route-prefix   "note"
   fo/title          "Edit Note"
   fo/subforms       {:note/author  {fo/ui    PartyUnion
                                     fo/title "Author"}
                      :note/parties {fo/ui             PartyUnion
                                     fo/can-delete?    true
                                     fo/default-values [(fn [id] {:person/id id})
                                                        (fn [id] {:company/id id})]
                                     fo/title          "Interested Parties"}}})

(comment
  (comp/get-ident CompanyForm {:company/id 2}))

(report/defsc-report NoteList [this props]
  {ro/title            "All Notes"
   ro/columns          [note/content note/parties]
   ro/row-pk           note/id
   ro/source-attribute :note/all-notes
   ro/run-on-mount?    true
   ro/form-links       {:note/content NoteForm}
   ro/controls         {:refresh   {:local? true
                                    :label  "Reload"
                                    :action (fn [this] (control/run! this))}
                        ::new-note {:type   :button
                                    :local? true
                                    :label  "New Note"
                                    :action (fn [this _] (form/create! this NoteForm))}}

   ro/control-layout   {:action-buttons [::new-note :refresh]}
   ro/route            "notes"})

(comment
  (comp/get-query NoteList))
