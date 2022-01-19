(ns com.example.components.parser
  (:require
   [com.example.components.auto-resolvers :refer [automatic-resolvers]]
   [com.example.components.blob-store :as bs]
   [com.example.components.config :refer [config]]
   [com.example.components.delete-middleware :as delete]
   [com.example.components.save-middleware :as save]
   [com.example.components.xtdb :refer [xtdb-nodes]]
   [com.example.model :refer [all-attributes]]
   [com.example.model.account :as account]
   [com.example.model.invoice :as invoice]
   [com.example.model.item :as item]
   [com.example.model.sales :as sales]
   [com.example.model.timezone :as timezone]
   [com.fulcrologic.rad.attributes :as attr]
   [com.fulcrologic.rad.blob :as blob]
   [com.wsscode.pathom.viz.ws-connector.core :as pvc]
   [com.wsscode.pathom.viz.ws-connector.pathom3 :as p.connector]
   [com.wsscode.pathom3.connect.runner :as pcr]
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.plugin :as p.plugin]
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [edn-query-language.core :as eql]
   [taoensso.timbre :as log]
   [com.fulcrologic.rad.pathom-common :as rpc]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.pathom3 :as pathom3]
   [mount.core :refer [defstate]]
   [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]))

(letfn [(wrap-mutate-exceptions [mutate]
          (fn [env ast]
            (try
              (mutate env ast)
              (catch Throwable e
                (log/errorf e "Mutation %s failed." (:key ast))
                ;; TASK: Need a bit more work on returning errors that are handled globally.
                ;; Probably should just propagate exceptions out, so the client sees a server error
                ;; Pathom 2 compatible message so UI can detect the problem
                {:com.wsscode.pathom.core/errors [{:message (ex-message e)
                                                   :data    (ex-data e)}]}))))]
  (p.plugin/defplugin rewrite-mutation-exceptions {::pcr/wrap-mutate wrap-mutate-exceptions})
  (defn new-processor
    "Create a new EQL processor. You may pass Pathom 2 resolvers or mutations to this function, but beware
     that the translation is not 100% perfect, since the `env` is different between the two versions.

     The config options go under :com.fulcrologic.rad.pathom/config, and include:

     - `:log-requests? boolean` Enable logging of incoming queries/mutations.
     - `:log-responses? boolean` Enable logging of parser results.
     - `:sensitive-keys` a set of keywords that should not have their values logged
     "
    [{{:keys [log-requests? log-responses?]} :com.fulcrologic.rad.pathom/config :as config} env-middleware extra-plugins resolvers]
    (let [base-env (-> {}
                       (p.plugin/register extra-plugins)
                       (p.plugin/register-plugin pathom3/attribute-error-plugin)
                       (p.plugin/register-plugin rewrite-mutation-exceptions)
                     ;(p.plugin/register-plugin log-resolver-error)
                       (pci/register (pathom3/convert-resolvers resolvers))
                       (assoc :config config)
                       (cond-> (:pathom.viz config)
                         (p.connector/connect-env {::pvc/parser-id `env})))
          process  (p.eql/boundary-interface base-env)]
      (fn [env tx]
        (when log-requests?
          (rpc/log-request! {:env env :tx tx}))
        (let [ast      (eql/query->ast tx)
              env      (assoc
                        (env-middleware env)
                         ;; for p2 compatibility
                        :parser p.eql/process
                         ;; legacy param support
                        :query-params (rpc/combined-query-params ast))
              response (process env {:pathom/ast           ast
                                     :pathom/lenient-mode? true})]
          (when log-responses? (rpc/log-response! env response))
          response)))))

(pco/defresolver index-explorer [env {:com.wsscode.pathom.viz.index-explorer/keys [id]}]
  #:com.wsscode.pathom.viz.index-explorer{:id id
                                          :index
                                          (-> env
                                              (update ::pci/index-resolvers #(into {} (map (fn [[k v]] [k (dissoc v :com.wsscode.pathom3.connect.operation.Resolver/resolve)])) %))
                                              (update ::pci/index-mutations #(into {} (map (fn [[k v]] [k (dissoc v :com.wsscode.pathom3.connect.operation.Mutation/mutate)])) %)))})

(defstate parser
  :start
  (let [env-middleware (-> (attr/wrap-env all-attributes)
                           (form/wrap-env save/middleware delete/middleware)
                           (xtdb/wrap-env (fn [_] {:production (:main xtdb-nodes)}))
                           (blob/wrap-env bs/temporary-blob-store {:files         bs/file-blob-store
                                                                   :avatar-images bs/image-blob-store}))
        resolvers (cond-> [automatic-resolvers
                           form/resolvers
                           (blob/resolvers all-attributes)
                           account/resolvers
                           invoice/resolvers
                           item/resolvers
                           sales/resolvers
                           timezone/resolvers]
                    (:pathom.viz config) (conj index-explorer))]
    (new-processor config env-middleware [] resolvers)))

