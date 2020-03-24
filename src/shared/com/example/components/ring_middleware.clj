(ns com.example.components.ring-middleware
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :as server]
    [com.fulcrologic.fulcro.networking.file-upload :as file-upload]
    [com.fulcrologic.rad.blob :as blob]
    [mount.core :refer [defstate]]
    [hiccup.page :refer [html5]]
    [ring.middleware.defaults :refer [wrap-defaults]]
    [com.example.components.config :as config]
    [com.example.components.parser :as parser]
    [taoensso.timbre :as log]
    [ring.util.response :as resp]
    [com.example.components.blob-store :as bs]))

(defn index [csrf-token]
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title "Application"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]

      [:link {:href "https://cdn.jsdelivr.net/npm/fomantic-ui@2.7.8/dist/semantic.min.css"
              :rel  "stylesheet"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body
      [:div#app]
      [:script {:src "js/main/main.js"}]]]))

(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      (server/handle-api-request (:transit-params request)
        (fn [query]
          (parser/parser {:ring/request request}
            query)))
      (handler request))))

(def not-found-handler
  (fn [req]
    {:status 404
     :body   {}}))

(defn wrap-html-routes [ring-handler]
  (fn [{:keys [uri anti-forgery-token] :as req}]
    (cond
      (#{"/" "/index.html"} uri)
      (-> (resp/response (index anti-forgery-token))
        (resp/content-type "text/html"))

      :else
      (ring-handler req))))

(defstate middleware
  :start
  (let [defaults-config (:ring.middleware/defaults-config config/config)]
    (-> not-found-handler
      (wrap-api "/api")
      (file-upload/wrap-mutation-file-uploads {})
      (blob/wrap-blob-service "/images" bs/image-blob-store)
      (blob/wrap-blob-service "/files" bs/file-blob-store)
      (server/wrap-transit-params {})
      (server/wrap-transit-response {})
      (wrap-html-routes)
      (wrap-defaults defaults-config))))

