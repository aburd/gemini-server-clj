(ns gs-clj.core
  (:require [aleph.tcp :as tcp]
            [aleph.netty :as netty])
  (:gen-class))

(defn echo-handler [s info]
  (println (take 10 s) info))

(defn start-server
  "start the gemini server"
  [& {:keys [port] :or {port 1965}}]
  (println (str "Starting server on port: " port))
  (tcp/start-server echo-handler {:port port}))

(defn stop-server
  "stop the gemini server"
  [s]
  (println "Stopping the server on port: " (netty/port s))
  (.close s))

(defn -main []
  (start-server))
