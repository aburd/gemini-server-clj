(ns gs-clj.server
  (:import [java.util.concurrent TimeoutException])
  (:require [aleph.tcp :as tcp]
            [aleph.netty :as netty]
            [gloss.io :as io]
            [gloss.core :as gloss]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.java.io :as java-io]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [gs-clj.gemini :as gemini]
            [gs-clj.headers :as headers]))

(def protocol
  (gloss/compile-frame
   (gloss/string :utf-8)))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)
    (s/splice
     out
     (io/decode-stream s protocol))))

(def timeout-ms 10000)

; TODO: Check uri better
(defn gemini-uri? [uri]
  (and
   (string/starts-with? uri "gemini://")
   (string/ends-with? uri gemini/clrf)))

(defn handle-tcp-error
  [err stream msg]
  (log/error err)
  (s/put! stream (headers/permanent-failure msg))
  (s/close! stream))

(defn wrap-tcp-stream
  [gemini-req-handler-fn]
  (fn [s info]
    (log/debug "Connection established: " info)

    (d/loop [req ""]
      ;; take a message, and define a default value that tells us if the connection is closed
      (-> (s/take! s ::none)

          (d/chain

           ;; first, check if there even was a message
           (fn [msg]
             (if (= ::none msg)
               ::none
               (str req msg)))

           ;; check that the request is valid
           ;; if so, process on another thread
           (fn [maybe-uri]
             (log/debug "uri?" maybe-uri)
             (d/timeout!
              (d/future {:req maybe-uri
                         :result
                         (when (gemini-uri? maybe-uri)
                           (gemini-req-handler-fn maybe-uri))})
              timeout-ms))

           ;; if there was a success result, we write it to the stream
           (fn [{:keys [req result]}]
             (log/debug "req: " req)
             (log/debug "gemini result: " result)
             {:req req
              :result (when-not (nil? result)
                        (let [{:keys [header body success?]} result]
                          (log/debug header body success?)
                          @(s/put! s header)
                          (when (true? success?)
                            @(s/put! s body))))})

           ;; close the connection on success
           (fn [{:keys [req result]}]
             (when result
               (s/close! s))
             (when (not= req ::none)
               (d/recur req))))

            ;; if there were any issues on the far end, send a stringified exception back
            ;; and close the connection
          (d/catch
           TimeoutException
           #(handle-tcp-error % s "The request has timed out"))
          (d/catch
           #(handle-tcp-error % s "An unknown error occurred"))))))

(defn resolve-path
  [^String path]
  (log/debug "resolving:" path)
  (if (= path "/")
    "/index.gmi"
    path))

(defn get-file-body [file-path]
  (log/debug "Getting file at:", file-path)
  (slurp (java-io/resource (str "public" file-path))))

(defn gemini-req-handler
  "Handles a gemini requests"
  [^String uri]
  (log/debug "Got a URI! " uri)
  (let [path (string/trim (string/replace uri "gemini://localhost", ""))]
    (log/debug "path:" path)
    {:header (headers/success "text/gemini")
     :body (get-file-body (resolve-path path))
     :success? true}))

(defn server->ssl-context
  [key cert]
  (netty/ssl-server-context
   {:private-key (java-io/file key)
    :certificate-chain (java-io/file cert)}))

(defn start-server
  "Starts the server and applies the tcp-stream to the stream-handler.
  port - server port"
  [stream-handler & {:keys [port] :or {port gemini/default-port}}]
  (log/debug (str "Starting server on port <" port ">"))
  (tcp/start-server
   (fn [s info]
     (stream-handler
      (wrap-duplex-stream protocol s)
      info))
   {:port port
    :ssl-context (server->ssl-context
                  (java-io/resource "app.key")
                  (java-io/resource "app.pem"))}))

(defn start!
  "Starts the server with the gemini request handler fn. wrap-tcp-stream will stream valid uris to the handler."
  [& opts]
  (start-server (wrap-tcp-stream gemini-req-handler) opts))
