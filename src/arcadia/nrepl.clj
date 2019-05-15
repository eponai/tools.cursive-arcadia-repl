(ns arcadia.nrepl
  (:gen-class)
  (:require
    [clojure.tools.cli :as cli]
    [nrepl.server :as server]
    [nrepl.transport :as transport]
    [nrepl.middleware :refer [set-descriptor!]]
    [nrepl.core :as nrepl]))


(defn from-cursive?
  "Returns true if expr is loaded by Cursive."
  [expr]
  (and (string? expr)
       (re-find #"cursive.repl.runtime" expr)))


(def ^:dynamic *nrepl* nil)


(defn arcadia-nrepl-eval
  [handler {:keys [transport session] :as msg}]
  (let [code (or (:code msg) (:file msg))
        client (nrepl/client *nrepl* 1000)]
    (if (from-cursive? code)
      (handler msg)
      (let [id-id (:id msg)
            msg (select-keys msg [:id :op :code :file :file-name :file-path])
            res (nrepl/message client msg)

            session-id (if (instance? clojure.lang.AReference session)
                         (-> session meta :id)
                         session)]

        (doseq [r res]
          (transport/send transport (cond-> r
                                            (some? session-id)
                                            (assoc :session session-id)
                                            (some? id-id)
                                            (assoc :id id-id))))))))


(defn wrap-arcadia-repl
  [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      ("eval" "load-file") (arcadia-nrepl-eval handler msg)
      (handler msg))))


(set-descriptor! #'wrap-arcadia-repl
                 {:requires #{"clone"}
                  :expects #{"eval" "load-file"}
                  :handles {}})


(defn -main
  [& args]
  (let [cli-opts [["-p" "--port PORT" "Port number for nREPL server that Cursive should connect to."
                   :default 7888
                   :parse-fn #(Integer/parseInt %)
                   :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
                  ["-a" "--arcadia-port PORT" "Port number for Arcadia nREPL running in Unity."
                   :default 3722
                   :parse-fn #(Integer/parseInt %)
                   :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
        {:keys [arcadia-port port]} (:options (cli/parse-opts args cli-opts))]

    (print (format "Connecting to Arcadia on port %d... " arcadia-port))
    (with-open [conn (nrepl/connect :port arcadia-port)]
      (println "Done!")
      (binding [*nrepl* conn]
        (print (format "Starting nREPL server on port %d... " port))
        (server/start-server
          :port port
          :handler (server/default-handler #'wrap-arcadia-repl))
        (println "Done!")
        (loop [] (read-line) (recur))))))

