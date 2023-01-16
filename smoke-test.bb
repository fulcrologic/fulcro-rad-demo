(ns smoke-test
  "Check that Demo is running, we can log in, and list accounts"
  (:require [babashka.wait :as wait]
            [babashka.process :as p]
            [cognitect.transit :as transit]
            [org.httpkit.client :as http]))

(def sigterm-exit-code "JVM exits with this when it gets SIGTERM from p/delete-tree" 143)
(def ok-exitcode? #{0 sigterm-exit-code})

(defn transit-str [data]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (let [writer (transit/writer out :json nil #_{:handlers nil})]
      (transit/write writer data)
      (.toString out "UTF-8"))))

(defn parse-transit-str [text] 
  (transit/read 
   (transit/reader (java.io.ByteArrayInputStream. (.getBytes text "UTF-8")) 
                   :json {:handlers nil})))

(defn try-slurp [f]
  (try (slurp f) 
       (catch Exception e
         (println "Warning: Failed to slurp" e)
         nil)))

(def db-deps-alias
  (doto (System/getenv "DB_DEPS_ALIAS")
    (assert "Missing env var DB_DEPS_ALIAS")))

;; I cannot get in extra config via the user deps.edn file in GH for reasons I
;; do not understand so let's set it inline
(def extra-clj-args
  (when (= db-deps-alias "datomic")
    "-Sdeps '{:mvn/repos {\"cognitect-dev-tools\" {:url \"https://dev-tools.cognitect.com/maven/releases/\"}}}'"))

(println "Starting the server for" db-deps-alias "...")
(def server-process (p/process {:shutdown p/destroy-tree
                                ;; TODO It seems the exit-fn is never called?!
                                :exit-fn (fn [{:keys [exit err out] :as resp}]
                                           (if (ok-exitcode? exit)
                                             (println "Server exited OK, out:"
                                                      (slurp out))
                                             (do
                                               (println "Server failed with code" exit
                                                        (slurp out) "\n" (slurp err))
                                               (System/exit exit))))} 
                               (doto (format "env JAVA_OPTS='-Dclojure.main.report=stderr' clojure %s -X:%s:dev development/cli-start" (or extra-clj-args "") db-deps-alias)
                                 println)))

(def success? (atom false))

(try

  (println "Waiting for the server to start...")
  (assert (not= (wait/wait-for-port "localhost" 3000 {:timeout 60000 :default ::timeout})
                ::timeout)
          "Timeout waiting for the server to start")
  (println "OK, server is running")

  (def res1
    @(http/post "http://localhost:3000/api"
                {:as :text
                 :headers {"Content-Type" "application/transit+json"}
                 :body (transit-str
                        '[{(com.example.model.account/login
                            {:username "tony@example.com", :password "letmein"}) [*]}])}))

  (assert (= 200 (:status res1)) (str "Expected 200, got " res1))
  (println "OK, managed to log in")

  (def res2
    (->
     @(http/post "http://localhost:3000/api"
                 {:as :text
                  :headers {"Content-Type" "application/transit+json"}
                  :body (transit-str '[{:account/all-accounts [:account/id :account/name]}])})
     (select-keys [:status :body])))

  (assert (= 200 (:status res2)) (str "Expected 200, got " res2))

  (let [resp-data (parse-transit-str (:body res2))]
    (assert (sequential? (:account/all-accounts resp-data))
            (str "Expected an accounts list, got " resp-data))
    ;; NOTE The list may be empty if seed!, which runs async, has not finished yet
    (println "OK, managed to list accounts:" (count (:account/all-accounts resp-data))))

  (reset! success? true)
  (finally
    (p/destroy-tree server-process)
    (let [{:keys [exit out err]} @server-process
          out (not-empty (try-slurp out)), err (not-empty (try-slurp err))
          problem? (or (not @success?) (not (ok-exitcode? exit)))]
      (when problem?
        (println "Server OUT:" out (when err (str "\nERR: " err)))))))