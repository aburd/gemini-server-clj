(ns gs-clj.core
  (:require [gs-clj.server :as server]
            [gs-clj.request :as request]
            [gs-clj.cli :as cli]
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
  [{:keys [log-level verbose]}]
  (get -log-levels (if verbose
                     0
                     log-level)))

(defmethod ig/init-key ::server [_ {:keys [port handler ssl-config public-path]}]
  (log/info (str "Starting server on port <" port ">"))
  (server/start! handler ssl-config {:port port
                                     :public-path public-path}))

(defmethod ig/halt-key! ::server [_ server]
  (.close server))

(defmethod ig/init-key ::logger [_ {:keys [level]}]
  (log/debug "log-level" level)
  (log/set-min-level! level))

(defn start-server
  [options]
  (let [config {::server {:port (:port options)
                          :handler request/handle!
                          :ssl-config {:key (or (:key options) "resources/certs/app.key")
                                       :cert (or (:cert options) "resources/certs/app.pem")}
                          :public-path "resources/public"
                          :logger (ig/ref ::logger)}
                ::logger {:level (log-level options)}}]
    (reset! system (ig/init config))
    (log/info "gemini-server-clj is ready for connections")))

; (start-server {:port 1965
;                :log-level 0
;                :ssl-config {:key "resources/certs/app.key"
;                             :cert "resources/certs/app.pem"}})

; (ig/halt! @system)

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (cli/validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start"  (start-server options)))))
