(ns com.example.ui
  (:require
    [com.example.ui.line-item-forms :refer [LineItemForm]]
    [com.example.ui.account-forms :refer [AccountForm AccountList]]
    [com.example.ui.item-forms :refer [ItemForm]]
    [com.example.ui.invoice-forms :refer [InvoiceForm]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]}
  (dom/div "Hello World"))

;; This will just be a normal router...but there can be many of them.
(defrouter MainRouter [this props]
  {:router-targets [LandingPage ItemForm InvoiceForm AccountList AccountForm]})

(def ui-main-router (comp/factory MainRouter))

(auth/defauthenticator Authenticator {:local LoginForm})

(def ui-authenticator (comp/factory Authenticator))

(defsc Root [this {::auth/keys [authorization]
                   :keys       [authenticator router]}]
  {:query         [{:authenticator (comp/get-query Authenticator)}
                   {:router (comp/get-query MainRouter)}
                   ::auth/authorization]
   :initial-state {:router        {}
                   :authenticator {}}}
  (let [logged-in? (= :success (some-> authorization :local ::auth/status))
        username   (some-> authorization :local :account/name)]
    (dom/div
      (div :.ui.top.menu
        (div :.ui.item "Demo")
        (when true #_logged-in?
          (comp/fragment
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this AccountForm (new-uuid 101)))} "My Account")
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this ItemForm (new-uuid 200)))} "Some Item")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this AccountForm))} "New Account")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this InvoiceForm))} "New Invoice")
            (dom/a :.ui.item {:onClick (fn [] (form/delete! this :com.example.model.account/id (new-uuid 102)))}
              "Delete account 2")
            (dom/a :.ui.item {:onClick (fn []
                                         (dr/change-route this (dr/path-to AccountList)))} "List Accounts")))
        (div :.right.menu
          (if logged-in?
            (comp/fragment
              (div :.ui.item
                (str "Logged in as " username))
              (div :.ui.item
                (dom/button :.ui.button {:onClick #(auth/logout! this :local)}
                  "Logout")))
            (div :.ui.item
              (dom/button :.ui.primary.button {:onClick #(auth/authenticate! this :local nil)}
                "Login")))))
      (div :.ui.container.segment
        (ui-authenticator authenticator)
        (ui-main-router router)))))

(def ui-root (comp/factory Root))
