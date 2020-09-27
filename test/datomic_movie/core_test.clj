(ns datomic-movie.core-test
  (:require [clojure.test :refer :all]
            [datomic-movie.core :as c]))


(def db-uri "datomic:mem://movies")
;;(def db-uri "datomic:free://localhost:4334/movies")

(defonce state (atom nil))


(defn connection
  "Get connection from the state"
  []
  (:conn @state))

(defn start
  "Start resets  the state attom afte  the database and
   transacts schema and seed data returning
   a map the the connection"
  []
  (let [res (c/setup-db! db-uri)]
    (reset! state res)))

(defn stop
  "Stop DB"
  []
  (c/teardown-db! (:conn state)))


(comment
  ::REPL commands
  (require '[datomic.api :as d])
  (require :reload '[datomic-movie.core :as c])
  (require :reload '[datomic-movie.core-test :as t])

  ;;1. All movies
  (count (d/q c/all-movies (d/db (t/connection))))
  (type (d/q c/all-movies (d/db (t/connection))))
  (d/q c/all-movies (d/db (t/connection)))
  (sort-by first (d/q c/all-movies (d/db (t/connection))))

  ;; 2. fixed parameter
  (d/q c/movies-of-1985 (d/db (:conn @state)))

  ;; Movies with actor
  (d/q c/movies-of-actor (d/db (:conn @t/state)) "Arnold Schwarzenegger")

  ;; year of titles
  (d/q c/year-by-title  (d/db (:conn @t/state)) ["Lethal Weapon"
                                                 "Lethal Weapon 2"
                                                 "Lethal Weapon 3"])


  ;; who directed wovies with actor
  (d/q c/who-directed-actor (d/db (:conn @t/state))  "Arnold Schwarzenegger")


  ;; Predicates
  (d/q c/movies-before (d/db (:conn @t/state)) 1984)

  ;; Actors older that a gvien actos 
  (d/q c/actor-older-and-movies (d/db (:conn @t/state)) "Danny Glover")

  )
