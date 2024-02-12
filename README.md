# Gemini Server CLJ [gs-clj]

A gemini server written in Clojure built mostly on top of the wonderful [aleph](https://github.com/clj-commons/aleph) library.

## Usage

### As a stand-alone Gemini Server

For now, all it does is map the requests to the public path (default: `resources/public`). Only handles gmi, txt, jpg, and png file types.

You can use this project as a server through the CLI. 
After cloning this repo, you'll need [leiningen](https://leiningen.org) to install the depedencies and run the server. After running `lein install`, use `lein run` for more information.

```
➜  gemini-server-clj git:(main) ✗ lein run
Gemini server

Usage: gemini-server-clj [options] ACTION

Options:
  -p, --port PORT                              Port number
  -v, --verbose                                Verbose logging
  -l, --log-level LEVEL                        Level of log 0-6, 0 is trace
  -c, --config CONFIG    resources/config.edn  path to config.edn
  -u, --public PUBLIC_PATH                        path to your public files
  -h, --help

Actions:
  start    Start a new server
```

#### Configuration File

You can set a configuration file at resources/config.edn. If you want to put your config file somewhere else, then tell the cli: `--config other_path/config.edn`. You can find a config in the resources/config.edn folder.

CLI opts will override config file if you pass them in.

### As a library

```clojure
(require '[gs-clj.server :as server])

(defn handler [req opts]
  {:header {:status :success    ; or just use :code
            :code 20            ; or just use :header
            :meta "text/gemini"
   :body {:utf8 "hello gemini!"})

(defn img-handler [req opts]
  {:header {:status :success    ; or just use :code
            :code 20            ; or just use :header
            :meta "image/png"
   :body {:bytes (utils/slurp-bytes "some-pic.png")})

; TODO: how to config ssl-certs
(server/start! handler {:port 1965})
```

`req` has various uri information, basically what you'd find in a [lambdaisland/uri](https://cljdoc.org/d/lambdaisland/uri/1.19.155/api/lambdaisland.uri) response, sans the things not in the gemini spec. 

`opts` contains public-path info.

## License

Copyright © 2024 Aaron Burdick

This program and the accompanying materials are made available under the
terms of the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)
