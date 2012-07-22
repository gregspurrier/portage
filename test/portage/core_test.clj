(ns portage.core-test
  (:use portage.core
        midje.sweet))

(unfinished result-fn one-arg-fn two-arg-fn)

(defn ^:portageable portage-wrapped-one-arg-fn
  [cc x]
  (cc (one-arg-fn x))
  37)

(defn ^:portageable portage-wrapped-two-arg-fn
  [cc x a]
  (cc (two-arg-fn x a))
  42)

(fact "normal functions are threaded as with ->"
  (-+-> result-fn ..input..
        one-arg-fn
        (one-arg-fn)
        (two-arg-fn ..arg..))
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "portageable functions appear to be threaded as with ->"
  (-+-> result-fn ..input..
        portage-wrapped-one-arg-fn
        (portage-wrapped-one-arg-fn)
        (portage-wrapped-two-arg-fn ..arg..))
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "normal and porteagable functions can be mixed in the same flow"
  (-+-> result-fn ..input..
        (two-arg-fn ..arg..)
        portage-wrapped-one-arg-fn
        one-arg-fn)
  => nil
  (provided (two-arg-fn ..input.. ..arg..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (one-arg-fn ..result2..) => ..result3..
            (result-fn ..result3..) => ..anything..)

  (-+-> result-fn ..input..
        (portage-wrapped-two-arg-fn ..arg..)
        one-arg-fn
        portage-wrapped-one-arg-fn)
  => nil
  (provided (two-arg-fn ..input.. ..arg..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (one-arg-fn ..result2..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "-+-> forms are portageable themselves"
  (-+-> result-fn ..input..
        (-+-> one-arg-fn (two-arg-fn ..arg..))
        one-arg-fn)
  => nil
  (provided (one-arg-fn ..input..)           => ..result1..
            (two-arg-fn ..result1.. ..arg..) => ..result2..
            (one-arg-fn ..result2..)         => ..result3..
            (result-fn ..result3..)          => ..anything..))
