(ns gs-clj.core
  (:require [gs-clj.server :as server]
            [gs-clj.request :as request]
            [gs-clj.cli :as cli]
            [taoensso.timbre :as log])
  (:gen-class))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(def -log-levels [:trace
                  :debug
                  :info
                  :warn
                  :error
                  :fatal
                  :report])

(defn- log-level [{:keys [log-level verbose]}]
  (get -log-levels (if verbose
                     0
                     log-level)))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (cli/validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        ; general setup
        (log/set-min-level! (log-level options))
        (log/debug "Command line options: " options)
        ; handle action
        (case action
          "start"  (server/start! request/handle! options))))))
