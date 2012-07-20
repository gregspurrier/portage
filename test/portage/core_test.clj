(ns portage.core-test
  (:use portage.core
        midje.sweet))

(unfinished result-fn one-arg-fn two-arg-fn)
(fact "non-portagable functions are threaded as with ->"
  (portage-> ..input.. result-fn
             one-arg-fn
             (one-arg-fn)
             (two-arg-fn ..arg2..))
  => nil
  (provided (one-arg-fn ..input..) => ..result1..
            (one-arg-fn ..result1..) => ..result2..
            (two-arg-fn ..result2.. ..arg2..) => ..result3..
            (result-fn ..result3..) => ..anything..))