(ns gs-clj.core
  (:require [gs-clj.server :as server]
            [gs-clj.gemini :as gemini])
  (:gen-class))

; TODO: parse options
(defn -main []
  (println (str "Starting server on port '" gemini/default-port "'"))
  (let [s (server/start)]
    (println "Server listening...")))
