(ns gs-clj.core
  (:require [gs-clj.server :as server]
            [gs-clj.request :as request]
            [gs-clj.cli :as cli]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [integrant.core :as ig])
  (:gen-class))

(def system (atom nil))

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

(defn- log-level
  [{:keys [level verbose]}]
  (get -log-levels (if verbose
                     0
                     level)))

(defmethod ig/init-key ::server [_ {:keys [port handler ssl-config public-path]}]
  (log/info (str "Starting server on port <" port ">"))
  (server/start! handler ssl-config {:port port
                                     :public-path public-path}))

(defmethod ig/halt-key! ::server [_ server]
  (.close server))

(defmethod ig/init-key ::logger [_ {:keys [level]}]
  (log/set-min-level! level)
  (log/debug "log-level" level))

(defn get-file-config
  [path]
  (edn/read (java.io.PushbackReader. (io/reader path))))

(defn merge-options
  "Merges the two configs together, cli-config should always override file-config"
  [file-config cli-config]
  {:port (or (:port cli-config) (get-in file-config [:server :port]))
   :ssl-config {:key (get-in file-config [:server :ssl-config :key])
                :cert (get-in file-config [:server :ssl-config :cert])}
   :public-path (get-in file-config [:server :public-path])
   :log-level (log-level
               {:level (or (:log-level cli-config)
                           (.indexOf -log-levels (get-in file-config [:logger :level])))
                :verbose (:verbose cli-config)})})

(defn start-server
  [{:keys [port ssl-config public-path log-level]}]
  (let [config {::server {:port port
                          :handler request/handle!
                          :ssl-config ssl-config
                          :public-path public-path
                          :logger (ig/ref ::logger)}
                ::logger {:level log-level}}]
    (reset! system (ig/init config))
    (log/info "gemini-server-clj is ready for connections")))

; (start-server {:port 1965
;                :ssl-config {:key "resources/certs/app.key"
;                             :cert "resources/certs/app.pem"}
;                :public-path "resources/public"
;                :log-level :debug})
; (ig/halt! @system)

(defn -main [& args]
  (let [{:keys [action exit-message ok?] :as cli-res} (cli/validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [file-options (get-file-config (get-in cli-res [:options :config]))
            user-options (merge-options file-options (:options cli-res))]
        (case action
          "start"  (start-server user-options))))))
