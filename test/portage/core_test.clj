(ns portage.core-test
  (:use portage.core
        midje.sweet
        [midje.util :only (expose-testables)]))

(expose-testables portage.core)

(unfinished result-fn one-arg-fn two-arg-fn)

(defn ^:portageable portage-wrapped-one-arg-fn
  [cc x]
  (cc (one-arg-fn x))
  37)

(defn ^:portageable portage-wrapped-two-arg-fn
  [cc x a]
  (cc (two-arg-fn x a))
  42)

(facts "about portageable?"
  (portageable? 'portage-wrapped-one-arg-fn)
  => true
  (portageable? 'identity)
  => false
  (portageable? :foo)
  => false
  (portageable? '(fn [x] x))
  => false)

(fact "normal functions are threaded as with ->"
  (-+-> ..input..
        one-arg-fn
        (one-arg-fn)
        (two-arg-fn ..arg..)
        result-fn)
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "portageable functions appear to be threaded as with ->"
  (-+-> ..input..
        portage-wrapped-one-arg-fn
        (portage-wrapped-one-arg-fn)
        (portage-wrapped-two-arg-fn ..arg..)
        result-fn)
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "normal and porteagable functions can be mixed in the same flow"
  (-+-> ..input..
        (two-arg-fn ..arg..)
        portage-wrapped-one-arg-fn
        one-arg-fn
        result-fn)
  => nil
  (provided (two-arg-fn ..input.. ..arg..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (one-arg-fn ..result2..) => ..result3..
            (result-fn ..result3..) => ..anything..)

  (-+-> ..input..
        (portage-wrapped-two-arg-fn ..arg..)
        one-arg-fn
        portage-wrapped-one-arg-fn
        result-fn)
  => nil
  (provided (two-arg-fn ..input.. ..arg..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (one-arg-fn ..result2..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact "Errors bypass intermediate forms, but not the final form"
  (let [err (error "an error")]
    (-+-> err
          one-arg-fn
          portage-wrapped-one-arg-fn
          result-fn)
    => nil
    (provided (one-arg-fn err) => nil :times 0
              (portage-wrapped-one-arg-fn) => nil :times 0
              (result-fn err) => ..anything..)))

(defn ^:accepts-errors error-accepting-fn
  [x]
  x)

(fact "Intermediate forms marked as :accepts-errors are not bypassed"
  (let [err (error "an error")]
    (-+-> err
          error-accepting-fn
          one-arg-fn
          result-fn)
    => nil
    (provided (error-accepting-fn err) => err
              (one-arg-fn err) => nil :times 0
              (result-fn err) => ..anything..)))