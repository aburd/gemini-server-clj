# Gemini Server CLJ [gs-clj]

A gemini server written in Clojure built mostly on top of the wonderful [aleph](https://github.com/clj-commons/aleph) library.

## Usage

### As a stand-alone Gemini Server

You can use this project as a server through the CLI. 
After cloning this repo, you'll need [leiningen](https://leiningen.org) to install the depedencies and run the server. After running `lein install`, use `lein run` for more information.

```
➜  gemini-server-clj git:(main) ✗ lein run
Gemini server

Usage: gemini-server-clj [options] ACTION

Options:
  -p, --port PORT        1965  Port number
  -v, --verbose                Verbose logging
  -l, --log-level LEVEL  2     Level of log 0-6, 0 is trace
  -h, --help

Actions:
  start    Start a new server
```

#### Configuration File

TODO

### As a library

```clojure
(require '[gs-clj.server :as server])

(defn handler [req]
  {:header {:status :success    ; or just use :code
            :code 20            ; or just use :header
            :meta "text/gemini"
   :body "hello gemini!"})

; TODO: how to config ssl-certs
(server/start! handler {:port 1965})
```

## License

Copyright © 2024 Aaron Burdick

This program and the accompanying materials are made available under the
terms of the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)
