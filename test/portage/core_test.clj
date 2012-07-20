(ns portage.core-test
  (:use portage.core
        midje.sweet))

(fact (+ 1 1) => 2)