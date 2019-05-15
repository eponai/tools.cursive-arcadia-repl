(ns eponai.tools.cursive-arcadia-repl
  (:gen-class)
  (:require
    [clojure.tools.cli :as cli]
    [nrepl.server :as server]
    [nrepl.transport :as transport]
    [nrepl.middleware :refer [set-descriptor!]]
    [nrepl.core :as nrepl]))


(def ^:dynamic *nrepl-conn* "Transport for connection to Arcadia's nREPL." nil)


(def cli-opts
  [["-p" "--port PORT" "Port number for nREPL server that Cursive should connect to."
    :default 7888
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-a" "--arcadia-port PORT" "Port number for Arcadia nREPL running in Unity."
    :default 3722
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])


(defn from-cursive?
  "Returns true if expr is sent by Cursive."
  [expr]
  (and (string? expr)
    ;; TODO Handle as input? (e.g. file or cli)
    (re-find #"cursive.repl.runtime" expr)))


(defn arcadia-nrepl-eval
  [handler {:keys [transport session id] :as msg}]
  (let [code   (or (:code msg) (:file msg))
        client (nrepl/client *nrepl-conn* 1000)]
    (if (from-cursive? code)
      (handler msg)
      ;; TODO Figure out which keys should be selected.
      (let [message    (select-keys msg [:id :op :code :file :file-name :file-path])
            responses  (nrepl/message client message)
            session-id (if (instance? clojure.lang.AReference session)
                         (-> session meta :id)
                         session)]

        (doseq [response responses]
          (transport/send transport
            (cond-> response
              (some? session-id)
              (assoc :session session-id)
              (some? id)
              (assoc :id id))))))))


(defn wrap-arcadia-repl
  [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      ("eval" "load-file") (arcadia-nrepl-eval handler msg)
      (handler msg))))


(set-descriptor! #'wrap-arcadia-repl
  {:requires #{"clone"}
   :expects  #{"eval" "load-file"}
   :handles  {}})


(defn -main
  [& args]
  (let [{:keys [arcadia-port port]} (:options (cli/parse-opts args cli-opts))]
    (print (format "Connecting to Arcadia on port %d... " arcadia-port))
    (with-open [conn (nrepl/connect :port arcadia-port)]
      (println "Done!")
      (binding [*nrepl-conn* conn]
        (print (format "Starting nREPL server on port %d... " port))
        (server/start-server
          :port port
          :handler (server/default-handler #'wrap-arcadia-repl))
        (println "Done!")
        (loop [] (read-line) (recur))))))

