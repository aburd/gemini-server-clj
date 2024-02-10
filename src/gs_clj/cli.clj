(ns gs-clj.cli
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io])
  (:gen-class))

(def -cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   ["-v" "--verbose" "Verbose logging"]
   ["-l" "--log-level LEVEL" "Level of log 0-6, 0 is trace"
    :id :log-level
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 6) "Must be a number between 0 and 6"]]
   ["-c" "--config CONFIG" "path to config.edn"
    :id :config
    :default "resources/config.edn"
    :validate [#(.exists (io/file %)) "Config file does not exist"]]
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["Gemini server"
        ""
        "Usage: gemini-server-clj [options] ACTION"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"]
       (string/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args -cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"start" "stop" "status"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

