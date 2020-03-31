(ns com.example.ui
  (:require
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.example.ui.line-item-forms :refer [LineItemForm]]
    [com.example.ui.account-forms :refer [AccountForm AccountList]]
    [com.example.ui.item-forms :refer [ItemForm]]
    [com.example.ui.invoice-forms :refer [InvoiceForm InvoiceList AccountInvoices]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.rad.routing :as rroute]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]}
  (dom/div "Hello World"))

;; This will just be a normal router...but there can be many of them.
(defrouter MainRouter [this {:keys [route-factory route-props] :as props}]
  {:router-targets [LandingPage ItemForm InvoiceForm InvoiceList AccountList AccountForm AccountInvoices]}
  ;; Normal Fulcro code to show a loader on slow route change (assuming Semantic UI here, should
  ;; be generalized for RAD so UI-specific code isn't necessary)
  (dom/div
    (dom/div :.ui.active.loader)
    (when route-factory
      (route-factory route-props))))

(def ui-main-router (comp/factory MainRouter))

(auth/defauthenticator Authenticator {:local LoginForm})

(def ui-authenticator (comp/factory Authenticator))

(defsc Root [this {::auth/keys [authorization]
                   ::app/keys  [active-remotes]
                   :keys       [authenticator router]}]
  {:query         [{:authenticator (comp/get-query Authenticator)}
                   {:router (comp/get-query MainRouter)}
                   ::app/active-remotes
                   ::auth/authorization]
   :initial-state {:router        {}
                   :authenticator {}}}
  (let [logged-in? (= :success (some-> authorization :local ::auth/status))
        busy?      (seq active-remotes)
        username   (some-> authorization :local :account/name)]
    (dom/div
      (div :.ui.top.menu
        (div :.ui.item "Demo")
        (when logged-in?
          (comp/fragment
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this AccountForm (new-uuid 101)))} "My Account")
            (dom/a :.ui.item {:onClick (fn [] (rroute/route-to! this AccountInvoices {:account/id (new-uuid 101)}))} "My Invoices")
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this ItemForm (new-uuid 200)))} "Some Item")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this AccountForm))} "New Account")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this InvoiceForm))} "New Invoice")
            (dom/a :.ui.item {:onClick (fn []
                                         (rroute/route-to! this InvoiceList {}))} "List Invoices")
            (dom/a :.ui.item {:onClick (fn []
                                         (rroute/route-to! this AccountList {}))} "List Accounts")))
        (div :.right.menu
          (div :.item
            (div :.ui.tiny.loader {:classes [(when busy? "active")]})
            ent/nbsp ent/nbsp ent/nbsp ent/nbsp)
          (if logged-in?
            (comp/fragment
              (div :.ui.item
                (str "Logged in as " username))
              (div :.ui.item
                (dom/button :.ui.button {:onClick (fn []
                                                    ;; TODO: check if we can change routes...
                                                    (rroute/route-to! this LandingPage {})
                                                    (auth/logout! this :local))}
                  "Logout")))
            (div :.ui.item
              (dom/button :.ui.primary.button {:onClick #(auth/authenticate! this :local nil)}
                "Login")))))
      (div :.ui.container.segment
        (ui-authenticator authenticator)
        (ui-main-router router)))))

(def ui-root (comp/factory Root))

