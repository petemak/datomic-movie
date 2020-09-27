(ns datomic-movie.core
  (:require [datomic.api :as d]
            [datomic-movie.db-util :as u]))


;--------------------------------------------------------------
;; 1. Get datomic-pro starter
;; 2. Extract 
;; 3. Copy a transactor.properties and specify license-key
;; 4. Start transactor by specifying the property file
;;   $ ./bin/transactor ./config/transactor.properties
;;   Launching with Java options -server -Xms1g -Xmx1g -XX:...
;;   Starting datomic:dev://localhost:4334/<DB-NAME>, storing
;; data in: data ...
;;   System started datomic:dev://localhost:4334/<DB-NAME>,
;;   storing data in: data
;;
;; 5. Start Console -p for port, transactor name and uri
;;    $ ./bin/console -p <port> <name> <uri>
;;    
;; $ ./bin/console -p 8080 dev datomic:dev://localhost:4334/
;; Console started on port: 8080
;;    dev = datomic:dev://localhost:4334/
;; Open http://localhost:8080/browse in your browser
;; (Chrome recommended)
;;--------------------------------------------------------------


;; Load schemam and data from files
(def schema  (u/read-EDN "resources/db/schema.edn"))
(def data (u/read-EDN "resources/db/data.edn"))

;; Next, create the URI that allows to create and connect to a database.
;;The URI for a memory storage has 3 parts:
;;
;; - "datomic", identifying it as a Datomic URI
;; - "mem", the mem storage protocol instead of a persistent store
;; - "hello", the name of the database
(def db-uri "datomic:mem://movies")
;;(def db-uri "datomic:free://localhost:4334/movies")



;; To interact with a datomic database
;; we need a connection to the peer. Connections are
;; created using the database URI
;;
;; conn  --> peer ---> Query endinge
;;               |
;;                ---> Transactor
;; 
(defn connect
  "Creates an in memory databse and connects the
   the peer library to the database. Returns a
   connection if successful"
  [uri]
  (d/create-database uri)
  (d/connect uri))


;;-------------------------------------------------------------
;; Required only during REPL sessions
;;-------------------------------------------------------------
(defn setup-db!
  "Connects to the db transacts schema and data
  and returns a map containing
  :db-created? - bolean to singify if creation succeeded
  :conn - the coonection
  :schema - result of tracting the schema
  :db     - the current databse value after transacting data
  
  Note: schema ans db are futures and contain the following if
        the transaction completes
  :db-before         database value before the transaction
  :db-after          database value after the transaction
  :tx-data           collection of Datoms produced by the transaction
  :tempids           argument to resolve-tempids"
  
  [uri]
  (let [created? (d/create-database uri)
        conn   (d/connect uri)
        schema (d/transact conn schema)
        db     (d/transact conn data)]
    {:created created? :conn conn :db db}))


;;-------------------------------------------------------------
;; Required only during REPL sessions
;;-------------------------------------------------------------
(defn teardown-db!
  "Clean up database"
  [uri]
  (try
    (d/delete-database uri)
    (catch Exception e
      (str "::-> delete-db! failed: " (.getMessage e)))))


;;-------------------------------------------------------------
;; Simple query 
;; 
;; Query all movies 
;;-------------------------------------------------------------
(def all-movies
  '[:find ?e ?title
    :where
    [?e :movie/title ?title]])


;;-------------------------------------------------------------
;; Simple query with fixed value
;;
;; query moviews from a particular year
;;
;;(d/q movies-of-year (d/db conn))
;;-------------------------------------------------------------
(def movies-of-1985
  '[:find ?title
    :where
    [?e :movie/year 1985]
    [?e :movie/title ?title]])


;;-------------------------------------------------------------
;; Parametrised query
;;
;; query moviews by a specific actor
;;
;; (d/q movies-with-actor db "Arnold Schwazenegger")
;;-------------------------------------------------------------
(def movies-with-actor
  '[:find ?title
    :in $ ?name
    :where
    [?p :person/name ?name]
    [?m :movie/cast ?p]
    [?m :movie/title ?title]])


;;-------------------------------------------------------------
;; Parametrised query
;;
;; Query moviees by a particular director
;;
;; (d/q who-directed-actor db  "Arnold Schwazenegger")
;;-------------------------------------------------------------
(def who-directed-actor
  '[:find ?director-name
    :in $ ?actor-name
    :where
    [?p :person/name ?actor-name]
    [?m :movie/cast ?p]
    [?m :movie/director ?d]
    [?d :person/name ?director-name]])



;;-------------------------------------------------------------
;; Parametrised query
;;
;; query movie years by title
;;
;; (d/q year-by-title db ["Lethal Weapon" "Lethal Weapon 2" "Lethal Weapon 3"])
;;-------------------------------------------------------------
(def year-by-title
  '[:find ?title ?year
    :in $ [?title ...]
    :where
    [?e :movie/title ?title]
    [?e :movie/year ?year]])




;;-------------------------------------------------------------
;; Predicates
;;
;; query movies released before a given date
;;
;; (d/q movies-before db 1984)
;;-------------------------------------------------------------
(def movies-before
  '[:find ?title
    :in $ ?date
    :where
    [?m :movie/title ?title]
    [?m :movie/year ?year]
    [(< ?year ?date)]])

;;-------------------------------------------------------------
;; Predicates
;;
;; query actors older than a given actor and their movies
;; birth year
;;
;; (d/q actor-older-and-movies db "Danny Glover")
;;-------------------------------------------------------------
(def actor-older-and-movies
  '[:find ?actor ?year ?title
    :in $ ?name
    :where
    [?a :person/name ?name]
    [?a :person/born ?max-year]
    [?p :person/born ?year]
    [?p :person/name ?actor]
    [?m :movie/cast ?p]
    [?m :movie/title ?title]
    [(< ?year ?max-year)]])




;;-------------------------------------------------------------
;; Relations
;;
;; query movies released before someones
;; birth year
;;
;; (d/q actor-older-and-movies db "Danny Glover")
;;-------------------------------------------------------------
(def earnings
  [["Die Hard" 14030000]
   ["Alien" 104931801]
   ["Commando" 54791000]])

;; Find 
(def earnings-of-director
  '[:find ?title ?earnings
    :in $ ?director
    [?p :person/name ?director]
    [?m :movie/direcor ?p]
    [?m :movie/title ?title]])



(defn run-query
  [query conn]
  (d/q query (d/db conn)))
