(ns gs-clj.client
  (:require [aleph.tcp :as tcp]
            [gloss.core :as gloss]
            [gloss.io :as io]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [gs-clj.gemini :as gemini]))

(def protocol
  (gloss/compile-frame
   (gloss/string :utf-8)))

; URL test

; (def buffer (java.nio.ByteBuffer/wrap (.getBytes "gemini://example.net/search?hello\r\n")))
; (try
;   (io/decode req-protocol buffer)
;   (catch Exception e
;     (println "Error!" e)))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)
    (s/splice
     out
     (io/decode-stream s protocol))))

(defn client
  "Create a basic gemini client"
  [& {:keys [host port] :or {host "localhost" port gemini/default-port}}]
  (d/chain (tcp/client {:port port :host host})
           #(wrap-duplex-stream protocol %)))

; (def c (client))
; (s/put! c (str "gemini://hey.gov" gemini/clrf))
