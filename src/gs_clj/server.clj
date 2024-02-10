(ns gs-clj.server
  (:import [java.util.concurrent TimeoutException])
  (:require [aleph.tcp :as tcp]
            [aleph.netty :as netty]
            [gloss.io :as io]
            [gloss.core :as gloss]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.java.io :as java-io]
            [taoensso.timbre :as log]
            [gs-clj.gemini :as gemini]
            [gs-clj.headers :as headers]
            [gs-clj.request :as request]
            [gs-clj.utils :refer [emphasize]]))

(def protocol
  (gloss/compile-frame
   (gloss/string :utf-8)))

(defn- wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)
    (s/splice
     out
     (io/decode-stream s protocol))))

(def timeout-ms 10000)

(defn- handle-tcp-error!
  [err stream msg]
  (do
    (log/error err)
    (s/put! stream (headers/permanent-failure msg))
    (s/close! stream)))

(defn wrap-tcp-stream
  "Returns a tcp-stream handler. Takes a `gemini-req-handler` which the stream handler will pass
  gemini requests to."
  [gemini-req-handler]
  (fn [s info]
    (log/debug (emphasize "start"))
    (log/debug (str "Connection established with [" (:remote-addr info)) "]")

    ; Will build up a uri from the tcp-stream
    ; Once the uri is determined to valid, we hand it off to the gemini-req-handler
    (d/loop [uri ""]
      ;; take a message, and define a default value that tells us if the connection is closed
      (-> (s/take! s ::none)

          (d/chain

           ;; first, check if there even was a message
           (fn [msg]
             (if (= ::none msg)
               ::none
               (str uri msg)))

           ;; check that the request is valid
           ;; if so, process on another thread
           (fn [maybe-uri]
             (log/debug "maybe-uri:" maybe-uri)
             (let [gemini-req (request/from-str maybe-uri)]
               (log/debug "gemini-req:" gemini-req)
               (d/timeout!
                (d/future {:maybe-uri maybe-uri
                           :gemini-res (if-let [r (request/from-str maybe-uri)]
                                         (gemini-req-handler r))})
                timeout-ms)))

           ;; if there was a success result, we write it to the stream
           (fn [{:keys [maybe-uri gemini-res]}]
             (log/debug "gemini-res:" gemini-res)
             {:maybe-uri maybe-uri
              :result (when-not (nil? gemini-res)
                        (log/debug "Successfully handled request")
                        (let [{:keys [header body]} gemini-res]
                          (log/debug {:header header :body body})
                          @(s/put! s header)
                          (when-not (nil? body)
                            @(s/put! s body))))})

           ;; close the connection on success
           (fn [{:keys [maybe-uri result]}]
             (log/debug {:maybe-uri maybe-uri :result result})
             (when result
               (do
                 (log/debug (emphasize "end"))
                 (s/close! s)))
             (when (and (not result) (not= maybe-uri ::none))
               (d/recur maybe-uri))))

            ;; if there were any issues on the far end, send a stringified exception back
            ;; and close the connection
          (d/catch
           TimeoutException
           #(handle-tcp-error! % s "The request has timed out"))
          (d/catch
           #(handle-tcp-error! % s "An unknown error occurred"))))))

(defn- server->ssl-context
  [key cert]
  (netty/ssl-server-context
   {:private-key (java-io/file key)
    :certificate-chain (java-io/file cert)}))

(defn- start-tcp-server!
  "Starts the server and applies the tcp-stream to the stream-handler.
  port - server port"
  [tcp-stream-handler & {:keys [port ssl-context]
                         :or {port gemini/default-port
                              ssl-context (server->ssl-context
                                           (java-io/resource "app.key")
                                           (java-io/resource "app.pem"))}}]
  (log/debug (str "Starting server on port <" port ">"))
  (tcp/start-server
   (fn [s info]
     (tcp-stream-handler
      (wrap-duplex-stream protocol s)
      info))
   {:port port
    :ssl-context ssl-context}))

(defn start!
  "Starts the server with the gemini request handler fn. wrap-tcp-stream will stream valid uris to the handler.

   === Options ===
   * port - the port to listen at
   * ssl-context - a netty/ssl-server-context"
  [gemini-handler & opts]
  (start-tcp-server! (wrap-tcp-stream gemini-handler) opts))
