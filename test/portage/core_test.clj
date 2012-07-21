(ns portage.core-test
  (:use portage.core
        midje.sweet))

(unfinished result-fn one-arg-fn two-arg-fn)

(defn ^:portageable portage-wrapped-one-arg-fn
  [cc x]
  (cc (one-arg-fn x))
  37)

(fact "non-portageable functions are threaded as with ->"
  (-+-> result-fn ..input..
        (one-arg-fn)
        (one-arg-fn)
        (two-arg-fn ..arg2..))
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg2..) => ..result3..
            (result-fn ..result3..) => ..anything..))

(fact
  (-+-> result-fn ..input..
        (portage-wrapped-one-arg-fn))
  => nil
  (provided (one-arg-fn ..input..) => ..result..
            (result-fn ..result..) => ..anything..))

(fact
  (-+-> result-fn ..input..
        (portage-wrapped-one-arg-fn)
        (one-arg-fn))
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (result-fn ..result2..) => ..anything..))

(fact
  (-+-> result-fn ..input..
        (two-arg-fn ..arg1..)
        (portage-wrapped-one-arg-fn)
        (one-arg-fn)
        (portage-wrapped-one-arg-fn)
        (two-arg-fn ..arg2..))
  => nil
  (provided (two-arg-fn ..input.. ..arg1..)   => ..result1..
            (one-arg-fn ..result1..)          => ..result2..
            (one-arg-fn ..result2..)          => ..result3..
            (one-arg-fn ..result3..)          => ..result4..
            (two-arg-fn ..result4.. ..arg2..) => ..result5..
            (result-fn ..result5..)           => ..anything..))
