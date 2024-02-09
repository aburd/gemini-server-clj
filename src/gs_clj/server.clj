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

(defn emphasize
  [s & {:keys [len c] :or {len 20 c '*}}]
  (let [s-len (count s)
        star-count (/ (- len s-len) 2)]
    (if (> s-len len)
      s
      (let [stars (apply str (take star-count (repeat c)))]
        (str stars (string/upper-case s) stars)))))

(defn wrap-tcp-stream
  [gemini-req-handler-fn]
  (fn [s info]
    (log/debug (emphasize "start"))
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
                          @(s/put! s header)
                          (when (true? success?)
                            @(s/put! s body))))})

           ;; close the connection on success
           (fn [{:keys [req result]}]
             (when result
               (do
                 (log/debug (emphasize "end"))
                 (s/close! s)))
             (when (and (not result) (not= req ::none))
               (d/recur req))))

            ;; if there were any issues on the far end, send a stringified exception back
            ;; and close the connection
          (d/catch
           TimeoutException
           #(handle-tcp-error % s "The request has timed out"))
          (d/catch
           #(handle-tcp-error % s "An unknown error occurred"))))))

(defn get-path
  [uri]
  (string/trim (string/replace uri "gemini://localhost", "")))

(defn resolve-file-path
  [^String uri-path]
  (condp = uri-path
    "/" "/index.gmi"
    "" "/index.gmi"
    nil "/index.gmi"
    uri-path))

(defn get-file-body [file-path]
  (log/debug "Getting file at:", file-path)
  (slurp (java-io/resource (str "public" file-path))))

; TODO: Not implemented
(defn get-handler-type
  "get the general class of response handler type"
  [^String path]
  (cond
    false :input
    ; TODO; not sure
    false :failure
    ; TODO; not sure
    false :client-certifiates
    :else :resource))

; (deftype Response [fields]
;   Protocol)

; TODO: Not-implemented
(defn handle-input-req []
  {:header (headers/input)
   :body nil
   :success? true})

; TODO: Not-implemented
(defn handle-failure-req [^String msg]
  {:header (headers/permanent-failure msg)
   :body nil
   :success? true})

; TODO: Not-implemented
(defn handle-client-certificates-req [path]
  {:header (headers/success "text/gemini")
   :body nil
   :success? true})

; TODO: Not-implemented
(defn handle-resource-req [uri-path]
  {:header (headers/success "text/gemini")
   :body (get-file-body (resolve-file-path uri-path))
   :success? true})

(def req-handlers
  {:input handle-input-req
   :failure handle-failure-req
   :client-certifiates handle-client-certificates-req
   :resource handle-resource-req})

(defn gemini-req-handler
  "Handles a gemini requests"
  [^String uri]
  (log/debug "Got a URI! " uri)
  (let [path (get-path uri)
        handler-type (get-handler-type path)
        handler-fn (get req-handlers handler-type)]
    (log/debug "Handling path:" path)
    (log/debug "Handler Type:" handler-type)
    (handler-fn path)))

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
