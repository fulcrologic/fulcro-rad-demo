(ns com.example.ui.util.toast
  #?(:cljs
     (:require
       ["react-toastify" :refer [ToastContainer toast]]
       [com.fulcrologic.fulcro.dom :as dom])))

(defn ui-toast-container
  "Embed the toast container. Must be placed somewhere near the root where it will always be rendered.

   props are as described in https://fkhadra.github.io/react-toastify/introduction

   * :position - one of top-right, top-left, top-center, bottom-*
   * :autoClose - ms until closing (5000)
   * :hideProgressBar - Default false
   * :newestOnTop - Default false
   * :closeOnClick - Default true
   * :rtl - Default false
   * :pauseOnFocusLoss - Default true
   * :draggable - Default true
   * :pauseOnHover - Default true

   These can be overridden in the trigger function `toast!`.
   "
  ([props]
   #?(:cljs (dom/create-element ToastContainer (clj->js props))))
  ([]
   #?(:cljs (dom/create-element ToastContainer))))

(defn toast!
  "Open a toast in the given toast container. Default options are specified on that container.

   props can override the options (described at https://fkhadra.github.io/react-toastify/introduction)

   * :position - one of top-right, top-left, top-center, bottom-*
   * :autoClose - ms until closing
   * :hideProgressBar
   * :newestOnTop
   * :closeOnClick
   * :rtl
   * :pauseOnFocusLoss
   * :draggable
   * :pauseOnHover
  "
  ([props message]
   #?(:cljs (toast message (clj->js props))))
  ([message]
   #?(:cljs (toast message))))
