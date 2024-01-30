(ns gs-clj.server
  (:require [aleph.tcp :as tcp]
            [aleph.netty :as netty]
            [gloss.io :as io]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.java.io :as java-io]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [gs-clj.gemini :as gemini]
            [gs-clj.headers :as headers]))

; TODO: Add TLS
(def ssl-context
  (netty/ssl-client-context
   {:private-key (java-io/file (java-io/resource "app.key"))
    :certificate-chain (java-io/file (java-io/resource "app.pem"))}))

(defn wrap-server-stream
  "wrap the server connection stream with gloss protocol to handle 
  sending/receiving bytes"
  [req-protocol res-protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode req-protocol %) out)
     s)

    (s/splice
     out
     (io/decode-stream s res-protocol))))

; TODO: Is there a specification for timeouts?
(def timeout-ms 10000)

; TODO: Check uri better
(defn gemini-uri? [uri]
  (string/starts-with? uri "gemini://"))

(defn wrap-tcp-stream
  [gemini-req-handler-fn]
  (fn [s info]
    (log/debug "Connection established: " info)

    (d/timeout!
      ;; take a message, and define a default value that tells us if the connection is closed
     (-> (s/take! s ::none)

         (d/chain

          ;; first, check if there even was a message, continue processing
          (fn [msg]
            (log/debug "msg" msg)
            (if (= ::none msg)
              (throw (new Exception "no request"))
              msg))

          ;; check that the request is valid
          (fn [maybe-uri]
            (log/debug "uri?" maybe-uri)
            (if (gemini-uri? maybe-uri)
              (throw (new Exception (str "invalid gemini uri: " maybe-uri)))
              maybe-uri))

          ;; once we have a valid uri; process the uri on another thread
          (fn [uri]
            (log/debug "decoded uri: " uri)
            (d/future (gemini-req-handler-fn uri)))

          ;; once we generate a response, write it back to the client
          (fn [args]
            (log/info args))

          ; (fn [{:keys [header body success?]}]
          ;   (log/debug (str "gemini response: " header))
          ;   (s/put! s header)
          ;   (when (true? success?)
          ;     (s/put! s body)))

           ;; regardless, close the connection
          (fn []
            (s/close! s)))

          ;; if there were any issues on the far end, send a stringified exception back
          ;; and close the connection
         (d/catch
          (fn [ex]
            (log/error ex)
            ; TODO: Give better error messages
            (s/put! s (headers/permanent-failure "An unknown error occurred"))
            (s/close! s))))
     timeout-ms)))

(defn gemini-req-handler
  "Handles a gemini requests"
  [^String uri]
  (log/debug "Got a URI! " uri)
  {:header (headers/success "text/gemini")
   :body "Hello there"
   :success? true})

(defn start-server
  "Starts the server and applies the tcp-stream to the stream-handler.
  port - server port"
  [stream-handler & {:keys [port] :or {port gemini/default-port}}]
  (tcp/start-server
   (fn [s info]
     (stream-handler
      (wrap-server-stream gemini/request-protocol gemini/response-protocol s)
      info))
   {:port port}))

(defn start
  "Starts the server with the gemini request handler fn. wrap-tcp-stream will stream valid uris to the handler."
  [& opts]
  (start-server (wrap-tcp-stream gemini-req-handler) opts))
