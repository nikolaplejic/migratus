(ns migratus.test.migration.sql
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.test :refer :all]
            [migratus.core :as core]
            [migratus.database :as db]
            [migratus.migration.sql :refer :all]
            migratus.mock
            [migratus.protocols :as proto]
            [next.jdbc :as jdbc]))

(def db-store (str (.getName (io/file ".")) "/site.db"))

(def test-config {:migration-dir        "migrations/"
                  :db                   {:dbtype      "h2"
                                         :dbname      db-store}})

(defn reset-db []
  (letfn [(delete [f]
            (when (.exists f)
              (.delete f)))]
    (delete (io/file "site.db.trace.db"))
    (delete (io/file "site.db.mv.db"))
    (delete (io/file "site.db"))))

(defn setup-test-db [f]
  (reset-db)
  (f))

(use-fixtures :each setup-test-db)

(defn verify-table-exists? [config table-name]
  (with-open [con (jdbc/get-connection (:db config))]
    (db/table-exists? {:connection con} table-name)))

(deftest test-run-sql-migrations
  (let [config (merge test-config
                      {:store :mock
                       :completed-ids (atom #{})})]

    (is (not (verify-table-exists? config "foo")))
    (is (not (verify-table-exists? config "bar")))
    (is (not (verify-table-exists? config "quux")))
    (is (not (verify-table-exists? config "quux2")))

    (core/migrate config)

    (is (verify-table-exists? config "foo"))
    (is (verify-table-exists? config "bar"))
    (is (verify-table-exists? config "quux"))
    (is (verify-table-exists? config "quux2"))

    (core/rollback config)

    (is (verify-table-exists? config "foo"))
    (is (verify-table-exists? config "bar"))
    (is (not (verify-table-exists? config "quux")))
    (is (not (verify-table-exists? config "quux2")))))
